import { Component, OnDestroy, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';
import { StoreContextDto } from '../../../store/model/dto/StoreContextDto';
import { StoreContextService } from '../../../store/service/store-context.service';
import { CartItemDto } from '../../model/dto/CartItemDto';
import { ShoppingCartDto } from '../../model/dto/ShoppingCartDto';
import { CartService } from '../../service/cart.service';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit, OnDestroy {
  public Cart = new ShoppingCartDto();
  public StoreContext: StoreContextDto | null = null;

  private subscriptions = new Subscription();

  public constructor(
    private cartService: CartService,
    private storeContextService: StoreContextService,
    private toastrService: ToastrService
  ) {
  }

  public ngOnInit(): void {
    this.subscriptions.add(this.cartService.Cart$.subscribe(cart => this.Cart = cart));
    this.subscriptions.add(this.storeContextService.Context$.subscribe(context => this.StoreContext = context));
  }

  public ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  public changeQuantity(item: CartItemDto, change: number): void {
    try {
      this.cartService.updateQuantity(item.ProductCod, item.Quantity + change);
    } catch (error: any) {
      this.toastrService.warning(error.message);
    }
  }

  public setQuantity(item: CartItemDto, event: Event): void {
    const input = event.target as HTMLInputElement;
    try {
      this.cartService.updateQuantity(item.ProductCod, Number(input.value));
    } catch (error: any) {
      input.value = String(item.Quantity);
      this.toastrService.warning(error.message);
    }
  }

  public remove(ProductCod: string): void {
    this.cartService.remove(ProductCod);
  }

  public subtotal(): number {
    return this.cartService.subtotal();
  }

  public lineTotal(item: CartItemDto): number {
    return Math.round(item.UnitPrice * item.Quantity * 100) / 100;
  }

  public currency(): string {
    return this.Cart.Items[0]?.CurrencyCod || 'PEN';
  }
}
