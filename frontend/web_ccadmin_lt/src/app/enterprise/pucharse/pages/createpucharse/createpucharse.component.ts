import { Component, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { IRegisterForm } from 'src/app/enterprise/shared/interface/IRegisterForm';
import { PucharseRequestRegisterDto } from '../../model/dto/PucharseRequestRegisterDto';
import { PucharseRequestHeadService } from '../../service/PucharseRequestHeadService';
import { ToastrService } from 'ngx-toastr';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ProductEntity } from 'src/app/enterprise/product/model/entity/ProductEntity';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { PucharseRequestDetEntity } from '../../model/entity/PucharseRequestDetEntity';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ProductInfoDto } from 'src/app/enterprise/product/model/dto/ProductInfoDto';
import { PucharseRequestDetailsDto } from '../../model/dto/PucharseRequestDetailsDto';
import { ProductRegisterDto } from 'src/app/enterprise/product/model/dto/ProductRegisterDto';
import { SupplierService } from 'src/app/enterprise/supplier/service/supplier.service';
import { SupplierEntity } from 'src/app/enterprise/supplier/model/entity/SupplierEntity';

@Component({
  selector: 'app-createpucharse',
  templateUrl: './createpucharse.component.html'
})
export class CreatepucharseComponent implements IRegisterForm<PucharseRequestRegisterDto, string> {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNumUnit') txtNumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNumUnitPrice') txtNumUnitPrice!: ElementRef<HTMLInputElement>;

  @ViewChild('txtDealerCod') txtDealerCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtExternalCod') txtExternalCod!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCommenter') txtCommenter!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseModal') btnCloseModal!: ElementRef<HTMLButtonElement>;
  @ViewChild('btnCloseModalCreateProduct') btnCloseModalCreateProduct!: ElementRef<HTMLButtonElement>;
  @ViewChild('btnCloseModalCreateSupplier') btnCloseModalCreateSupplier!: ElementRef<HTMLButtonElement>;
  Page: number = 0;
  showCreateProduct: boolean = false;
  showCreateSupplier: boolean = false;
  PucharseReqCod: string = "";
  pucharseRequestRegister: PucharseRequestRegisterDto = new PucharseRequestRegisterDto();
  pucharseRequestDetails: PucharseRequestDetailsDto = new PucharseRequestDetailsDto();
  productList: ProductEntity[] = [];
  responsePageSearch: ResponsePageSearch<ProductEntity> = new ResponsePageSearch();
  productSelect: ProductEntity = new ProductEntity();
  currentSearchQuery: string = '';
  supplierInfo: string = '';
  supplierNotFound: boolean = false;

  constructor(
    private pucharseRequestHeadService: PucharseRequestHeadService,
    private router: Router,
    private toastrService: ToastrService,
    private productService: ProductService,
    private supplierService: SupplierService,
    private session: DataSesionService
  ) {
    this.GetParamUrl(this.router);
  }

  GetParamUrl(router: Router): void {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.PucharseReqCod = (urlTree.queryParams['PucharseReqCod']) ? urlTree.queryParams['PucharseReqCod'] : "";
    this.FindDataForm(this.PucharseReqCod);
  }

  async FindDataForm(PucharseReqCod: string): Promise<void> {

    this.Page = 1;
    setTimeout(() => { this.FindAllProduct(this.Page); }, 100);


    if (!PucharseReqCod) return;

    const rpt = await this.pucharseRequestHeadService.FindDataForm(PucharseReqCod);

    if (!rpt.ErrorStatus) {
      this.pucharseRequestDetails = rpt.DataAdditional.find(e => e.Name === "PucharseRequestDetails")?.Data;

      this.pucharseRequestRegister.Headboard = this.pucharseRequestDetails.Headboard;
      this.pucharseRequestRegister.DetailList = this.pucharseRequestDetails.DetailList;

      setTimeout(() => { this.LoadingForm(this.pucharseRequestRegister); }, 100);

    }

  }

  LoadingForm(PucharseRequestRegister: PucharseRequestRegisterDto): void {

    this.txtDealerCod.nativeElement.value = PucharseRequestRegister.Headboard.DealerCod;
    this.txtExternalCod.nativeElement.value = PucharseRequestRegister.Headboard.ExternalCod;
    this.txtCommenter.nativeElement.value = PucharseRequestRegister.Headboard.Commenter;
    this.FindSupplierByRuc();

  }

  private sameDetailLine(a: PucharseRequestDetEntity, b: PucharseRequestDetEntity): boolean {
    if ((a?.ItemNumber ?? 0) > 0 && (b?.ItemNumber ?? 0) > 0) {
      return a.ItemNumber === b.ItemNumber;
    }
    return a.ProductCod === b.ProductCod
      && a.Variant === b.Variant
      && (a.LotNumber ?? '') === (b.LotNumber ?? '')
      && (a.ExpirationDate ?? '') === (b.ExpirationDate ?? '');
  }

  async Save(): Promise<void> {
    this.pucharseRequestRegister.Headboard.DealerCod = this.txtDealerCod.nativeElement.value;
    this.pucharseRequestRegister.Headboard.ExternalCod = this.txtExternalCod.nativeElement.value;
    this.pucharseRequestRegister.Headboard.Commenter = this.txtCommenter.nativeElement.value;

    const rpt = await this.pucharseRequestHeadService.Save(this.pucharseRequestRegister);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Operación realizada con exito");
      setTimeout(() => {
        this.router.navigate(['/enterprise/pucharse/pages/listpucharse']);
      }, 1000);
    }
  }

  async FindSupplierByRuc(): Promise<void> {
    const ruc = String(this.txtDealerCod.nativeElement.value || '').trim();

    this.supplierInfo = '';
    this.supplierNotFound = false;

    if (!ruc) return;

    if (ruc.length !== 11) {
      this.toastrService.error('El RUC debe tener 11 caracteres');
      return;
    }

    const rpt: ResponseWsDto = await this.supplierService.findByDocumentNum('06', ruc);

    if (!rpt.ErrorStatus && rpt.Data && rpt.Data.SupplierCod) {
      const supplier: SupplierEntity = rpt.Data;
      this.loadingSupplierInfo(supplier);
    } else {
      this.supplierNotFound = true;
    }
  }

  loadingSupplierInfo(supplier: SupplierEntity): void {
    const person = supplier.Person;
    this.supplierInfo = person.BusinessName || person.CommercialName || `${person.Names} ${person.LastNames}`;
    this.supplierNotFound = false;
  }

  async FindAllProduct(Page: number) {
    if (Page === 1) {
      this.currentSearchQuery = this.txtSearch.nativeElement.value;
      this.txtSearch.nativeElement.value = '';
    }

    const rpt: ResponseWsDto = await this.productService.FindAll(this.currentSearchQuery, Page);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;

      if (this.responsePageSearch.resultSearch.length > 0) {
        this.productList = this.responsePageSearch.resultSearch;
      }


    }
  }

  FindAllProductNext(PagePlus: number) {
    if (this.Page + PagePlus < 1) return;
    this.Page = this.Page + PagePlus;
    console.log('Navigating to page:', this.Page);
    this.FindAllProduct(this.Page);
  }

  selectProduct(product: ProductEntity) {
    this.txtNumUnit.nativeElement.value = '';
    this.txtNumUnitPrice.nativeElement.value = '';
    this.productSelect = product;

    const existing = this.pucharseRequestRegister.DetailList.find(e => e.ProductCod === product.ProductCod);

    if (existing) {
      this.txtNumUnit.nativeElement.value = String(existing.NumUnit);
      this.txtNumUnitPrice.nativeElement.value = String(existing.NumUnitPrice);
    }
  }

  editDetail(detail: PucharseRequestDetEntity) {
    this.productSelect = detail.Product;
    this.txtNumUnit.nativeElement.value = String(detail.NumUnit);
    this.txtNumUnitPrice.nativeElement.value = String(detail.NumUnitPrice);
  }

  async removeProduct(detail: PucharseRequestDetEntity) {
    this.pucharseRequestRegister.DetailList = this.pucharseRequestRegister.DetailList.filter(e => !this.sameDetailLine(e, detail));
    this.calculateTotal();
  }

  async AddProduct() {
    const product = this.productSelect;
    if (!product || !product.ProductCod) {
      this.toastrService.error('Seleccione un producto');
      return;
    }

    const numUnit = Number(this.txtNumUnit.nativeElement.value);
    const numUnitPrice = Number(this.txtNumUnitPrice.nativeElement.value);

    if (numUnit <= 0) {
      this.toastrService.error('La cantidad debe ser mayor a cero');
      return;
    }

    // Allow 0 price? Assuming yes for now, but usually it's cost. Let's warn if negative.
    if (numUnitPrice < 0) {
      this.toastrService.error('El precio no puede ser negativo');
      return;
    }

    let purchaseDet: PucharseRequestDetEntity = new PucharseRequestDetEntity();
    const existing = this.pucharseRequestRegister.DetailList.find(e => e.ProductCod === product.ProductCod);

    if (existing) {
      purchaseDet = existing;
    }

    const productInfoDto: ProductInfoDto = await this.findDetailById(product.ProductCod);

    purchaseDet.ProductCod = product.ProductCod;
    purchaseDet.Variant = productInfoDto.VariantList[0]?.Variant || '0000';
    purchaseDet.NumUnit = numUnit;
    purchaseDet.NumUnitPrice = numUnitPrice;
    purchaseDet.NumTotalPrice = numUnit * numUnitPrice;
    purchaseDet.Product = product;

    if (!existing) {
      this.pucharseRequestRegister.DetailList.push(purchaseDet);
    }

    this.calculateTotal();
    this.closeModal();

    // Clear inputs
    this.txtNumUnit.nativeElement.value = '';
    this.txtNumUnitPrice.nativeElement.value = '';
  }

  calculateTotal() {
    this.pucharseRequestRegister.Headboard.NumTotalPrice = this.pucharseRequestRegister.DetailList
      .map(e => e.NumTotalPrice)
      .reduce((a, b) => a + b, 0);
  }

  closeModal() {
    this.btnCloseModal.nativeElement.click();
  }

  async findDetailById(ProductCod: string): Promise<ProductInfoDto> {
    let productInfoDto: ProductInfoDto = new ProductInfoDto();

    const rpt: ResponseWsDto = await this.productService.findDetailById(
      ProductCod,
      this.session.getSessionStorageDto().StoreCod
    );

    if (!rpt.ErrorStatus) {
      productInfoDto = rpt.Data;
    }

    return productInfoDto;
  }

  isProductSelected(product: ProductEntity): boolean {
    return this.pucharseRequestRegister.DetailList.some(e => e.ProductCod === product.ProductCod);
  }

  handleProductCreated(event: ProductRegisterDto) {
    this.btnCloseModalCreateProduct.nativeElement.click();
    this.txtSearch.nativeElement.value = event.product.ProductCod;
    this.FindAllProduct(1);
    this.showCreateProduct = false;
  }

  handleCancelCreateProduct() {
    this.btnCloseModalCreateProduct.nativeElement.click();
    this.showCreateProduct = false;
  }

  openCreateProductModal() {
    this.showCreateProduct = false;
    setTimeout(() => this.showCreateProduct = true, 50);
  }

  handleSupplierCreated(event: any) {
    const supplier: SupplierEntity = event;
    this.btnCloseModalCreateSupplier.nativeElement.click();
    this.showCreateSupplier = false;

    if (supplier && supplier.Person) {
      this.txtDealerCod.nativeElement.value = supplier.Person.DocumentNum;
      this.loadingSupplierInfo(supplier);
    }
  }

  openCreateSupplierModal() {
    this.showCreateSupplier = false;
    setTimeout(() => this.showCreateSupplier = true, 50);
  }

}
