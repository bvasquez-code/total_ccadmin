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
import { ProductTaxConfigEntity } from '../../model/entity/ProductTaxConfigEntity';
import { TaxAffectationEntity } from 'src/app/enterprise/system/model/entity/TaxAffectationEntity';
import { TaxEntity } from 'src/app/enterprise/system/model/entity/TaxEntity';
import { ProductService } from '../../service/product.service';
import { IndicatorDto } from 'src/app/enterprise/shared/model/dto/IndicatorDto';

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
  TaxList: TaxEntity[] = [];
  TaxAffectationList: TaxAffectationEntity[] = [];
  MainTaxConfig: ProductTaxConfigEntity = new ProductTaxConfigEntity();
  AdditionalTaxConfigList: ProductTaxConfigEntity[] = [];
  NewTaxConfig: ProductTaxConfigEntity = new ProductTaxConfigEntity();
  IndDetailedTaxIndicator : IndicatorDto = new IndicatorDto();
  SimpleTaxOptionCod: string = "IGV";
  SimpleTaxOptionList: any[] = [
    {
      OptionCod: "IGV",
      Label: "IGV 18%",
      MainTaxCod: "1000",
      TaxAffectationCod: "10",
      AdditionalTaxList: []
    },
    {
      OptionCod: "EXONERATED",
      Label: "Exonerado",
      MainTaxCod: "9997",
      TaxAffectationCod: "20",
      AdditionalTaxList: []
    },
    {
      OptionCod: "UNAFFECTED",
      Label: "Inafecto",
      MainTaxCod: "9998",
      TaxAffectationCod: "30",
      AdditionalTaxList: []
    },
    {
      OptionCod: "IGV_ICBPER",
      Label: "IGV 18% + ICBPER 0.50",
      MainTaxCod: "1000",
      TaxAffectationCod: "10",
      AdditionalTaxList: [
        {
          TaxCod: "7152",
          FixedUnitAmount: 0.50
        }
      ]
    }
  ];

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
      this.TaxList = rpt.DataAdditional.find(e => e.Name === "taxList")?.Data ?? [];
      this.TaxAffectationList = rpt.DataAdditional.find(e => e.Name === "taxAffectationList")?.Data ?? [];
      this.IndDetailedTaxIndicator = rpt.DataAdditional.find(e => e.Name === "indDetailedTaxIndicator")?.Data ?? new IndicatorDto();
      this.loadTaxConfig(rpt.DataAdditional.find(e => e.Name === "productTaxConfigList")?.Data ?? []);
      this.OneStoreSearchTerm = this.getStoreLabel(this.SelectedStore);
      this.Config.ProductCod = this.ProductCod;
      this.Config.StoreCod = this.SelectedStoreCod;
      this.syncVisiblePriceFromInternal();
      this.loadStores(this.SelectedStoreCod);
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

  async loadStores(StoreCod : string): Promise<void> {
    const currentStore = this.StoreList.find(e => e.StoreCod === StoreCod);
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
      || this.normalize(store.Description).includes(query)
      || this.normalize(this.getStoreLabel(store)).includes(query);
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

  loadTaxConfig(configList: ProductTaxConfigEntity[]): void {
    const activeList = (configList || []).filter(e => e.Status !== "I");
    const main = activeList.find(e => e.IsMainTax === "S");
    this.MainTaxConfig = main ? this.copyTaxConfig(main) : this.createDefaultMainTaxConfig();
    this.AdditionalTaxConfigList = activeList
      .filter(e => e.IsMainTax !== "S")
      .map(e => this.copyTaxConfig(e));
    this.onMainAffectationChange(false);
    this.resetNewTaxConfig();
    this.syncSimpleTaxOptionFromConfig();
  }

  copyTaxConfig(config: ProductTaxConfigEntity): ProductTaxConfigEntity {
    const copy = new ProductTaxConfigEntity();
    copy.ProductTaxConfigId = config.ProductTaxConfigId;
    copy.ProductCod = config.ProductCod || this.ProductCod;
    copy.StoreCod = config.StoreCod || this.SelectedStoreCod;
    copy.TaxCod = config.TaxCod || "";
    copy.TaxAffectationCod = config.TaxAffectationCod || "";
    copy.IsMainTax = config.IsMainTax || "N";
    copy.TaxRateValue = Number(config.TaxRateValue || 0);
    copy.FixedUnitAmount = Number(config.FixedUnitAmount || 0);
    copy.TaxCalculationType = config.TaxCalculationType || "P";
    copy.IsInformative = config.IsInformative || "N";
    copy.CalculationOrder = Number(config.CalculationOrder || 100);
    copy.Status = config.Status || "A";
    return copy;
  }

  createDefaultMainTaxConfig(): ProductTaxConfigEntity {
    const config = new ProductTaxConfigEntity();
    config.ProductCod = this.ProductCod;
    config.StoreCod = this.SelectedStoreCod;
    config.TaxCod = "1000";
    config.TaxAffectationCod = "10";
    config.IsMainTax = "S";
    config.TaxRateValue = 18;
    config.FixedUnitAmount = 0;
    config.TaxCalculationType = "P";
    config.IsInformative = "N";
    config.CalculationOrder = 20;
    config.Status = "A";
    return config;
  }

  get mainAffectationList(): TaxAffectationEntity[] {
    return this.TaxAffectationList.filter(e => e.Status === "A" || !e.Status);
  }

  get isDetailedTaxMode(): boolean {
    return (this.IndDetailedTaxIndicator?.Value || "N").toUpperCase() === "S";
  }

  get selectedSimpleTaxOption(): any {
    return this.SimpleTaxOptionList.find(e => e.OptionCod === this.SimpleTaxOptionCod) || this.SimpleTaxOptionList[0];
  }

  syncSimpleTaxOptionFromConfig(): void {
    if (this.MainTaxConfig.TaxCod === "9997" && this.MainTaxConfig.TaxAffectationCod === "20") {
      this.SimpleTaxOptionCod = "EXONERATED";
      return;
    }
    if (this.MainTaxConfig.TaxCod === "9998" && this.MainTaxConfig.TaxAffectationCod === "30") {
      this.SimpleTaxOptionCod = "UNAFFECTED";
      return;
    }
    if (this.MainTaxConfig.TaxCod === "1000" && this.MainTaxConfig.TaxAffectationCod === "10"
      && this.AdditionalTaxConfigList.some(e => e.Status !== "I" && e.TaxCod === "7152")) {
      this.SimpleTaxOptionCod = "IGV_ICBPER";
      return;
    }
    this.SimpleTaxOptionCod = "IGV";
  }

  onSimpleTaxOptionChange(): void {
    this.applySimpleTaxOption();
  }

  applySimpleTaxOption(): void {
    const option = this.selectedSimpleTaxOption;
    this.MainTaxConfig = this.createTaxConfigFromOption(option.MainTaxCod, option.TaxAffectationCod, "S");
    this.AdditionalTaxConfigList = (option.AdditionalTaxList || [])
      .map((item: any) => this.createTaxConfigFromOption(item.TaxCod, "", "N", item.FixedUnitAmount));
    this.resetNewTaxConfig();
  }

  createTaxConfigFromOption(taxCod: string, taxAffectationCod: string, isMainTax: string, fixedUnitAmount?: number): ProductTaxConfigEntity {
    const config = new ProductTaxConfigEntity();
    config.ProductCod = this.ProductCod;
    config.StoreCod = this.SelectedStoreCod;
    config.TaxCod = taxCod;
    config.TaxAffectationCod = taxAffectationCod;
    config.IsMainTax = isMainTax;
    config.Status = "A";
    this.applyTaxDefaults(config, this.getTax(taxCod));
    this.applySimpleTaxFallbackDefaults(config, fixedUnitAmount);
    return config;
  }

  applySimpleTaxFallbackDefaults(config: ProductTaxConfigEntity, fixedUnitAmount?: number): void {
    if (config.TaxCod === "1000") {
      config.TaxCalculationType = "P";
      config.IsInformative = "N";
      config.TaxRateValue = 18;
      config.FixedUnitAmount = 0;
      config.CalculationOrder = 20;
    }
    if (config.TaxCod === "9997" || config.TaxCod === "9998" || config.TaxCod === "9995") {
      config.TaxCalculationType = "N";
      config.IsInformative = "S";
      config.TaxRateValue = 0;
      config.FixedUnitAmount = 0;
      config.CalculationOrder = 20;
    }
    if (config.TaxCod === "7152") {
      config.TaxCalculationType = "F";
      config.IsInformative = "N";
      config.TaxRateValue = 0;
      config.FixedUnitAmount = Number(fixedUnitAmount ?? config.FixedUnitAmount ?? 0.50);
      config.CalculationOrder = Number(config.CalculationOrder || 100);
    }
  }

  get additionalTaxOptions(): TaxEntity[] {
    const activeTaxCodSet = new Set(this.taxConfigTableList.map(config => config.TaxCod));
    const mainAffectationTaxCodSet = this.getMainAffectationTaxCodSet();
    return this.TaxList
      .filter(e => e.Status === "A" || !e.Status)
      .filter(e => !mainAffectationTaxCodSet.has(e.TaxCod))
      .filter(e => !activeTaxCodSet.has(e.TaxCod));
  }

  get taxConfigTableList(): ProductTaxConfigEntity[] {
    return [this.MainTaxConfig, ...this.AdditionalTaxConfigList]
      .filter(e => e.Status !== "I" && Boolean(e.TaxCod));
  }

  getTax(taxCod: string): TaxEntity | undefined {
    return this.TaxList.find(e => e.TaxCod === taxCod);
  }

  getAffectation(taxAffectationCod: string): TaxAffectationEntity | undefined {
    return this.TaxAffectationList.find(e => e.TaxAffectationCod === taxAffectationCod);
  }

  getMainAffectationTaxCodSet(): Set<string> {
    return new Set(
      this.TaxAffectationList
        .filter(e => e.Status === "A" || !e.Status)
        .map(e => e.TaxCod)
    );
  }

  getTaxLabel(taxCod: string): string {
    const tax = this.getTax(taxCod);
    return tax ? `${tax.TaxCod} - ${tax.Name}` : taxCod;
  }

  getAffectationTaxLabel(taxAffectationCod: string): string {
    const affectation = this.getAffectation(taxAffectationCod);
    return affectation ? this.getTaxLabel(affectation.TaxCod) : "";
  }

  onMainAffectationChange(removeInvalidAdditional: boolean = true): void {
    const affectation = this.getAffectation(this.MainTaxConfig.TaxAffectationCod);
    if (!affectation) return;
    this.MainTaxConfig.IsMainTax = "S";
    this.MainTaxConfig.TaxCod = affectation.TaxCod;
    this.MainTaxConfig.Status = "A";
    this.applyTaxDefaults(this.MainTaxConfig, this.getTax(affectation.TaxCod));
    if (removeInvalidAdditional && affectation.TaxAffectationCod !== "10") {
      this.AdditionalTaxConfigList = this.AdditionalTaxConfigList.filter(e => e.TaxCod !== "1000");
    }
    this.resetNewTaxConfig();
  }

  resetNewTaxConfig(): void {
    const config = new ProductTaxConfigEntity();
    config.ProductCod = this.ProductCod;
    config.StoreCod = this.SelectedStoreCod;
    config.IsMainTax = "N";
    config.Status = "A";
    config.CalculationOrder = 100;
    this.NewTaxConfig = config;
  }

  onNewTaxChange(): void {
    this.applyTaxDefaults(this.NewTaxConfig, this.getTax(this.NewTaxConfig.TaxCod));
  }

  addAdditionalTax(): void {
    const tax = this.getTax(this.NewTaxConfig.TaxCod);
    if (!tax) {
      this.toastrService.error("Debe seleccionar un tributo adicional.");
      return;
    }
    if (!this.additionalTaxOptions.some(e => e.TaxCod === tax.TaxCod)) {
      this.toastrService.error("El tributo seleccionado no esta disponible para este producto/local.");
      return;
    }
    const config = this.copyTaxConfig(this.NewTaxConfig);
    config.ProductCod = this.ProductCod;
    config.StoreCod = this.SelectedStoreCod;
    config.TaxCod = tax.TaxCod;
    config.TaxAffectationCod = "";
    config.IsMainTax = "N";
    config.Status = "A";
    config.ProductTaxConfigId = undefined;
    this.AdditionalTaxConfigList.push(config);
    this.resetNewTaxConfig();
  }

  removeAdditionalTax(index: number): void {
    this.AdditionalTaxConfigList.splice(index, 1);
  }

  removeTaxConfig(config: ProductTaxConfigEntity): void {
    if (config.IsMainTax === "S") {
      config.Status = "I";
      config.TaxCod = "";
      config.TaxAffectationCod = "";
      this.resetNewTaxConfig();
      return;
    }
    const index = this.AdditionalTaxConfigList.indexOf(config);
    if (index >= 0) {
      this.removeAdditionalTax(index);
      this.resetNewTaxConfig();
    }
  }

  onAdditionalTaxChange(config: ProductTaxConfigEntity): void {
    this.applyTaxDefaults(config, this.getTax(config.TaxCod));
  }

  applyTaxDefaults(config: ProductTaxConfigEntity, tax?: TaxEntity): void {
    if (!tax) return;
    config.TaxCalculationType = tax.TaxCalculationType || "P";
    config.IsInformative = tax.IsInformative || "N";
    config.CalculationOrder = Number(tax.CalculationOrder || 100);
    config.TaxRateValue = tax.TaxCod === "1000" ? 18 : Number(tax.TaxRateValue || 0);
    config.FixedUnitAmount = Number(tax.FixedUnitAmount || 0);
    if (config.TaxCalculationType === "F") {
      config.TaxRateValue = 0;
    }
    if (config.TaxCalculationType === "N") {
      config.TaxRateValue = 0;
      config.FixedUnitAmount = 0;
    }
  }

  isPercentTax(config: ProductTaxConfigEntity): boolean {
    return config.TaxCalculationType === "P" && config.IsInformative !== "S";
  }

  isFixedTax(config: ProductTaxConfigEntity): boolean {
    return config.TaxCalculationType === "F";
  }

  canEditTaxRate(config: ProductTaxConfigEntity): boolean {
    return config.IsMainTax !== "S" && config.TaxCod !== "1000" && this.isPercentTax(config);
  }

  canEditFixedAmount(config: ProductTaxConfigEntity): boolean {
    return config.IsMainTax !== "S" && this.isFixedTax(config);
  }

  canEditCalculationOrder(config: ProductTaxConfigEntity): boolean {
    return config.IsMainTax !== "S";
  }

  canRemoveTaxConfig(config: ProductTaxConfigEntity): boolean {
    return true;
  }

  getCalculationTypeLabel(config: ProductTaxConfigEntity): string {
    if (config.TaxCalculationType === "P") return "Porcentaje";
    if (config.TaxCalculationType === "F") return "Monto fijo";
    return "No aplica";
  }

  isAdditionalTaxOptionDisabled(taxCod: string, index: number): boolean {
    return taxCod === this.MainTaxConfig.TaxCod
      || this.AdditionalTaxConfigList.some((e, ix) => ix !== index && e.TaxCod === taxCod);
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
      this.validateTaxConfig();
      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    }
  }

  validateTaxConfig(): void {
    if (!this.isDetailedTaxMode) {
      this.applySimpleTaxOption();
    }
    const activeConfigList = [this.MainTaxConfig, ...this.AdditionalTaxConfigList]
      .filter(e => e.Status !== "I" && Boolean(e.TaxCod));
    if (activeConfigList.length === 0) {
      throw new Error("Debe registrar al menos un tributo para el producto/local");
    }
    const activeMainList = activeConfigList.filter(e => e.IsMainTax === "S");
    if (activeMainList.length !== 1) {
      throw new Error("Debe registrar una sola afectacion principal activa");
    }

    const mainConfig = activeMainList[0];
    ValidationHelper.validateIsNotEmpty(mainConfig.TaxAffectationCod, "Debe seleccionar afectacion tributaria principal");
    const affectation = this.getAffectation(mainConfig.TaxAffectationCod);
    if (!affectation) {
      throw new Error("Afectacion tributaria principal no existe");
    }
    if (mainConfig.TaxCod !== affectation.TaxCod) {
      throw new Error("La afectacion principal no corresponde al tributo seleccionado");
    }

    const taxSet = new Set<string>();
    const mainAffectationTaxCodSet = this.getMainAffectationTaxCodSet();
    for (const config of activeConfigList) {
      ValidationHelper.validateIsNotEmpty(config.TaxCod, "Debe seleccionar tributo");
      if (config.TaxCod === "1000") {
        config.TaxRateValue = 18;
      }
      if (config.IsMainTax !== "S" && mainAffectationTaxCodSet.has(config.TaxCod)) {
        throw new Error("Los tributos de afectacion IGV solo pueden configurarse como principal");
      }
      if (taxSet.has(config.TaxCod)) {
        throw new Error("No se puede duplicar el mismo tributo activo para el producto/local");
      }
      taxSet.add(config.TaxCod);

      if (config.TaxCalculationType === "P" && config.IsInformative !== "S" && Number(config.TaxRateValue || 0) <= 0) {
        throw new Error("Tributo porcentual requiere tasa mayor a cero");
      }
      if (config.TaxCalculationType === "F" && Number(config.TaxRateValue || 0) > 0) {
        throw new Error("Tributo de monto fijo no debe tener tasa porcentual");
      }
    }

    if (affectation.TaxAffectationCod !== "10" && taxSet.has("1000")) {
      throw new Error("Producto exonerado, inafecto o exportacion no puede tener IGV real calculado");
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
    request.TaxConfigList = this.buildTaxConfigRequestList();

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

  getTargetStoreCodList(): string[] {
    if (this.ScopeMode === "ALL") return this.StoreList.map(e => e.StoreCod);
    if (this.ScopeMode === "ONE") return [this.SelectedStoreCod];
    return this.getSelectedStoreCodList();
  }

  buildTaxConfigRequestList(): ProductTaxConfigEntity[] {
    if (!this.isDetailedTaxMode) {
      this.applySimpleTaxOption();
    }
    return [this.MainTaxConfig, ...this.AdditionalTaxConfigList]
      .filter(config => config.Status !== "I" && Boolean(config.TaxCod))
      .map(config => {
      const copy = this.copyTaxConfig(config);
      copy.ProductCod = this.ProductCod;
      copy.StoreCod = this.SelectedStoreCod;
      copy.Status = "A";
      if (copy.TaxCod === "1000") {
        copy.TaxRateValue = 18;
      }
      copy.ProductTaxConfigId = undefined;
      return copy;
    });
  }

  cancel(): void {
    this.router.navigate(['/enterprise/product/pages/listProduct']);
  }
}
