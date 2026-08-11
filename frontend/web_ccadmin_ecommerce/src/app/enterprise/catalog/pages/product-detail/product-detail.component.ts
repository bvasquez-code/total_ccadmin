import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';
import { CartService } from '../../../cart/service/cart.service';
import { StoreContextDto } from '../../../store/model/dto/StoreContextDto';
import { StoreContextService } from '../../../store/service/store-context.service';
import { ProductDeliveryDetailDto, ProductDeliveryPictureDto } from '../../model/dto/ProductDeliveryDetailDto';
import { ProductSearchEntity } from '../../model/entity/ProductSearchEntity';
import { ProductDeliverySearchService } from '../../service/product-delivery-search.service';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.css']
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  public ProductCod: string = '';
  public StoreContext: StoreContextDto | null = null;
  public Product: ProductSearchEntity | null = null;
  public ImageRoutes: string[] = [];
  public SelectedImageRoute: string = '';
  public Quantity: number = 1;
  public IsLoading: boolean = false;
  public HasLoadError: boolean = false;

  private subscriptions = new Subscription();
  private loadedKey: string = '';

  public constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private productDeliverySearchService: ProductDeliverySearchService,
    private storeContextService: StoreContextService,
    private cartService: CartService,
    private toastrService: ToastrService
  ) {
  }

  public ngOnInit(): void {
    this.subscriptions.add(this.activatedRoute.paramMap.subscribe(params => {
      this.ProductCod = params.get('productCode') || '';
      this.loadedKey = '';
      void this.loadProduct();
    }));
    this.subscriptions.add(this.storeContextService.Context$.subscribe(context => {
      const storeChanged = context?.Store.StoreCod !== this.StoreContext?.Store.StoreCod;
      this.StoreContext = context;
      if (storeChanged) {
        this.loadedKey = '';
        void this.loadProduct();
      }
    }));
  }

  public ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  public selectLocation(): void {
    this.storeContextService.requestLocationSelection();
  }

  public selectImage(route: string): void {
    this.SelectedImageRoute = route;
  }

  public decreaseQuantity(): void {
    if (this.Quantity > 1) this.Quantity--;
  }

  public increaseQuantity(): void {
    if (!this.Product || !this.canIncreaseQuantity()) return;
    this.Quantity++;
  }

  public canIncreaseQuantity(): boolean {
    return !!this.Product && (this.isDigital() || this.Quantity < this.visibleStock());
  }

  public async addToCart(): Promise<void> {
    if (!this.Product || !this.StoreContext) return;

    const response = await this.productDeliverySearchService.findAvailability(
      this.Product.ProductCod,
      this.StoreContext.Store.StoreCod
    );
    if (response.ErrorStatus || !response.Data) {
      this.toastrService.error(response.Message || 'No pudimos verificar el stock de este producto.');
      return;
    }

    try {
      const availableProduct = Object.assign(new ProductSearchEntity(), response.Data);
      this.cartService.add(availableProduct, this.Quantity);
      this.Product = availableProduct;
      this.toastrService.success(
        `${this.Quantity} ${this.Quantity === 1 ? 'unidad agregada' : 'unidades agregadas'} al carrito.`
      );
    } catch (error: any) {
      this.toastrService.warning(error.message);
    }
  }

  public goToCart(): void {
    void this.router.navigate(['/cart']);
  }

  public isDigital(): boolean {
    return (this.Product?.IsDigital || 'N').toUpperCase() === 'S';
  }

  public visiblePrice(): number {
    if (!this.Product) return 0;
    return this.money(
      Number(this.Product.NumPrice || 0) * Math.max(1, Number(this.Product.ProductUnitFactor || 1))
    );
  }

  public visibleStock(): number {
    if (!this.Product) return 0;
    return Math.max(0, Math.floor(
      Number(this.Product.NumPhysicalStock || 0)
      / Math.max(1, Number(this.Product.ProductUnitFactor || 1))
    ));
  }

  public isInCart(): boolean {
    return !!this.Product && this.cartService.getCurrent().Items.some(
      item => item.ProductCod === this.Product?.ProductCod
    );
  }

  private async loadProduct(): Promise<void> {
    const StoreCod = this.StoreContext?.Store.StoreCod || '';
    if (!this.ProductCod || !StoreCod || this.IsLoading) return;

    const requestKey = `${StoreCod}:${this.ProductCod}`;
    if (requestKey === this.loadedKey) return;

    this.IsLoading = true;
    this.HasLoadError = false;
    const response = await this.productDeliverySearchService.findDetail(this.ProductCod, StoreCod);
    this.IsLoading = false;

    if (this.StoreContext?.Store.StoreCod !== StoreCod) {
      void this.loadProduct();
      return;
    }

    if (response.ErrorStatus || !response.Data?.Product) {
      this.Product = null;
      this.ImageRoutes = [];
      this.SelectedImageRoute = '';
      this.HasLoadError = true;
      this.toastrService.error(response.Message || 'No pudimos cargar el producto seleccionado.');
      return;
    }

    const detail = Object.assign(new ProductDeliveryDetailDto(), response.Data);
    const product = Object.assign(new ProductSearchEntity(), detail.Product);
    this.Product = product;
    this.ImageRoutes = this.buildImageRoutes(detail.PictureList || [], product.FileRoute);
    this.SelectedImageRoute = this.ImageRoutes[0] || '';
    this.Quantity = 1;
    this.loadedKey = requestKey;
  }

  private buildImageRoutes(pictures: ProductDeliveryPictureDto[], principalRoute: string): string[] {
    const routes = pictures
      .map(picture => picture.appFile?.Route?.trim())
      .filter((route): route is string => !!route);
    if (principalRoute?.trim()) routes.unshift(principalRoute.trim());
    return routes.filter((route, index) => routes.indexOf(route) === index);
  }

  private money(value: number): number {
    return Math.round(value * 100) / 100;
  }
}
