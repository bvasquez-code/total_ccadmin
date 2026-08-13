import { Component, ElementRef, HostListener, Input, OnInit, ViewChild } from '@angular/core';
import { ProductSearchDto } from 'src/app/enterprise/product/model/dto/ProductSearchDto';
import { ProductSearchService } from 'src/app/enterprise/product/service/productsearch.service';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { ProductSearchEntity } from '../../../product/model/entity/ProductSearchEntity';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { RespuestaPaginacionDto } from 'src/app/enterprise/compartido/entity/RespuestaPaginacionDto';
import { InfoPaginaDto } from 'src/app/enterprise/compartido/entity/InfoPaginaDto';
import { ProductService } from '../../../product/service/product.service';
import { ProductInfoDto } from 'src/app/enterprise/product/model/dto/ProductInfoDto';
import { PresaleRegisterDto } from '../../model/dto/PresaleRegisterDto';
import { ShoppingCartService } from '../../service/shoppingcart.service';
import { ProductVariantEntity } from 'src/app/enterprise/product/model/entity/ProductVariantEntity';
import { PresaleService } from '../../service/presale.service';
import { CurrencyEntity } from 'src/app/enterprise/shared/model/entity/CurrencyEntity';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';
import { PresaleDetailDto } from '../../model/dto/PresaleDetailDto';
import { SaleDetailDto } from '../../model/dto/SaleDetailDto';
import { ClientService } from '../../../client/service/client.service';
import { ClientEntity } from '../../../client/model/entity/ClientEntity';
import Swal from 'sweetalert2';
import { PaginationUtil } from '../../utility/PaginationUtility';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { CreditNoteService } from '../../service/CreditNote.service';
import { CreditNoteDetailDto } from '../../model/dto/CreditNoteDetailDto';
import { IndicatorDto } from 'src/app/enterprise/shared/model/dto/IndicatorDto';

@Component({
  selector: 'app-createpresale',
  templateUrl: './createpresale.component.html',
  styleUrls: ['./createpresale.component.css']
})
export class CreatepresaleComponent implements OnInit {

  @Input() ResultFormClient: object | undefined;

  @ViewChild('txt_filtro_busqueda', { static: false }) txt_filtro_busqueda!: ElementRef<HTMLInputElement>;
  @ViewChild('txt_NumUnit', { static: false }) txt_NumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDocumentNum', { static: false }) txtDocumentNum!: ElementRef<HTMLInputElement>;
  @ViewChild('cboDocumentType') cboDocumentType!: ElementRef<HTMLSelectElement>;

