import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AppSetting } from '../../../config/app.setting';
import { ProductInfoDto } from '../../catalog/model/dto/ProductInfoDto';
import { ProductSearchEntity } from '../../catalog/model/entity/ProductSearchEntity';
import { StorageConstants } from '../../shared/model/constants/StorageConstants';
import { CartItemDto } from '../model/dto/CartItemDto';
import { ShoppingCartDto } from '../model/dto/ShoppingCartDto';

@Injectable({ providedIn: 'root' })
export class CartService {
  private cartSubject = new BehaviorSubject<ShoppingCartDto>(this.readCart());

  public readonly Cart$: Observable<ShoppingCartDto> = this.cartSubject.asObservable();

  public getCurrent(): ShoppingCartDto {
    return this.cartSubject.value;
  }

  public add(product: ProductSearchEntity, quantity: number = 1): void {
    const cart = this.cloneCart();
    if (cart.StoreCod && cart.StoreCod !== product.StoreCod) {
      throw new Error('El producto pertenece a otra tienda. Vacía el carrito antes de cambiar de tienda.');
    }

    const freshItem = CartItemDto.fromProduct(product);
    const current = cart.Items.find(item => item.ProductCod === product.ProductCod);
    const newQuantity = (current?.Quantity || 0) + quantity;
    this.validateQuantity(freshItem, newQuantity);

    cart.StoreCod = product.StoreCod;
    if (current) {
      Object.assign(current, freshItem);
      current.Quantity = newQuantity;
    } else {
      freshItem.Quantity = quantity;
      cart.Items.push(freshItem);
    }
    this.save(cart);
  }

  public updateQuantity(ProductCod: string, quantity: number): void {
    const cart = this.cloneCart();
    const item = cart.Items.find(current => current.ProductCod === ProductCod);
    if (!item) return;
    if (quantity <= 0) {
      this.remove(ProductCod);
      return;
    }
    this.validateQuantity(item, quantity);
    item.Quantity = quantity;
    this.save(cart);
  }

  public remove(ProductCod: string): void {
    const cart = this.cloneCart();
    cart.Items = cart.Items.filter(item => item.ProductCod !== ProductCod);
    if (cart.Items.length === 0) cart.StoreCod = '';
    this.save(cart);
  }

  public clear(): void {
    this.save(new ShoppingCartDto());
  }

  public count(): number {
    return this.cartSubject.value.Items.reduce((total, item) => total + item.Quantity, 0);
  }

  public subtotal(): number {
    return this.money(this.cartSubject.value.Items.reduce(
      (total, item) => total + item.UnitPrice * item.Quantity,
      0
    ));
  }

  private validateQuantity(item: CartItemDto, quantity: number): void {
    if (!Number.isInteger(quantity) || quantity <= 0) {
      throw new Error('La cantidad debe ser un número entero mayor que cero.');
    }
    if (item.IsDigital !== 'S' && quantity > item.AvailableQuantity) {
      throw new Error(`Solo hay ${item.AvailableQuantity} ${item.ProductUnitName} disponibles.`);
    }
  }

  private cloneCart(): ShoppingCartDto {
    const cart = Object.assign(new ShoppingCartDto(), JSON.parse(JSON.stringify(this.cartSubject.value)));
    cart.Items = (cart.Items || []).map((item: CartItemDto) => this.hydrateItem(item));
    return cart;
  }

  private save(cart: ShoppingCartDto): void {
    cart.UpdatedAt = new Date().toISOString();
    localStorage.setItem(StorageConstants.SHOPPING_CART, JSON.stringify(cart));
    this.cartSubject.next(cart);
  }

  private readCart(): ShoppingCartDto {
    const value = localStorage.getItem(StorageConstants.SHOPPING_CART);
    if (!value) return new ShoppingCartDto();
    try {
      const parsed = JSON.parse(value);
      const cart = Object.assign(new ShoppingCartDto(), parsed);
      cart.Items = (parsed.Items || []).map((item: CartItemDto) => this.hydrateItem(item));
      return cart;
    } catch {
      localStorage.removeItem(StorageConstants.SHOPPING_CART);
      return new ShoppingCartDto();
    }
  }

  private money(value: number): number {
    return Math.round(value * 100) / 100;
  }

  private hydrateItem(value: CartItemDto): CartItemDto {
    const item = Object.assign(new CartItemDto(), value);
    if (value.ProductInfo?.Config?.ProductCod) {
      item.ProductInfo = Object.assign(new ProductInfoDto(), value.ProductInfo);
      this.normalizeLegacyFileRoute(item);
      return item;
    }

    const product = new ProductSearchEntity();
    const factor = Math.max(1, Number(item.ProductUnitFactor || 1));
    product.ProductCod = item.ProductCod;
    product.StoreCod = item.StoreCod;
    product.ProductName = item.ProductName;
    product.ProductDesc = item.ProductDesc;
    product.NumPrice = this.money(Number(item.UnitPrice || 0) / factor);
    product.NumPhysicalStock = Number(item.AvailableQuantity || 0) * factor;
    product.NumTotalStock = product.NumPhysicalStock;
    product.IsDigital = item.IsDigital || 'N';
    product.ProductUnitName = item.ProductUnitName || 'NIU';
    product.ProductUnitFactor = factor;
    product.CurrencyCod = item.CurrencyCod;
    product.CurrencySymbol = item.CurrencySymbol;
    product.FileRoute = item.FileRoute;
    item.ProductInfo = ProductInfoDto.fromProductSearch(product);
    this.normalizeLegacyFileRoute(item);
    return item;
  }

  private normalizeLegacyFileRoute(item: CartItemDto): void {
    const fileCod = item.ProductInfo?.Picture?.FileCod || '';
    const usesLegacyRoute = (item.FileRoute || '').includes('/assets/public/');
    if (!fileCod || (!usesLegacyRoute && item.FileRoute)) return;
    const baseUrl = AppSetting.API.replace(/\/$/, '');
    item.FileRoute = `${baseUrl}/api/v1/public/appFile/${encodeURIComponent(fileCod)}`;
  }
}
