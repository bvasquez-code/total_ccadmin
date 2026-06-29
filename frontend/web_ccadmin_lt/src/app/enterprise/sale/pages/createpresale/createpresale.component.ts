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

@Component({
  selector: 'app-createpresale',
  templateUrl: './createpresale.component.html'
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
  ShoppingCartResult: PresaleDetailDto = new PresaleDetailDto();
  SaleDetail: SaleDetailDto = new SaleDetailDto();
  PresaleDetail: PresaleDetailDto = new PresaleDetailDto();
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
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.ShoppingCart.Headboard.PresaleCod = urlTree.queryParams['PresaleCod'];
    this.productSearch.StoreCod = this.session.getSessionStorageDto().StoreCod;
    this.productSearch.Page = 1;
    this.findAllProduct();
    this.findDataForm(this.ShoppingCart.Headboard.PresaleCod);
  }

  ngOnInit(): void {
    if (!this.ShoppingCart.Headboard.PresaleCod) {
      this.Clean();
    }
    this.shoppingCartService.Init();
    this.updateShoppingCart();
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
    const factor = ProductUnitFactor > 0 ? ProductUnitFactor : 1;
    return stock / factor;
  }

  getProductUnitName(): string {
    return this.productInfoDtoSelect.Config.ProductUnitName || 'NIU';
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
