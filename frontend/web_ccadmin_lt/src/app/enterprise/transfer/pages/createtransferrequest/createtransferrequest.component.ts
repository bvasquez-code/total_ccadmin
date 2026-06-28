import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProductEntity } from 'src/app/enterprise/product/model/entity/ProductEntity';
import { ProductInfoDto } from 'src/app/enterprise/product/model/dto/ProductInfoDto';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import { ProductSearchService } from 'src/app/enterprise/product/service/productsearch.service';
import { ProductSearchDto } from 'src/app/enterprise/product/model/dto/ProductSearchDto';
import { ProductSearchEntity } from 'src/app/enterprise/product/model/entity/ProductSearchEntity';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { IRegisterForm } from 'src/app/enterprise/shared/interface/IRegisterForm';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { TransferRegisterBundleDto } from '../../model/dto/TransferRegisterBundleDto';
import { TransferDetEntity } from '../../model/entity/TransferDetEntity';
import { TransferService } from '../../service/TransferService';
import { TransferConstants } from '../../model/constants/TransferConstants';
import { TransferRequestService } from '../../service/TransferRequestService';
import { TransferRequestRegisterBundleDto } from '../../model/dto/TransferRequestRegisterBundleDto';
import { TransferRequestDetEntity } from '../../model/entity/TransferRequestDetEntity';

