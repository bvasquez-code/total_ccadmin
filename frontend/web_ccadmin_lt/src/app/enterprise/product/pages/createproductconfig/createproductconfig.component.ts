import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import Swal from 'sweetalert2';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { StoreService } from 'src/app/enterprise/store/service/store.service';
import { ProductConfigStoreUpdateDto } from '../../model/dto/ProductConfigStoreUpdateDto';
import { ProductConfigEntity } from '../../model/entity/ProductConfigEntity';
import { ProductEntity } from '../../model/entity/ProductEntity';
import { ProductService } from '../../service/product.service';

@Component({
  selector: 'app-createproductconfig',
  templateUrl: './createproductconfig.component.html'
})
export class CreateproductconfigComponent implements OnInit {

  ProductCod: string = "";
  CurrentStoreCod: string = "";
  Product: ProductEntity = new ProductEntity();
  Config: ProductConfigEntity = new ProductConfigEntity();
  SelectedStore: StoreEntity = new StoreEntity();
  StoreList: StoreEntity[] = [];
  ScopeMode: string = "ONE";
  SelectedStoreCod: string = "";
  SelectedStoreMap: { [storeCod: string]: boolean } = {};
  OneStoreSearchTerm: string = "";
  ShowOneStoreDropdown: boolean = false;
  StoreQuery: string = "";
  StorePage: number = 1;
  StorePageSize: number = 10;
  VisibleUnitPrice: number = 0;

  constructor(
    private productService: ProductService,
    private storeService: StoreService,
    private dataSesionService: DataSesionService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    const urlTree: any = this.router.parseUrl(this.router.url);
    this.ProductCod = urlTree.queryParams['ProductCod'] ?? "";
    this.CurrentStoreCod = this.dataSesionService.getSessionStorageDto().StoreCod;
    this.SelectedStoreCod = this.CurrentStoreCod;
  }

  ngOnInit(): void {
    this.FindDataForm(this.CurrentStoreCod);
  }

