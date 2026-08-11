import { Component, OnDestroy, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';
import { CartService } from '../../../cart/service/cart.service';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { StoreContextDto } from '../../../store/model/dto/StoreContextDto';
import { StoreContextService } from '../../../store/service/store-context.service';
import { ProductSearchDto } from '../../model/dto/ProductSearchDto';
import { ProductSearchEntity } from '../../model/entity/ProductSearchEntity';
import { ProductDeliverySearchService } from '../../service/product-delivery-search.service';

@Component({
  selector: 'app-catalog',
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.css']
})
export class CatalogComponent implements OnInit, OnDestroy {
  public StoreContext: StoreContextDto | null = null;
  public Search = new ProductSearchDto();
  public Page = new ResponsePageSearch<ProductSearchEntity>();
  public Products: ProductSearchEntity[] = [];
  public SortOption: string = 'trend';
  public IsLoading: boolean = false;
  public HasAttemptedSearch: boolean = false;

  private contextSubscription?: Subscription;

  public constructor(
    private productDeliverySearchService: ProductDeliverySearchService,
    private storeContextService: StoreContextService,
    private cartService: CartService,
    private toastrService: ToastrService
  ) {
  }

  public ngOnInit(): void {
    this.contextSubscription = this.storeContextService.Context$.subscribe(context => {
      const storeChanged = context?.Store.StoreCod !== this.StoreContext?.Store.StoreCod;
      this.StoreContext = context;
      if (context && storeChanged) {
        this.Search.StoreCod = context.Store.StoreCod;
        this.Search.Page = 1;
        void this.findProducts();
      }
    });
  }

  public ngOnDestroy(): void {
    this.contextSubscription?.unsubscribe();
  }

  public selectLocation(): void {
    this.storeContextService.requestLocationSelection();
  }

  public submitSearch(): void {
    this.Search.Page = 1;
    void this.findProducts();
  }

  public changeSort(): void {
    this.Search.Page = 1;
    this.Search.SortedBy = this.SortOption === 'price_desc' ? 'price' : this.SortOption;
    this.Search.DirectionSortedBy = this.SortOption === 'trend' || this.SortOption === 'price_desc'
      ? 'desc'
      : 'asc';
    void this.findProducts();
  }

  public goToPage(page: number): void {
    if (page < 1 || page > this.Page.TotalPages || page === this.Search.Page) return;
    this.Search.Page = page;
    void this.findProducts();
    window.scrollTo({ top: 280, behavior: 'smooth' });
  }

  public pageNumbers(): number[] {
    const total = this.Page.TotalPages || 0;
    const current = this.Search.Page;
    const start = Math.max(1, Math.min(current - 2, total - 4));
    const end = Math.min(total, start + 4);
    return Array.from({ length: Math.max(0, end - start + 1) }, (_, index) => start + index);
  }

  public async addProduct(product: ProductSearchEntity): Promise<void> {
    if (!this.StoreContext) {
      this.selectLocation();
      return;
    }

    const response = await this.productDeliverySearchService.findAvailability(
      product.ProductCod,
      this.StoreContext.Store.StoreCod
    );
    if (response.ErrorStatus || !response.Data) {
      this.toastrService.error(response.Message || 'No pudimos verificar el stock de este producto.');
      return;
    }

    try {
      const availableProduct = Object.assign(new ProductSearchEntity(), response.Data);
      this.cartService.add(availableProduct);
      this.toastrService.success(`${product.ProductName} se agregó al carrito.`);
    } catch (error: any) {
      this.toastrService.warning(error.message);
    }
  }

  public isInCart(ProductCod: string): boolean {
    return this.cartService.getCurrent().Items.some(item => item.ProductCod === ProductCod);
  }

  public isDigital(product: ProductSearchEntity): boolean {
    return (product.IsDigital || 'N').toUpperCase() === 'S';
  }

  public visiblePrice(product: ProductSearchEntity): number {
    return this.money(Number(product.NumPrice || 0) * Math.max(1, Number(product.ProductUnitFactor || 1)));
  }

  public visibleStock(product: ProductSearchEntity): number {
    return Math.max(0, Math.floor(
      Number(product.NumPhysicalStock || 0) / Math.max(1, Number(product.ProductUnitFactor || 1))
    ));
  }

  public imageRoute(product: ProductSearchEntity): string | null {
    return product.FileRoute?.trim() || null;
  }

  private async findProducts(): Promise<void> {
    if (!this.Search.StoreCod || this.IsLoading) return;
    this.IsLoading = true;
    const response = await this.productDeliverySearchService.query(this.Search);
    this.IsLoading = false;
    this.HasAttemptedSearch = true;

    if (response.ErrorStatus) {
      this.Products = [];
      this.Page = new ResponsePageSearch<ProductSearchEntity>();
      this.toastrService.error(response.Message || 'No pudimos cargar los productos de la tienda.');
      return;
    }

    this.Page = Object.assign(new ResponsePageSearch<ProductSearchEntity>(), response.Data ?? {});
    this.Products = (this.Page.resultSearch || []).map(product => Object.assign(new ProductSearchEntity(), product));
  }

  private money(value: number): number {
    return Math.round(value * 100) / 100;
  }
}