@Component({
  selector: 'app-createtransferrequest',
  templateUrl: './createtransferrequest.component.html'
})
export class CreatetransferrequestComponent implements OnInit, IRegisterForm<TransferRequestRegisterBundleDto, string> {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNumUnit') txtNumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtObservation') txtObservation!: ElementRef<HTMLTextAreaElement>;
  @ViewChild('cboStoreOrigin') cboStoreOrigin!: ElementRef<HTMLSelectElement>;
  @ViewChild('chkAllowPartial') chkAllowPartial!: ElementRef<HTMLInputElement>;

  Page: number = 1;
  TransferCod: string = '';
  transferRegister: TransferRegisterBundleDto = new TransferRegisterBundleDto();
  transferRequestRegister: TransferRequestRegisterBundleDto = new TransferRequestRegisterBundleDto();
  responsePageSearch: ResponsePageSearch<ProductSearchEntity> = new ResponsePageSearch();
  productList: ProductSearchEntity[] = [];
  productSelect: ProductSearchEntity = new ProductSearchEntity();
  productSearch: ProductSearchDto = new ProductSearchDto();
  storeList: StoreEntity[] = [];

  constructor(
    private transferService: TransferService,
    private transferRequestService: TransferRequestService,
    private productService: ProductService,
    private productSearchService: ProductSearchService,
    private session: DataSesionService,
    private router: Router,
    private toastrService: ToastrService
  ) {

  }

  ngOnInit(): void {
    this.GetParamUrl(this.router);
  }

  GetParamUrl(router: Router): void {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.TransferCod = (urlTree.queryParams['TransferCod']) ? urlTree.queryParams['TransferCod'] : '';
    this.FindDataForm(this.TransferCod);
  }

  async FindDataForm(TransferCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.transferService.FindDataForm(TransferCod);

    if (!rpt.ErrorStatus) {
      const storeList: StoreEntity[] = rpt.DataAdditional?.find(e => e.Name === 'storeList')?.Data ?? [];
      this.storeList = storeList.filter(e => e.StoreCod !== this.session.getSessionStorageDto().StoreCod);

      const registerBundle = rpt.DataAdditional?.find(e => e.Name === 'TransferRegisterBundle')?.Data
        ?? rpt.DataAdditional?.find(e => e.Name === 'TransferRegister')?.Data
        ?? rpt.Data;

      if (registerBundle) {
        this.transferRequestRegister = registerBundle;
        setTimeout(() => this.LoadingForm(this.transferRequestRegister), 100);
      }
    }

    this.productList = [];
  }

  LoadingForm(Entity: TransferRequestRegisterBundleDto): void {
    if (this.cboStoreOrigin) {
      this.cboStoreOrigin.nativeElement.value = Entity.transferHead.StoreCodDest ?? '';
    }
    if (this.txtObservation) {
      this.txtObservation.nativeElement.value = Entity.transferHead.Observation ?? '';
    }
    if (this.chkAllowPartial) {
      this.chkAllowPartial.nativeElement.checked = !!Entity.allowPartial;
    }
  }

  async Save(): Promise<void> {
    try {
      const storeOrigin = this.cboStoreOrigin.nativeElement.value;
      ValidationHelper.validateIsNotEmpty(storeOrigin, 'Seleccione el local a solicitar stock');

      if (this.transferRequestRegister.transferDetList.length === 0) {
        throw new Error('Debe agregar al menos un producto');
      }

      const invalidQty = this.transferRequestRegister.transferDetList.find(det => det.NumUnit <= 0);
      if (invalidQty) {
        throw new Error('La cantidad debe ser mayor a cero');
      }

      this.transferRequestRegister.transferHead.TransferReqCod = await this.createCode(storeOrigin);
      this.transferRequestRegister.transferHead.StoreCodOrigin = storeOrigin;
      this.transferRequestRegister.transferHead.StoreCodDest = this.session.getSessionStorageDto().StoreCod;
      this.transferRequestRegister.transferHead.StoreCodRequestedBy = this.session.getSessionStorageDto().StoreCod;
      this.transferRequestRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
      this.transferRequestRegister.transferHead.Observation = this.txtObservation.nativeElement.value;
      this.transferRequestRegister.allowPartial = this.chkAllowPartial.nativeElement.checked;

      this.transferRequestRegister.transferDetList = this.transferRequestRegister.transferDetList.map((det, index) => {
        det.ItemNumber = index + 1;
        det.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
        det.TransferReqCod = this.transferRequestRegister.transferHead.TransferReqCod;
        return det;
      });

      const rpt: ResponseWsDto = await this.transferRequestService.Save(this.transferRequestRegister);

      if (!rpt.ErrorStatus) {

        this.transferRequestRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
        this.transferRequestRegister.transferHead.TransferStatus = TransferConstants.STATUS_PENDING;
        const rptTs: ResponseWsDto = await this.transferService.Save(this.transferRequestRegister.buildTransferRegister());

        if (!rptTs.ErrorStatus) {
          this.toastrService.success(rptTs.Message || 'Transferencia registrada correctamente');
          setTimeout(() => {
            this.router.navigate(['/enterprise/transfer/pages/listtransferrequest']);
          }, 1000);
        } else {
          this.toastrService.error(rptTs.Message || 'Ocurrió un error al registrar la transferencia');
        }
      } else {
        this.toastrService.error(rpt.Message || 'Ocurrió un error al registrar la transferencia');
      }
    } catch (e: any) {
      this.toastrService.error(e.message);
    }
  }

  async FindAllProduct(Page: number) {
    const storeOrigin = this.cboStoreOrigin?.nativeElement.value ?? '';
    if (!storeOrigin) {
      this.toastrService.error('Seleccione un local destino para buscar productos');
      return;
    }

    this.Page = Page;
    this.productSearch.StoreCod = storeOrigin;
    this.productSearch.Page = Page;

    if (Page === 1) {
      this.productSearch.Query = this.txtSearch?.nativeElement.value ?? '';
      if (this.txtSearch) this.txtSearch.nativeElement.value = '';
    }

    this.productSearch.StockMin = 1;

    const response: ResponseWsDto = await this.productSearchService.query(this.productSearch);

    if (!response.ErrorStatus) {
      this.responsePageSearch = response.Data;
      this.productList = this.responsePageSearch.resultSearch;
    }
  }

  FindAllProductNext(PagePlus: number) {
    if (this.Page + PagePlus < 1) return;
    this.Page = this.Page + PagePlus;
    this.FindAllProduct(this.Page);
  }

  selectProduct(product: ProductSearchEntity) {
    this.txtNumUnit.nativeElement.value = '';
    this.productSelect = product;

    const existing = this.transferRequestRegister.transferDetList.find(e => e.ProductCod === product.ProductCod);
    if (existing) {
      this.txtNumUnit.nativeElement.value = String(existing.NumUnit);
    }
  }

  async AddProduct() {
    const product = this.productSelect;
    if (!product || !product.ProductCod) {
      this.toastrService.error('Seleccione un producto');
      return;
    }

    let transferDet: TransferRequestDetEntity = new TransferRequestDetEntity();
    let transferDetExist: TransferRequestDetEntity | undefined = this.transferRequestRegister.transferDetList.find(e => e.ProductCod === product.ProductCod);

    if (transferDetExist) {
      transferDet = transferDetExist;
    }

    const numUnit = Number(this.txtNumUnit.nativeElement.value);
    if (!numUnit || numUnit <= 0) {
      this.toastrService.error('Ingrese una cantidad válida');
      return;
    }

    let productInfoDto: ProductInfoDto = await this.findDetailById(product.ProductCod);
    const productEntity: ProductEntity = new ProductEntity();
    productEntity.ProductCod = product.ProductCod;
    productEntity.ProductName = product.ProductName;

    transferDet.ProductCod = product.ProductCod;
    transferDet.Variant = productInfoDto.VariantList[0]?.Variant ?? '0000';
    transferDet.NumUnit = numUnit;
    transferDet.Product = productEntity;

    if (!transferDetExist) {
      this.transferRequestRegister.transferDetList.push(transferDet);
    }

    this.txtNumUnit.nativeElement.value = '';
    this.closeModal();
  }

  private sameDetailLine(a: TransferRequestDetEntity, b: TransferRequestDetEntity): boolean {
    if ((a?.ItemNumber ?? 0) > 0 && (b?.ItemNumber ?? 0) > 0) {
      return a.ItemNumber === b.ItemNumber;
    }
    return a.ProductCod === b.ProductCod
      && a.Variant === b.Variant
      && (a.LotNumber ?? '') === (b.LotNumber ?? '')
      && (a.ExpirationDate ?? '') === (b.ExpirationDate ?? '');
  }

  async removeProduct(product: TransferRequestDetEntity) {
    this.transferRequestRegister.transferDetList = this.transferRequestRegister.transferDetList.filter(e => !this.sameDetailLine(e, product));
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

  async createCode(StoreCod: string) {
    const rpt: ResponseWsDto = await this.transferService.CreateCode(StoreCod);
    if (rpt?.ErrorStatus) {
      this.toastrService.error(rpt.Message);
      throw new Error(rpt.Message);
    }
    return String(rpt.Data);
  }

  editDetail(detail: TransferRequestDetEntity) {
    this.productSelect = new ProductSearchEntity();
    this.productSelect.ProductCod = detail.ProductCod;
    this.productSelect.ProductName = detail.Product.ProductName;

    this.txtNumUnit.nativeElement.value = String(detail.NumUnit);
  }

  isProductSelected(product: ProductSearchEntity): boolean {
    return this.transferRequestRegister.transferDetList.some(e => e.ProductCod === product.ProductCod);
  }

  @ViewChild('btnCloseModal') btnCloseModal!: ElementRef<HTMLButtonElement>;

  closeModal() {
    this.btnCloseModal.nativeElement.click();
  }
}