  async FindDataForm(StoreCod: string): Promise<void> {
    if (!this.ProductCod) {
      this.toastrService.error("Debe seleccionar un producto.");
      this.cancel();
      return;
    }

    const rpt = await this.productService.FindDataConfigForm(this.ProductCod, StoreCod);

    if (!rpt.ErrorStatus) {
      this.Product = rpt.DataAdditional.find(e => e.Name === "product")?.Data ?? new ProductEntity();
      this.Config = this.copyConfig(rpt.DataAdditional.find(e => e.Name === "config")?.Data ?? new ProductConfigEntity());
      this.SelectedStore = rpt.DataAdditional.find(e => e.Name === "store")?.Data ?? new StoreEntity();
      this.SelectedStoreCod = this.SelectedStore.StoreCod || StoreCod;
      this.StoreList = rpt.DataAdditional.find(e => e.Name === "storeList")?.Data ?? [];
      this.OneStoreSearchTerm = this.getStoreLabel(this.SelectedStore);
      this.Config.ProductCod = this.ProductCod;
      this.Config.StoreCod = this.SelectedStoreCod;
      this.syncVisiblePriceFromInternal();
      this.loadStores();
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

  async loadStores(): Promise<void> {
    const currentStore = this.StoreList.find(e => e.StoreCod === this.CurrentStoreCod);
      if (currentStore) {
        this.SelectedStore = currentStore;
        this.OneStoreSearchTerm = this.getStoreLabel(currentStore);
      }
  }

  changeScopeMode(mode: string): void {
    this.ScopeMode = mode;
    this.StoreQuery = "";
    this.StorePage = 1;
    this.ShowOneStoreDropdown = false;
    if (this.ScopeMode === "ONE" && this.SelectedStoreCod) {
      this.FindDataForm(this.SelectedStoreCod);
    }
  }

  getStoreLabel(store: StoreEntity): string {
    return store?.StoreCod ? `${store.StoreCod} - ${store.Name}` : "";
  }

  get filteredOneStores(): StoreEntity[] {
    const query = this.normalize(this.OneStoreSearchTerm);
    if (!query) return this.StoreList.slice(0, 20);
    return this.StoreList
      .filter(store => this.storeMatchesQuery(store, query))
      .slice(0, 20);
  }

  get filteredSomeStores(): StoreEntity[] {
    const query = this.normalize(this.StoreQuery);
    if (!query) return this.StoreList;
    return this.StoreList.filter(store => this.storeMatchesQuery(store, query));
  }

  get pagedSomeStores(): StoreEntity[] {
    const start = (this.StorePage - 1) * this.StorePageSize;
    return this.filteredSomeStores.slice(start, start + this.StorePageSize);
  }

  get totalStorePages(): number {
    return Math.max(1, Math.ceil(this.filteredSomeStores.length / this.StorePageSize));
  }

  storeMatchesQuery(store: StoreEntity, query: string): boolean {
    return this.normalize(store.StoreCod).includes(query)
      || this.normalize(store.Name).includes(query)
      || this.normalize(store.Description).includes(query);
  }

  normalize(value: string): string {
    return (value || "").toLowerCase().trim();
  }

  onStoreQueryChange(): void {
    this.StorePage = 1;
  }

  setStorePage(page: number): void {
    if (page < 1 || page > this.totalStorePages) return;
    this.StorePage = page;
  }

  async selectOneStore(store: StoreEntity): Promise<void> {
    this.SelectedStore = store;
    this.SelectedStoreCod = store.StoreCod;
    this.OneStoreSearchTerm = this.getStoreLabel(store);
    this.ShowOneStoreDropdown = false;
    await this.FindDataForm(store.StoreCod);
  }

  onOneStoreBlur(): void {
    setTimeout(() => {
      this.ShowOneStoreDropdown = false;
      this.OneStoreSearchTerm = this.getStoreLabel(this.SelectedStore);
    }, 200);
  }

  toggleStore(store: StoreEntity): void {
    this.SelectedStoreMap[store.StoreCod] = !this.SelectedStoreMap[store.StoreCod];
  }

  isStoreSelected(store: StoreEntity): boolean {
    return Boolean(this.SelectedStoreMap[store.StoreCod]);
  }

  getSelectedStoreCodList(): string[] {
    return Object.keys(this.SelectedStoreMap).filter(storeCod => this.SelectedStoreMap[storeCod]);
  }

  getSelectedStoreCount(): number {
    if (this.ScopeMode === "ALL") return this.StoreList.length;
    if (this.ScopeMode === "ONE") return this.SelectedStoreCod ? 1 : 0;
    return this.getSelectedStoreCodList().length;
  }

  copyConfig(config: ProductConfigEntity): ProductConfigEntity {
    const copy = new ProductConfigEntity();
    copy.ProductCod = config.ProductCod;
    copy.StoreCod = config.StoreCod;
    copy.NumPrice = Number(config.NumPrice || 0);
    copy.NumMaxStock = Number(config.NumMaxStock || 0);
    copy.NumMinStock = Number(config.NumMinStock || 0);
    copy.IsDiscontable = config.IsDiscontable;
    copy.DiscountType = config.DiscountType;
    copy.NumDiscountMax = Number(config.NumDiscountMax || 0);
    copy.ProductUnitName = config.ProductUnitName || "NIU";
    copy.ProductUnitFactor = ProductUnitHelper.normalizeFactor(Number(config.ProductUnitFactor || 1));
    copy.Version = config.Version || "V.1";
    return copy;
  }

  getProductUnitName(): string {
    return this.Config.ProductUnitName || "NIU";
  }

  getProductUnitFactor(): number {
    return ProductUnitHelper.normalizeFactor(Number(this.Config.ProductUnitFactor || 1));
  }

  onUnitConfigChange(): void {
    if (!this.Config.ProductUnitName) {
      this.Config.ProductUnitName = "NIU";
    }
    this.Config.ProductUnitFactor = this.getProductUnitFactor();
    this.syncVisiblePriceFromInternal();
  }

  syncVisiblePriceFromInternal(): void {
    this.VisibleUnitPrice = ProductUnitHelper.toVisibleUnitPrice(Number(this.Config.NumPrice || 0), this.getProductUnitFactor());
  }

  syncInternalPriceFromVisible(): void {
    this.Config.NumPrice = ProductUnitHelper.toInternalUnitPrice(Number(this.VisibleUnitPrice || 0), this.getProductUnitFactor());
  }

  getTargetSummary(): string {
    if (this.ScopeMode === "ALL") return "todas las tiendas";
    if (this.ScopeMode === "ONE") return `la tienda ${this.SelectedStoreCod}`;
    return `${this.getSelectedStoreCount()} tienda(s)`;
  }

  validate(): boolean {
    try {
      ValidationHelper.validateIsNotEmpty(this.ProductCod, "Debe seleccionar un producto");
      ValidationHelper.validNumber(this.Config.NumMaxStock, null, 0, "Stock maximo no valido");
      ValidationHelper.validNumber(this.Config.NumMinStock, null, 0, "Stock minimo no valido");
      ValidationHelper.validateIsNotEmpty(this.Config.ProductUnitName, "Debe ingresar la unidad de venta");
      ValidationHelper.validNumber(this.Config.ProductUnitFactor, null, 1, "Factor de operacion no valido");
      ValidationHelper.validateIsNotEmpty(this.Config.NumPrice, "Debe ingresar un precio para el producto");
      ValidationHelper.validNumber(this.Config.NumPrice, null, 0, "Precio por NIU no valido");
      if (this.ScopeMode === "ONE") {
        ValidationHelper.validateIsNotEmpty(this.SelectedStoreCod, "Debe seleccionar una tienda");
      }
      if (this.ScopeMode === "SOME" && this.getSelectedStoreCodList().length === 0) {
        throw new Error("Debe seleccionar al menos una tienda");
      }
      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  async save(): Promise<void> {
    this.syncInternalPriceFromVisible();
    if (!this.validate()) return;

    const result = await Swal.fire({
      title: 'Confirmar configuracion',
      text: `Se actualizara ${this.getTargetSummary()} para el producto ${this.ProductCod}.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Si, guardar',
      cancelButtonText: 'No, cancelar'
    });

    if (!result.isConfirmed) return;

    const request = new ProductConfigStoreUpdateDto();
    request.ProductCod = this.ProductCod;
    request.config = this.Config;

    if (this.ScopeMode === "ALL") {
      request.ApplyAllStores = true;
    } else if (this.ScopeMode === "ONE") {
      request.StoreCod = this.SelectedStoreCod;
    } else {
      request.StoreCodList = this.getSelectedStoreCodList();
    }

    const rpt = await this.productService.SaveConfigByStores(request);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operacion realizada con exito.");
      this.cancel();
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

  cancel(): void {
    this.router.navigate(['/enterprise/product/pages/listProduct']);
  }
}
