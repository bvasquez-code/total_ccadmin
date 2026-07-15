import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProductEntity } from 'src/app/enterprise/product/model/entity/ProductEntity';
import { ProductInfoDto } from 'src/app/enterprise/product/model/dto/ProductInfoDto';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import { ProductSearchService } from 'src/app/enterprise/product/service/productsearch.service';
import { ProductSearchDto } from 'src/app/enterprise/product/model/dto/ProductSearchDto';
import { ProductSearchEntity } from 'src/app/enterprise/product/model/entity/ProductSearchEntity';
import { ProductConversionRequestDto } from 'src/app/enterprise/product/model/dto/ProductConversionRequestDto';
import { ProductConversionResultDto } from 'src/app/enterprise/product/model/dto/ProductConversionResultDto';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { IRegisterForm } from 'src/app/enterprise/shared/interface/IRegisterForm';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { TransferService } from '../../service/TransferService';
import { TransferConstants } from '../../model/constants/TransferConstants';
import { TransferRequestService } from '../../service/TransferRequestService';
import { TransferRequestRegisterBundleDto } from '../../model/dto/TransferRequestRegisterBundleDto';
import { TransferRequestDetEntity } from '../../model/entity/TransferRequestDetEntity';
import { TransferRequestHeadEntity } from '../../model/entity/TransferRequestHeadEntity';
import { TransferRequestDetSaveDto } from '../../model/dto/TransferRequestDetSaveDto';
import { TransferRequestDetService } from '../../service/TransferRequestDetService';
import { TransferReceiveDto } from '../../model/dto/TransferReceiveDto';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';

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
  TransferReqCod: string = '';
  transferRequestRegister: TransferRequestRegisterBundleDto = new TransferRequestRegisterBundleDto();
  responsePageSearch: ResponsePageSearch<ProductSearchEntity> = new ResponsePageSearch();
  productList: ProductSearchEntity[] = [];
  productSelect: ProductSearchEntity = new ProductSearchEntity();
  productSearch: ProductSearchDto = new ProductSearchDto();
  storeList: StoreEntity[] = [];
  conversionValidationMessage: string = '';
  isSavingDetail: boolean = false;

  constructor(
    private transferService: TransferService,
    private transferRequestService: TransferRequestService,
    private productService: ProductService,
    private productSearchService: ProductSearchService,
    private session: DataSesionService,
    private router: Router,
    private toastrService: ToastrService,
    private transferRequestDetService: TransferRequestDetService,
    private alertService: AlertService
  ) {

  }

  ngOnInit(): void {
    this.GetParamUrl(this.router);
  }

  GetParamUrl(router: Router): void {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.TransferReqCod = urlTree.queryParams['TransferReqCod'] ?? urlTree.queryParams['TransferCod'] ?? '';
    this.FindDataForm(this.TransferReqCod);
  }

  async FindDataForm(TransferReqCod: string): Promise<void> {
    const rpt: ResponseWsDto = await this.transferRequestService.FindDataForm(TransferReqCod);

    if (!rpt.ErrorStatus) {
      const storeList: StoreEntity[] = rpt.DataAdditional?.find(e => e.Name === 'storeList')?.Data ?? [];
      this.storeList = storeList.filter(e => e.StoreCod !== this.session.getSessionStorageDto().StoreCod);

      const transferDetail = rpt.DataAdditional?.find(e => e.Name === 'transferDetail')?.Data;
      if (transferDetail?.transferHeadRequest) {
        this.loadRequestData(transferDetail.transferHeadRequest, transferDetail.transferDetRequestList ?? []);
        setTimeout(() => this.LoadingForm(this.transferRequestRegister), 100);
      }
    }

    this.productList = [];
  }

  LoadingForm(Entity: TransferRequestRegisterBundleDto): void {
    if (this.cboStoreOrigin) {
      this.cboStoreOrigin.nativeElement.value = Entity.transferHead.StoreCodOrigin ?? '';
    }
    if (this.txtObservation) {
      this.txtObservation.nativeElement.value = Entity.transferHead.Observation ?? '';
    }
    if (this.chkAllowPartial) {
      this.chkAllowPartial.nativeElement.checked = !!Entity.allowPartial;
    }
    if (Entity.transferHead.StoreCodOrigin) {
      void this.FindAllProduct(1);
    }
  }

  private loadRequestData(head: TransferRequestHeadEntity, detailList: TransferRequestDetEntity[]): void {
    this.transferRequestRegister.transferHead = Object.assign(new TransferRequestHeadEntity(), head);
    this.transferRequestRegister.transferDetList = (detailList ?? []).map(detail =>
      Object.assign(new TransferRequestDetEntity(), detail)
    );
  }

  private updateRequestFromInputs(): void {
    const storeOrigin = this.cboStoreOrigin.nativeElement.value;
    ValidationHelper.validateIsNotEmpty(storeOrigin, 'Seleccione el local a solicitar stock');

    this.transferRequestRegister.transferHead.StoreCodOrigin = storeOrigin;
    this.transferRequestRegister.transferHead.StoreCodDest = this.session.getSessionStorageDto().StoreCod;
    this.transferRequestRegister.transferHead.StoreCodRequestedBy = this.session.getSessionStorageDto().StoreCod;
    this.transferRequestRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
    this.transferRequestRegister.transferHead.TransferStatus = TransferConstants.STATUS_PENDING;
    this.transferRequestRegister.transferHead.Observation = this.txtObservation.nativeElement.value;
    this.transferRequestRegister.allowPartial = this.chkAllowPartial.nativeElement.checked;
  }

  private buildDetailSaveDto(detail: TransferRequestDetEntity): TransferRequestDetSaveDto {
    const dto = new TransferRequestDetSaveDto();
    dto.transferHead = this.transferRequestRegister.transferHead;
    dto.transferDet = detail;
    return dto;
  }

  private cloneDetailList(): TransferRequestDetEntity[] {
    return this.transferRequestRegister.transferDetList.map(detail =>
      Object.assign(new TransferRequestDetEntity(), detail, { Product: detail.Product })
    );
  }

  private syncSavedDetail(savedDetail: TransferRequestDetEntity, product: ProductEntity): void {
    savedDetail.Product = product || savedDetail.Product;
    const index = this.transferRequestRegister.transferDetList.findIndex(detail =>
      this.sameDetailLine(detail, savedDetail)
    );

    if (index >= 0) {
      this.transferRequestRegister.transferDetList[index] = savedDetail;
    } else {
      this.transferRequestRegister.transferDetList.push(savedDetail);
    }
  }

  private async saveInitialRequest(): Promise<boolean> {
    if (this.isSavingDetail) return false;
    this.isSavingDetail = true;

    try {
      this.updateRequestFromInputs();
      const storeOrigin = this.getStoredCodOrigin();
      this.transferRequestRegister.transferHead.TransferReqCod = await this.createCode(storeOrigin);
      this.transferRequestRegister.transferDetList.forEach((detail, index) => {
        detail.TransferReqCod = this.transferRequestRegister.transferHead.TransferReqCod;
        detail.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
        detail.ItemNumber = detail.ItemNumber > 0 ? detail.ItemNumber : index + 1;
      });

      const productByItem = new Map(
        this.transferRequestRegister.transferDetList.map(detail => [detail.ItemNumber, detail.Product])
      );
      const rpt: ResponseWsDto = await this.transferRequestService.Save(this.transferRequestRegister);

      if (rpt.ErrorStatus) {
        this.transferRequestRegister.transferHead.TransferReqCod = '';
        this.toastrService.error(rpt.Message || 'No se pudo guardar la solicitud de transferencia');
        return false;
      }

      const saved: TransferRequestRegisterBundleDto = rpt.Data;
      this.loadRequestData(saved.transferHead, saved.transferDetList);
      this.transferRequestRegister.transferDetList.forEach(detail => {
        detail.Product = productByItem.get(detail.ItemNumber) || detail.Product;
      });
      this.TransferReqCod = this.transferRequestRegister.transferHead.TransferReqCod;

      await this.router.navigate(
        ['/enterprise/transfer/pages/createtransferrequest'],
        { queryParams: { TransferReqCod: this.TransferReqCod }, replaceUrl: true }
      );
      this.toastrService.success('Solicitud de transferencia guardada como pendiente');
      return true;
    } catch (e: any) {
      this.transferRequestRegister.transferHead.TransferReqCod = '';
      this.toastrService.error(e.message);
      return false;
    } finally {
      this.isSavingDetail = false;
    }
  }

  private async saveDetail(detail: TransferRequestDetEntity): Promise<boolean> {
    if (this.isSavingDetail) return false;

    try {
      detail.TransferReqCod = this.transferRequestRegister.transferHead.TransferReqCod;
      this.isSavingDetail = true;
      const rpt: ResponseWsDto = await this.transferRequestDetService.Save(this.buildDetailSaveDto(detail));

      if (rpt.ErrorStatus) {
        this.toastrService.error(rpt.Message || 'No se pudo guardar el producto');
        return false;
      }

      const data: TransferRequestDetSaveDto = rpt.Data;
      this.transferRequestRegister.transferHead = Object.assign(new TransferRequestHeadEntity(), data.transferHead);
      this.syncSavedDetail(Object.assign(new TransferRequestDetEntity(), data.transferDet), detail.Product);
      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    } finally {
      this.isSavingDetail = false;
    }
  }

  private async deleteDetail(detail: TransferRequestDetEntity): Promise<boolean> {
    if (this.isSavingDetail) return false;

    try {
      this.isSavingDetail = true;
      const rpt: ResponseWsDto = await this.transferRequestDetService.Delete(this.buildDetailSaveDto(detail));

      if (rpt.ErrorStatus) {
        this.toastrService.error(rpt.Message || 'No se pudo eliminar el producto');
        return false;
      }

      this.transferRequestRegister.transferDetList = this.transferRequestRegister.transferDetList
        .filter(item => !this.sameDetailLine(item, detail));
      return true;
    } catch (e: any) {
      this.toastrService.error(e.message);
      return false;
    } finally {
      this.isSavingDetail = false;
    }
  }

  async Save(): Promise<void> {
    try {
      if (this.isSavingDetail) return;
      this.updateRequestFromInputs();

      if (this.transferRequestRegister.transferDetList.length === 0) {
        throw new Error('Debe agregar al menos un producto');
      }

      const invalidQty = this.transferRequestRegister.transferDetList.find(det => det.NumUnit <= 0);
      if (invalidQty) {
        throw new Error('La cantidad debe ser mayor a cero');
      }

      const confirmation = await this.alertService.waring(
        'Estás a punto de enviar tu solicitud. Ya no podrás editarla.',
        'Enviar solicitud'
      );
      if (!confirmation.isConfirmed) return;

      if (!this.transferRequestRegister.transferHead.TransferReqCod) {
        const saved = await this.saveInitialRequest();
        if (!saved) return;
      }

      const productByItem = new Map(
        this.transferRequestRegister.transferDetList.map(detail => [detail.ItemNumber, detail.Product])
      );
      this.isSavingDetail = true;
      const rpt: ResponseWsDto = await this.transferRequestService.Save(this.transferRequestRegister);

      if (!rpt.ErrorStatus) {
        const saved: TransferRequestRegisterBundleDto = rpt.Data;
        this.loadRequestData(saved.transferHead, saved.transferDetList);
        this.transferRequestRegister.transferDetList.forEach(detail => {
          detail.Product = productByItem.get(detail.ItemNumber) || detail.Product;
        });

        const transferRegister = this.transferRequestRegister.buildTransferRegister();
        transferRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
        transferRegister.transferHead.TransferStatus = TransferConstants.STATUS_PENDING;
        const rptTs: ResponseWsDto = await this.transferService.Save(transferRegister);

        if (!rptTs.ErrorStatus) {
          const requestApproved = new TransferReceiveDto();
          requestApproved.transferCod = this.transferRequestRegister.transferHead.TransferReqCod;
          requestApproved.user = this.session.getSessionStorageDto().UserCod;
          requestApproved.observation = this.transferRequestRegister.transferHead.Observation;
          requestApproved.typeOperation = TransferConstants.TYPE_OPERATION_REQUEST;

          const rptApproved: ResponseWsDto = await this.transferRequestService.InReviewTransfer(requestApproved);
          if (rptApproved.ErrorStatus) {
            this.toastrService.error(rptApproved.Message || 'La transferencia se registró, pero no se pudo aprobar la solicitud');
            return;
          }

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
    } finally {
      this.isSavingDetail = false;
    }
  }

  async FindAllProduct(Page: number) {
    const storeOrigin = this.cboStoreOrigin?.nativeElement.value ?? '';
    if (!storeOrigin) {
      this.toastrService.error('Seleccione un local a solicitar stock para buscar productos');
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
    this.clearConversionValidationMessage();
    this.txtNumUnit.nativeElement.value = '';
    this.productSelect = product;

    const existing = this.transferRequestRegister.transferDetList.find(e => e.ProductCod === product.ProductCod);
    if (existing) {
      this.txtNumUnit.nativeElement.value = String(this.toVisibleQuantity(existing.NumUnit, existing.ProductUnitFactor));
    }
  }

  async AddProduct() {
    if (this.isSavingDetail) return;
    this.clearConversionValidationMessage();
    const product = this.productSelect;
    if (!product || !product.ProductCod) {
      this.toastrService.error('Seleccione un producto');
      return;
    }

    const isNewRequest = !this.transferRequestRegister.transferHead.TransferReqCod;
    const previousDetailList = this.cloneDetailList();
    let transferDet: TransferRequestDetEntity = new TransferRequestDetEntity();
    let transferDetExist: TransferRequestDetEntity | undefined = this.transferRequestRegister.transferDetList.find(e => e.ProductCod === product.ProductCod);

    if (transferDetExist) {
      transferDet = transferDetExist;
    }
    const previousNumUnit = transferDet.NumUnit;
    const previousProductUnitName = transferDet.ProductUnitName;
    const previousProductUnitFactor = transferDet.ProductUnitFactor;

    const numUnit = Number(this.txtNumUnit.nativeElement.value);
    if (!numUnit || numUnit <= 0) {
      this.toastrService.error('Ingrese una cantidad válida');
      return;
    }

    let productInfoDto: ProductInfoDto = await this.findDetailById(product.ProductCod, this.getStoredCodOrigin());
    const productEntity: ProductEntity = new ProductEntity();
    productEntity.ProductCod = product.ProductCod;
    productEntity.ProductName = product.ProductName;

    transferDet.ProductCod = product.ProductCod;
    transferDet.Variant = productInfoDto.VariantList[0]?.Variant ?? '0000';
    const ProductUnitFactor = productInfoDto.Config.ProductUnitFactor > 0 ? productInfoDto.Config.ProductUnitFactor : 1;
    transferDet.NumUnit = numUnit * ProductUnitFactor;
    transferDet.ProductUnitName = productInfoDto.Config.ProductUnitName || 'NIU';
    transferDet.ProductUnitFactor = ProductUnitFactor;
    transferDet.Product = productEntity;

    if (!await this.validateConvertProductBetweenStores(transferDet)) {
      transferDet.NumUnit = previousNumUnit;
      transferDet.ProductUnitName = previousProductUnitName;
      transferDet.ProductUnitFactor = previousProductUnitFactor;
      return;
    }

    if (!transferDetExist) {
      this.transferRequestRegister.transferDetList.push(transferDet);
    }

    const saved = isNewRequest
      ? await this.saveInitialRequest()
      : await this.saveDetail(transferDet);

    if (!saved) {
      this.transferRequestRegister.transferDetList = previousDetailList;
      return;
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
    if (!this.transferRequestRegister.transferHead.TransferReqCod) {
      this.transferRequestRegister.transferDetList = this.transferRequestRegister.transferDetList
        .filter(e => !this.sameDetailLine(e, product));
      return;
    }

    await this.deleteDetail(product);
  }

  async findDetailById(ProductCod: string, StoreCod: string = this.session.getSessionStorageDto().StoreCod): Promise<ProductInfoDto> {
    let productInfoDto: ProductInfoDto = new ProductInfoDto();

    const rpt: ResponseWsDto = await this.productService.findDetailById(
      ProductCod,
      StoreCod
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

    this.txtNumUnit.nativeElement.value = String(this.toVisibleQuantity(detail.NumUnit, detail.ProductUnitFactor));
  }

  toVisibleQuantity(internalQuantity: number, ProductUnitFactor: number): number {
    const factor = ProductUnitFactor > 0 ? ProductUnitFactor : 1;
    return internalQuantity / factor;
  }

  isProductSelected(product: ProductSearchEntity): boolean {
    return this.transferRequestRegister.transferDetList.some(e => e.ProductCod === product.ProductCod);
  }

  @ViewChild('btnCloseModal') btnCloseModal!: ElementRef<HTMLButtonElement>;

  closeModal() {
    this.clearConversionValidationMessage();
    this.btnCloseModal.nativeElement.click();
  }

  async validateConvertProductBetweenStores(transferDet: TransferRequestDetEntity): Promise<boolean> {
    const request: ProductConversionRequestDto = new ProductConversionRequestDto();

    request.ProductCod = transferDet.ProductCod;
    request.quantityToConvert = ProductUnitHelper.toVisibleQuantity(transferDet.NumUnit, transferDet.ProductUnitFactor);
    request.StoredCodDestination = this.getStoredCodDestination();
    request.StoredCodOrigin = this.getStoredCodOrigin();

    const rpt: ResponseWsDto = await this.transferRequestService.validateConvertProductBetweenStores(request);

    if (!rpt.ErrorStatus) {
      const productConversionResult: ProductConversionResultDto = rpt.Data;

      if (!productConversionResult.valid) {
        this.conversionValidationMessage = this.buildProductConversionErrorMessage(transferDet, productConversionResult);
        this.toastrService.error(this.conversionValidationMessage);
      } else {
        this.clearConversionValidationMessage();
      }
      return productConversionResult.valid;
    } else {
      this.conversionValidationMessage = 'No se pudo validar si el producto puede transferirse entre locales. Intentelo nuevamente.';
      this.toastrService.error(this.conversionValidationMessage);
    }
    return false;
  }

  clearConversionValidationMessage(): void {
    this.conversionValidationMessage = '';
  }

  private buildProductConversionErrorMessage(transferDet: TransferRequestDetEntity, result: ProductConversionResultDto): string {
    const productName = transferDet.Product?.ProductName || transferDet.ProductCod;
    const visibleQuantity = ProductUnitHelper.toVisibleQuantity(transferDet.NumUnit, transferDet.ProductUnitFactor);
    const originStore = this.getStoredCodOrigin();
    const destinationStore = this.getStoredCodDestination();
    const detail = result?.message ? ` Detalle: ${result.message}` : '';

    return `No se puede solicitar ${visibleQuantity} ${transferDet.ProductUnitName || 'NIU'} del producto "${productName}" `
      + `desde el local ${originStore} hacia el local ${destinationStore}. `
      + `La cantidad no coincide con la unidad de venta configurada en el local destino.${detail}`;
  }

  getStoredCodOrigin(): string {
    return this.cboStoreOrigin.nativeElement.value;
  }

  getStoredCodDestination(): string {
    return this.session.getSessionStorageDto().StoreCod;
  }
}