  @ViewChild('cboSortedBy') cboSortedBy!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboStockMin') cboStockMin!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboDirectionSortedBy') cboDirectionSortedBy!: ElementRef<HTMLSelectElement>;

  productSearch: ProductSearchDto = new ProductSearchDto();
  responsePageSearch: ResponsePageSearch<ProductSearchEntity> = new ResponsePageSearch();
  productList: ProductSearchEntity[] = [];
  productListHtml: ProductSearchEntity[][] = [];
  productInfoDtoSelect: ProductInfoDto = new ProductInfoDto();
  NumPhysicalStockTotal: number = 0;

  ShoppingCart: PresaleRegisterDto = new PresaleRegisterDto();
  CurrencySystem: CurrencyEntity = new CurrencyEntity();
  IndManualDiscount: IndicatorDto = new IndicatorDto();
  ShoppingCartResult: PresaleDetailDto = new PresaleDetailDto();
  SaleDetail: SaleDetailDto = new SaleDetailDto();
  PresaleDetail: PresaleDetailDto = new PresaleDetailDto();
  CreditNoteDetail: CreditNoteDetailDto = new CreditNoteDetailDto();
  CreditNoteCod: string = "";
  isProductExchangeMode: boolean = false;
  isProductExchangeLoading: boolean = false;
  productExchangeLoadError: boolean = false;
  creditNoteBalance: number = 0;
  ShowClientRegister: boolean = false;
  ShowClient: boolean = false;
  ShowClientSearch: boolean = false;

  DocumentType: string = "";
  DocumentNum: string = "";

  lastKeypressTime: number = 0;
  inputBuffer: string = '';

  RptSearchProduct: RespuestaPaginacionDto<ProductSearchEntity> = new RespuestaPaginacionDto();
  ButtonList: InfoPaginaDto[] = [];


  constructor(
    private productSearchService: ProductSearchService
    , private session: DataSesionService
    , private productService: ProductService
    , private shoppingCartService: ShoppingCartService
    , private presaleService: PresaleService
    , private toastrService: ToastrService
    , private router: Router
    , private clientService: ClientService
    , private creditNoteService: CreditNoteService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.ShoppingCart.Headboard.PresaleCod = urlTree.queryParams['PresaleCod'];
    this.CreditNoteCod = urlTree.queryParams['CreditNoteCod'] ?? "";
    this.isProductExchangeLoading = !!this.CreditNoteCod;
    this.productSearch.StoreCod = this.session.getSessionStorageDto().StoreCod;
    this.productSearch.Page = 1;
    this.findAllProduct();
    this.findDataForm(this.ShoppingCart.Headboard.PresaleCod);
  }

  async ngOnInit(): Promise<void> {
    if (!this.ShoppingCart.Headboard.PresaleCod) {
      this.Clean();
    }
    this.shoppingCartService.Init();
    this.updateShoppingCart();
    if (this.CreditNoteCod) {
      await this.loadProductExchange();
    }
  }

  async loadProductExchange(): Promise<void> {
    const response: ResponseWsDto = await this.creditNoteService.FindById(this.CreditNoteCod);
    if (response?.ErrorStatus || !response?.Data) {
      this.isProductExchangeLoading = false;
      this.productExchangeLoadError = true;
      this.toastrService.error(response?.Message || "No se pudo cargar la nota de credito.");
      return;
    }

    const creditNoteDetail: CreditNoteDetailDto = response.Data;
    const creditNoteHead = creditNoteDetail.Headboard;
    const availableBalance: number = this.toMoney(
      Number(creditNoteDetail.NumAvailableBalance ?? creditNoteHead?.NumTotalPrice ?? 0)
    );

    if (creditNoteHead?.CreditNoteStatus !== "C" || creditNoteHead?.IsProductExchange !== "S") {
      this.isProductExchangeLoading = false;
      this.productExchangeLoadError = true;
      this.toastrService.error("La nota de credito no esta habilitada para cambio de producto.");
      return;
    }
    if (availableBalance <= 0) {
      this.isProductExchangeLoading = false;
      this.productExchangeLoadError = true;
      this.toastrService.error("La nota de credito ya no tiene saldo disponible.");
      return;
    }

    this.CreditNoteDetail = creditNoteDetail;
    this.creditNoteBalance = availableBalance;
    this.isProductExchangeMode = true;
    this.isProductExchangeLoading = false;
    this.productExchangeLoadError = false;

    if (creditNoteHead.ClientCod && creditNoteDetail.Client) {
      this.shoppingCartService.AddClient(creditNoteDetail.Client);
      this.updateShoppingCart();
    }
  }

  isProductExchangeTotalValid(): boolean {
    if (this.CreditNoteCod && (this.isProductExchangeLoading || this.productExchangeLoadError)) {
      return false;
    }
    return !this.isProductExchangeMode
      || this.toMoney(this.ShoppingCart.Headboard.NumTotalPrice) >= this.creditNoteBalance;
  }

  getProductExchangeDifference(): number {
    return this.toMoney(Math.max(
      Number(this.ShoppingCart.Headboard.NumTotalPrice || 0) - this.creditNoteBalance,
      0
    ));
  }

  private validateProductExchangeTotal(): boolean {
    if (this.isProductExchangeTotalValid()) return true;

    this.toastrService.error(
      `El total de la nueva compra debe ser igual o mayor a ${this.creditNoteBalance.toFixed(2)}.`
    );
    return false;
  }

  private toMoney(value: number): number {
    return Math.round(Number(value || 0) * 100) / 100;
  }

  async createCode() {
    const response: ResponseWsDto = await this.presaleService.createCode();

    if (response.ErrorStatus) return;
    this.ShoppingCart.Headboard.PresaleCod = String(response.Data);
  }

  async findDataForm(PresaleCod: string) {
    if (!PresaleCod) PresaleCod = "";
    const response: ResponseWsDto = await this.presaleService.findDataForm(PresaleCod);

    if (!response.ErrorStatus) {
      this.CurrencySystem = response.DataAdditional.find(e => e.Name === "CurrencySystem")?.Data;
      this.IndManualDiscount = response.DataAdditional.find(e => e.Name === "IndManualDiscount")?.Data
        ?? new IndicatorDto();
      this.PresaleDetail = response.DataAdditional.find(e => e.Name === "PresaleDetail")?.Data;

      if (this.PresaleDetail) {
        setTimeout(() => { this.SetCart(this.PresaleDetail); }, 100);
      }
    }
  }

  async findAllProduct(IsBarcodeReaderInput: boolean = false) {
    const response: ResponseWsDto = await this.productSearchService.query(this.productSearch);

    if (!response.ErrorStatus) {
      this.responsePageSearch = response.Data;
      this.productList = this.responsePageSearch.resultSearch;
      this.productListHtml = PaginationUtil.organizeElement(this.productList, 4);

      this.ButtonList = [];
      this.RptSearchProduct.addResultPage(this.responsePageSearch);
      this.ButtonList = PaginationUtil.buildButtons(this.RptSearchProduct);

      if (IsBarcodeReaderInput) {
        this.addUnitDirect(this.productList[0].ProductCod);
        this.txt_filtro_busqueda.nativeElement.value = "";
      }
    }

    this.txt_filtro_busqueda.nativeElement.focus();
  }

  async addUnitDirect(ProductCod: string) {
    const response: ResponseWsDto = await this.productService.findDetailById(
      ProductCod, this.session.getSessionStorageDto().StoreCod
    );
    const productInfoDto: ProductInfoDto = response.Data;

    this.addUnit(productInfoDto, productInfoDto.VariantList[0]);
  }

  async subtractUnitDirect(ProductCod: string) {
    const response: ResponseWsDto = await this.productService.findDetailById(
      ProductCod, this.session.getSessionStorageDto().StoreCod
    );
    const productInfoDto: ProductInfoDto = response.Data;

    this.subtractUnit(productInfoDto, productInfoDto.VariantList[0]);
  }


  filterProduct(p_num_pagina_busqueda: number = 1, IsBarcodeReaderInput: boolean = false) {
    if (p_num_pagina_busqueda <= 0) return;

    this.productSearch.StoreCod = this.session.getSessionStorageDto().StoreCod;
    this.productSearch.Query = this.txt_filtro_busqueda.nativeElement.value;
    this.productSearch.Page = p_num_pagina_busqueda;
    this.productSearch.SortedBy = this.cboSortedBy.nativeElement.value;
    this.productSearch.StockMin = (this.cboStockMin.nativeElement.value === "S") ? 1 : 0;
    this.productSearch.DirectionSortedBy = this.cboDirectionSortedBy.nativeElement.value;
    this.findAllProduct(IsBarcodeReaderInput);
  }

  async findDetailById(ProductCod: string) {
    const response: ResponseWsDto = await this.productService.findDetailById(
      ProductCod, this.session.getSessionStorageDto().StoreCod
    );

    if (!response.ErrorStatus) {
      this.productInfoDtoSelect = response.Data;
      this.NumPhysicalStockTotal = this.productInfoDtoSelect.InfoList.map(item => item.NumPhysicalStock).reduce((a, b) => a + b, 0);
    }

  }

  async SetCart(PresaleDetail: PresaleDetailDto) {
    for (let Product of PresaleDetail.DetailList) {
      const response: ResponseWsDto = await this.productService.findDetailById(
        Product.ProductCod,
        this.session.getSessionStorageDto().StoreCod
      );

      Product.ProductInfo = response.Data;
    }

    this.shoppingCartService.SetCart(PresaleDetail);
    this.updateShoppingCart();
  }

  addUnit(ProductInfo: ProductInfoDto, ProductVariant: ProductVariantEntity) {
    this.shoppingCartService.addUnit(ProductInfo, ProductVariant);
    this.updateShoppingCart();
  }

  subtractUnit(ProductInfo: ProductInfoDto, ProductVariant: ProductVariantEntity) {
    if (this.shoppingCartService.preventZeroSubtract(ProductInfo, ProductVariant)) {

      Swal.fire({
        title: '¿Estás seguro?',
        text: 'Al modificar a 0 unidades el producto se eliminara del carrito',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'No, cancelar'
      }).then(async (result) => {
        if (result.isConfirmed) {
          this.shoppingCartService.subtractUnit(ProductInfo, ProductVariant);
          this.updateShoppingCart();
        }
      });
    } else {
      this.shoppingCartService.subtractUnit(ProductInfo, ProductVariant);
      this.updateShoppingCart();
    }

  }

  HandbookUnit(ProductInfo: ProductInfoDto, ProductVariant: ProductVariantEntity) {
    try {
      this.shoppingCartService.HandbookUnit(ProductInfo, ProductVariant, Number(this.txt_NumUnit.nativeElement.value));
      this.updateShoppingCart();
    } catch (e) {
      this.txt_NumUnit.nativeElement.value = "0";
      this.updateShoppingCart();
    }
  }

  applyManualDiscount(
    ProductInfo: ProductInfoDto,
    ProductVariant: ProductVariantEntity,
    event: Event
  ): void {
    const input = event.target as HTMLInputElement;
    try {
      const discount = this.clampManualDiscountInput(ProductInfo, input, false);
      this.shoppingCartService.setManualDiscount(ProductInfo, ProductVariant, discount);
      this.updateShoppingCart();
      input.value = String(this.getManualDiscountInput(ProductInfo, ProductVariant));
    } catch (error: any) {
      input.value = String(this.getManualDiscountInput(ProductInfo, ProductVariant));
      this.toastrService.error(error.message);
    }
  }

  enforceManualDiscountMaximum(ProductInfo: ProductInfoDto, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.clampManualDiscountInput(ProductInfo, input, true);
  }

  private clampManualDiscountInput(
    ProductInfo: ProductInfoDto,
    input: HTMLInputElement,
    showWarning: boolean
  ): number {
    const maximum = this.getManualDiscountMaximum(ProductInfo);
    let value = Number(input.value || 0);
    if (!Number.isFinite(value)) value = 0;
    if (value < 0) value = 0;

    if (value > maximum) {
      value = maximum;
      input.value = String(maximum);
      if (showWarning) {
        const limit = this.isPercentageManualDiscount(ProductInfo)
          ? `${maximum}%`
          : `${this.CurrencySystem.CurrencyCod} ${maximum.toFixed(2)}`;
        this.toastrService.warning(
          `El descuento maximo permitido es ${limit}. Se reemplazo el valor por el limite configurado.`
        );
      }
    }
    return value;
  }

  updateShoppingCart() {
    this.ShoppingCart = this.shoppingCartService.getCart();
  }

  getTotalProduct(ProductCod: string): number {
    return this.shoppingCartService.getTotalProduct(ProductCod);
  }

  getTotalProductVariant(ProductCod: string, Variant: string): number {
    return this.shoppingCartService.getTotalProductVariant(ProductCod, Variant);
  }

  getTotalProductVisible(ProductCod: string): number {
    return this.shoppingCartService.getTotalProductVisible(ProductCod);
  }

  getTotalProductVariantVisible(ProductCod: string, Variant: string): number {
    return this.shoppingCartService.getTotalProductVariantVisible(ProductCod, Variant);
  }

  getVisibleStock(stock: number, ProductUnitFactor: number): number {
    return ProductUnitHelper.toVisibleQuantity(stock, ProductUnitFactor);
  }

  isDigitalSearchProduct(product: ProductSearchEntity): boolean {
    return (product?.IsDigital || "N").trim().toUpperCase() === "S";
  }

  isDigitalSelectedProduct(): boolean {
    return (this.productInfoDtoSelect?.Config?.IsDigital || "N").trim().toUpperCase() === "S";
  }

  getVisibleQuantity(internalQuantity: number, ProductUnitFactor: number): number {
    return ProductUnitHelper.toVisibleQuantity(internalQuantity, ProductUnitFactor);
  }

  getVisibleUnitPrice(internalUnitPrice: number, ProductUnitFactor: number): number {
    return ProductUnitHelper.toVisibleUnitPrice(internalUnitPrice, ProductUnitFactor);
  }

  getProductUnitName(): string {
    return this.productInfoDtoSelect.Config.ProductUnitName || 'NIU';
  }

  get isManualDiscountEnabled(): boolean {
    return this.IndManualDiscount?.Indicator === "IND_MANUAL_DISCOUNT"
      && (this.IndManualDiscount?.Value || "N").trim().toUpperCase() === "S";
  }

  canApplyManualDiscount(ProductInfo: ProductInfoDto): boolean {
    const config = ProductInfo?.Config;
    const discountType = (config?.DiscountType || "").trim().toUpperCase();
    return this.isManualDiscountEnabled
      && this.isProductConfiguredDiscountable(ProductInfo)
      && (discountType === "MP" || discountType === "MF")
      && Number(config?.NumDiscountMax || 0) > 0;
  }

  isProductConfiguredDiscountable(ProductInfo: ProductInfoDto): boolean {
    return (ProductInfo?.Config?.IsDiscontable || "").trim().toUpperCase() === "S";
  }

  isSearchProductDiscountable(product: ProductSearchEntity): boolean {
    const discountType = (product?.DiscountType || "").trim().toUpperCase();
    return this.isManualDiscountEnabled
      && (product?.IsDiscontable || "").trim().toUpperCase() === "S"
      && (discountType === "MP" || discountType === "MF")
      && Number(product?.NumDiscountMax || 0) > 0;
  }

  isVariantInCart(ProductInfo: ProductInfoDto, ProductVariant: ProductVariantEntity): boolean {
    return !!this.shoppingCartService.GetProductInCart(
      ProductInfo.Product.ProductCod,
      ProductVariant.Variant
    );
  }

  getManualDiscountInput(ProductInfo: ProductInfoDto, ProductVariant: ProductVariantEntity): number {
    const detail = this.shoppingCartService.GetProductInCart(
      ProductInfo.Product.ProductCod,
      ProductVariant.Variant
    );
    if (!detail || detail.NumDiscount <= 0) return 0;

    const discountType = (ProductInfo.Config.DiscountType || "").trim().toUpperCase();
    if (discountType === "MP") {
      return detail.NumUnitPrice > 0
        ? this.toMoney(detail.NumDiscount * 100 / detail.NumUnitPrice)
        : 0;
    }
    return this.toMoney(detail.NumDiscount * ProductInfo.Config.ProductUnitFactor);
  }

  getManualDiscountMaximum(ProductInfo: ProductInfoDto): number {
    const config = ProductInfo.Config;
    return (config.DiscountType || "").trim().toUpperCase() === "MP"
      ? Number(config.NumDiscountMax || 0)
      : this.toMoney(
        Number(config.NumDiscountMax || 0)
        * ProductUnitHelper.normalizeFactor(Number(config.ProductUnitFactor || 1))
      );
  }

  isPercentageManualDiscount(ProductInfo: ProductInfoDto): boolean {
    return (ProductInfo.Config.DiscountType || "").trim().toUpperCase() === "MP";
  }

  getVisiblePriceAfterDiscount(ProductInfo: ProductInfoDto, ProductVariant: ProductVariantEntity): number {
    const detail = this.shoppingCartService.GetProductInCart(
      ProductInfo.Product.ProductCod,
      ProductVariant.Variant
    );
    const internalPrice = detail?.NumUnitPriceSale ?? ProductInfo.Config.NumPrice;
    return this.toMoney(
      ProductUnitHelper.toVisibleUnitPrice(internalPrice, ProductInfo.Config.ProductUnitFactor)
    );
  }

  getProductTotalAfterDiscount(ProductCod: string): number {
    return this.toMoney(
      this.ShoppingCart.DetailList
        .filter(item => item.ProductCod === ProductCod)
        .reduce((total, item) => total + Number(item.NumTotalPrice || 0), 0)
    );
  }

  DeleteProduct(ProductCod: string) {
    Swal.fire({
      title: '¿Estás seguro?',
      text: 'El producto se eliminara del carrito',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'No, cancelar'
    }).then(async (result) => {
      if (result.isConfirmed) {
        this.shoppingCartService.DeleteProduct(ProductCod);
        this.updateShoppingCart();
      }
    });
  }

  async save() {
    if (!this.validateProductExchangeTotal()) return;

    await this.createCode();

    this.ShoppingCart.Headboard.CurrencyCod = this.CurrencySystem.CurrencyCod;

    const response: ResponseWsDto = await this.presaleService.save(this.ShoppingCart);

    if (!response.ErrorStatus) {
      this.toastrService.success(
        `Se genero la venta con el codigo ${response.Data.Headboard.PresaleCod}`,
        'Operación realizada con exito');

      this.ShoppingCartResult = response.Data;
      this.Clean();
    }
  }

  newSale(): void {
    this.router.navigate(['/enterprise/sale/pages/createpresale']);
    setTimeout(() => { window.location.reload(); }, 100);

  }

  InitSale(): void {
    this.ShoppingCartResult = new PresaleRegisterDto();
  }

  Clean(): void {
    this.shoppingCartService.Clean();
    this.ShoppingCart = new PresaleRegisterDto();
  }

  async Confirm() {
    this.ShoppingCart.Headboard = this.ShoppingCartResult.Headboard;
    this.ShoppingCart.DetailList = this.ShoppingCartResult.DetailList;
    if (!this.validateProductExchangeTotal()) return;
    this.ShoppingCart.CreditNoteCod = this.isProductExchangeMode ? this.CreditNoteCod : "";

    const response: ResponseWsDto = await this.presaleService.confirm(this.ShoppingCart);

    if (!response.ErrorStatus) {
      this.SaleDetail = response.Data;
      const SaleCod: string = this.SaleDetail.Headboard.SaleCod;
      this.router.navigate(['/enterprise/sale/pages/createsale'], { queryParams: { SaleCod: SaleCod } });
    }
  }

  async findByDocumentNum() {
    this.DocumentType = this.cboDocumentType.nativeElement.value;
    this.DocumentNum = this.txtDocumentNum.nativeElement.value;

    const rpt: ResponseWsDto = await this.clientService.findByDocumentNum(this.DocumentType, this.DocumentNum);

    if (!rpt.ErrorStatus) {
      if (rpt.Data != null) {
        let Client: ClientEntity = rpt.Data;

        this.shoppingCartService.AddClient(Client);
        this.updateShoppingCart();

        this.ShowClientRegister = false;
        this.ShowClientSearch = false;
        this.ShowClient = true;

      }
      else {
        this.ShowClientRegister = true;
        this.ShowClientSearch = false;
        this.ShowClient = false;
      }
    }
  }

  ResponseResultFormClient(event: any) {
    this.ResultFormClient = event;

    this.shoppingCartService.AddClient(event);
    this.updateShoppingCart();

    this.ShowClientRegister = false;
    this.ShowClientSearch = false;
    this.ShowClient = true;

  }

  OpenClientModal() {
    this.ShowClient = false;
    this.ShowClientRegister = false;
    this.ShowClientSearch = true;
  }

  filterProductEnter(event: KeyboardEvent) {
    const now = Date.now();
    const timeDifference = now - this.lastKeypressTime;
    let IsBarcodeReaderInput: boolean = false;

    if (timeDifference < 50) {
      this.inputBuffer += event.key;
    } else {
      this.inputBuffer = event.key;
    }

    this.lastKeypressTime = now;

    if (event.key === 'Enter') {
      if (this.isBarcodeScannerInput()) {
        IsBarcodeReaderInput = true;
        console.log('Entrada detectada como lector de código de barras:', this.inputBuffer);
      } else {
        IsBarcodeReaderInput = false;
        console.log('Entrada detectada como manual:', this.inputBuffer);
      }

      this.filterProduct(1, IsBarcodeReaderInput);
      this.inputBuffer = '';
    }
  }

  isBarcodeScannerInput(): boolean {
    return this.inputBuffer.length > 5;
  }

  isProductInCart(ProductCod: string): boolean {
    return this.getTotalProduct(ProductCod) > 0;
  }

  getInfoClient(): string {
    if (this.ShoppingCart.Headboard.Client.Person.DocumentNum) {
      return this.ShoppingCart.Headboard.Client.Person.DocumentNum + ' - ' + this.ShoppingCart.Headboard.Client.Person.Names + ' ' + this.ShoppingCart.Headboard.Client.Person.LastNames;
    }
    return '';
  }

  @HostListener('document:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if (event.altKey && (event.key === 'b' || event.key === 'B')) {
      event.preventDefault();
      window.scrollTo(0, 0);
      this.txt_filtro_busqueda.nativeElement.focus();
      this.txt_filtro_busqueda.nativeElement.select();
    }
  }

}
