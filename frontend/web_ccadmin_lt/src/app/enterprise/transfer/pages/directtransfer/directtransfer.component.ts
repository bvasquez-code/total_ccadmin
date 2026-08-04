import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ProductEntity } from 'src/app/enterprise/product/model/entity/ProductEntity';
import { ProductInfoDto } from 'src/app/enterprise/product/model/dto/ProductInfoDto';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import { ProductSearchService } from 'src/app/enterprise/product/service/productsearch.service';
import { ProductSearchDto } from 'src/app/enterprise/product/model/dto/ProductSearchDto';
import { ProductSearchEntity } from 'src/app/enterprise/product/model/entity/ProductSearchEntity';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { ValidationHelper } from 'src/app/enterprise/shared/helper/ValidationHelper';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { TransferRegisterBundleDto } from '../../model/dto/TransferRegisterBundleDto';
import { TransferService } from '../../service/TransferService';
import { TransferRequestService } from '../../service/TransferRequestService';
import { TransferRequestRegisterBundleDto } from '../../model/dto/TransferRequestRegisterBundleDto';
import { TransferRequestDetEntity } from '../../model/entity/TransferRequestDetEntity';
import { TransferConstants } from '../../model/constants/TransferConstants';
import { TransferDispatchDto } from '../../model/dto/TransferDispatchDto';
import { TransferReceiveDto } from '../../model/dto/TransferReceiveDto';
import { CarrierService } from '../../service/CarrierService';
import { CarrierEntity } from '../../model/entity/CarrierEntity';
import { TransferLotDispatchDto } from '../../model/dto/TransferLotDispatchDto';
import { ProductConversionRequestDto } from 'src/app/enterprise/product/model/dto/ProductConversionRequestDto';
import { ProductUnitHelper } from 'src/app/enterprise/shared/helper/ProductUnitHelper';
import { ProductConversionResultDto } from 'src/app/enterprise/product/model/dto/ProductConversionResultDto';
import { TransferRequestHeadEntity } from '../../model/entity/TransferRequestHeadEntity';
import { TransferRequestDetSaveDto } from '../../model/dto/TransferRequestDetSaveDto';
import { TransferRequestDetService } from '../../service/TransferRequestDetService';
import { TransferDetRegisterMassiveDto } from '../../model/dto/TransferDetRegisterMassiveDto';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
import { TransferStockAvailabilityService } from '../../service/TransferStockAvailabilityService';

@Component({
  selector: 'app-directtransfer',
  templateUrl: './directtransfer.component.html'
})
export class DirecttransferComponent implements OnInit {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  @ViewChild('txtNumUnit') txtNumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtObservation') txtObservation!: ElementRef<HTMLTextAreaElement>;
  @ViewChild('cboStoreDest') cboStoreDest!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboTransportMode') cboTransportMode!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboReason') cboReason!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtVehiclePlate') txtVehiclePlate!: ElementRef<HTMLInputElement>;
  @ViewChild('cboDriverDocType') cboDriverDocType!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtDriverDocNumber') txtDriverDocNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDriverLicenseNumber') txtDriverLicenseNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCarrierRuc') txtCarrierRuc!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCarrierName') txtCarrierName!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseModal') btnCloseModal!: ElementRef<HTMLButtonElement>;
  @ViewChild('txtLotNumUnit') txtLotNumUnit!: ElementRef<HTMLInputElement>;
  @ViewChild('txtLotNumber') txtLotNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtExpirationDate') txtExpirationDate!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseLotDispatchModal') btnCloseLotDispatchModal!: ElementRef<HTMLButtonElement>;

  Page: number = 1;
  TransferReqCod: string = '';
  transferRequestRegister: TransferRequestRegisterBundleDto = new TransferRequestRegisterBundleDto();
  responsePageSearch: ResponsePageSearch<ProductSearchEntity> = new ResponsePageSearch();
  productList: ProductSearchEntity[] = [];
  productSelect: ProductSearchEntity = new ProductSearchEntity();
  productSearch: ProductSearchDto = new ProductSearchDto();
  storeList: StoreEntity[] = [];
  selectedDetail: TransferRequestDetEntity = new TransferRequestDetEntity();
  isTransferWithLots: boolean = false;
  lotDispatchList: TransferLotDispatchDto[] = [];
  conversionValidationMessage: string = '';
  isSavingDetail: boolean = false;
  hasTransferDraft: boolean = false;
  selectedVisibleQuantity: number = 0;
  private readonly maxLotNumberLength: number = 32;

  transportModeList = [
    { Code: '01', Name: 'Transporte público' },
    { Code: '02', Name: 'Transporte privado' }
  ];

  reasonTransferList = [
    { Code: '01', Name: 'Venta' },
    { Code: '02', Name: 'Compra' },
    { Code: '03', Name: 'Consignación' },
    { Code: '04', Name: 'Traslado entre locales' }
  ];

  constructor(
    private transferService: TransferService,
    private transferRequestService: TransferRequestService,
    private productService: ProductService,
    private productSearchService: ProductSearchService,
    private session: DataSesionService,
    private router: Router,
    private toastrService: ToastrService,
    private carrierService: CarrierService,
    private transferRequestDetService: TransferRequestDetService,
    private alertService: AlertService,
    private transferStockAvailabilityService: TransferStockAvailabilityService
  ) { }

  ngOnInit(): void {
    const urlTree = this.router.parseUrl(this.router.url);
    this.TransferReqCod = urlTree.queryParams['TransferReqCod'] ?? urlTree.queryParams['TransferCod'] ?? '';
    void this.loadFormData();
  }

  async loadFormData() {
    const rpt: ResponseWsDto = await this.transferService.FindDataForm(this.TransferReqCod);
    if (!rpt.ErrorStatus) {
      const storeList = rpt.DataAdditional?.find((e: any) => e.Name === 'StoreList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'storeList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'stores')?.Data
        ?? [];
      this.storeList = storeList.filter((e: StoreEntity) => e.StoreCod !== this.getCurrentStoreCod());

      const transportModeList = rpt.DataAdditional?.find((e: any) => e.Name === 'TransportModeList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'transportModeList')?.Data
        ?? [];
      if (transportModeList.length > 0) {
        this.transportModeList = transportModeList;
      }

      const reasonList = rpt.DataAdditional?.find((e: any) => e.Name === 'ReasonTransferList')?.Data
        ?? rpt.DataAdditional?.find((e: any) => e.Name === 'reasonTransferList')?.Data
        ?? [];
      if (reasonList.length > 0) {
        this.reasonTransferList = reasonList;
      }

      const transferDetail = rpt.DataAdditional?.find((e: any) => e.Name === 'transferDetail')?.Data;
      this.hasTransferDraft = !!transferDetail?.transferHeadTs;
    }

    if (this.TransferReqCod) {
      await this.loadRequestData();
    }

    this.productList = [];
  }

  private async loadRequestData(): Promise<void> {
    const rpt: ResponseWsDto = await this.transferRequestService.FindDataForm(this.TransferReqCod);
    if (rpt.ErrorStatus) {
      this.toastrService.error(rpt.Message || 'No se pudo cargar la solicitud de transferencia');
      return;
    }

    const transferDetail = rpt.DataAdditional?.find((e: any) => e.Name === 'transferDetail')?.Data;
    if (!transferDetail?.transferHeadRequest) return;

    this.transferRequestRegister.transferHead = Object.assign(
      new TransferRequestHeadEntity(),
      transferDetail.transferHeadRequest
    );
    this.transferRequestRegister.transferDetList = (transferDetail.transferDetRequestList ?? []).map((detail: TransferRequestDetEntity) =>
      Object.assign(new TransferRequestDetEntity(), detail)
    );
    this.isTransferWithLots = this.transferRequestRegister.transferDetList.some(detail => !!detail.LotNumber);

    setTimeout(() => {
      this.cboStoreDest.nativeElement.value = this.transferRequestRegister.transferHead.StoreCodDest ?? '';
      this.txtObservation.nativeElement.value = this.transferRequestRegister.transferHead.Observation ?? '';
      void this.FindAllProduct(1);
    });
  }

  private updateRequestFromInputs(): void {
    const destStore = this.cboStoreDest.nativeElement.value;
    ValidationHelper.validateIsNotEmpty(destStore, 'Seleccione un local destino');
    if (destStore === this.getCurrentStoreCod()) {
      throw new Error('No puede seleccionar el mismo local como destino');
    }

    this.transferRequestRegister.transferHead.StoreCodOrigin = this.getCurrentStoreCod();
    this.transferRequestRegister.transferHead.StoreCodDest = destStore;
    this.transferRequestRegister.transferHead.StoreCodRequestedBy = destStore;
    this.transferRequestRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
    this.transferRequestRegister.transferHead.TransferStatus = TransferConstants.STATUS_DIRECT_DRAFT;
    this.transferRequestRegister.transferHead.Observation = this.txtObservation.nativeElement.value;
    this.transferRequestRegister.allowPartial = false;
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

  private applySavedRequest(
    saved: TransferRequestRegisterBundleDto,
    productByItem: Map<number, ProductEntity>
  ): void {
    this.transferRequestRegister.transferHead = Object.assign(new TransferRequestHeadEntity(), saved.transferHead);
    this.transferRequestRegister.transferDetList = (saved.transferDetList ?? []).map(detail => {
      const savedDetail = Object.assign(new TransferRequestDetEntity(), detail);
      savedDetail.Product = productByItem.get(savedDetail.ItemNumber) || savedDetail.Product;
      return savedDetail;
    });
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

  private buildPendingTransferRegister(): TransferRegisterBundleDto {
    const transferRegister = this.transferRequestRegister.buildTransferRegister();
    transferRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
    transferRegister.transferHead.TransferMode = TransferConstants.TRANSFER_MODE_DIRECT;
    transferRegister.transferHead.TransferStatus = TransferConstants.STATUS_PENDING;
    transferRegister.transferDetList.forEach(detail => {
      detail.TransferCod = transferRegister.transferHead.TransferCod;
      detail.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
      detail.NumUnitDispatch = detail.NumUnit;
    });
    return transferRegister;
  }

  private async savePendingTransferDraft(): Promise<boolean> {
    const rpt: ResponseWsDto = await this.transferService.Save(this.buildPendingTransferRegister());
    if (rpt.ErrorStatus) {
      this.toastrService.error(rpt.Message || 'La solicitud se guardó, pero no se pudo guardar el envío pendiente');
      return false;
    }

    this.hasTransferDraft = true;
    return true;
  }

  private async savePendingTransferDetail(detail: TransferRequestDetEntity): Promise<boolean> {
    if (!this.hasTransferDraft) {
      return await this.savePendingTransferDraft();
    }

    const transferDetail = detail.buildTransferDet();
    transferDetail.TransferCod = this.transferRequestRegister.transferHead.TransferReqCod;
    transferDetail.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
    transferDetail.NumUnitDispatch = transferDetail.NumUnit;

    const rpt: ResponseWsDto = await this.transferService.SaveDet(
      TransferDetRegisterMassiveDto.buildSimple(transferDetail)
    );
    if (rpt.ErrorStatus) {
      this.toastrService.error(rpt.Message || 'La solicitud se guardó, pero no se pudo actualizar el envío pendiente');
      return false;
    }
    return true;
  }

  private async deletePendingTransferDetail(detail: TransferRequestDetEntity): Promise<boolean> {
    if (!this.hasTransferDraft) return true;

    const transferDetail = detail.buildTransferDet();
    transferDetail.TransferCod = this.transferRequestRegister.transferHead.TransferReqCod;
    transferDetail.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;

    const rpt: ResponseWsDto = await this.transferService.DeleteDet(transferDetail);
    if (rpt.ErrorStatus) {
      this.toastrService.error(rpt.Message || 'La solicitud se actualizó, pero no se pudo eliminar el producto del envío pendiente');
      return false;
    }
    return true;
  }

  private async saveInitialRequest(): Promise<boolean> {
    if (this.isSavingDetail) return false;
    this.isSavingDetail = true;
    let requestPersisted = false;

    try {
      this.updateRequestFromInputs();
      this.transferRequestRegister.transferHead.TransferReqCod = await this.createRequestCode(this.getCurrentStoreCod());
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
        this.toastrService.error(rpt.Message || 'No se pudo guardar el envío directo');
        return false;
      }

      this.applySavedRequest(rpt.Data, productByItem);
      requestPersisted = true;
      if (!await this.savePendingTransferDraft()) {
        return false;
      }
      this.TransferReqCod = this.transferRequestRegister.transferHead.TransferReqCod;
      await this.router.navigate(
        ['/enterprise/transfer/pages/directtransfer'],
        { queryParams: { TransferReqCod: this.TransferReqCod }, replaceUrl: true }
      );
      this.toastrService.success('Envío directo guardado como pendiente');
      return true;
    } catch (e: any) {
      if (!requestPersisted) {
        this.transferRequestRegister.transferHead.TransferReqCod = '';
      }
      this.toastrService.error(e.message);
      return false;
    } finally {
      this.isSavingDetail = false;
    }
  }

  private async saveDetail(detail: TransferRequestDetEntity): Promise<boolean> {
    if (this.isSavingDetail) return false;

    try {
      this.updateRequestFromInputs();
      detail.TransferReqCod = this.transferRequestRegister.transferHead.TransferReqCod;
      detail.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
      this.isSavingDetail = true;
      const rpt: ResponseWsDto = await this.transferRequestDetService.Save(this.buildDetailSaveDto(detail));
      if (rpt.ErrorStatus) {
        this.toastrService.error(rpt.Message || 'No se pudo guardar el producto');
        return false;
      }

      const data: TransferRequestDetSaveDto = rpt.Data;
      this.transferRequestRegister.transferHead = Object.assign(new TransferRequestHeadEntity(), data.transferHead);
      const savedDetail = Object.assign(new TransferRequestDetEntity(), data.transferDet);
      this.syncSavedDetail(savedDetail, detail.Product);
      if (!await this.savePendingTransferDetail(savedDetail)) {
        return false;
      }
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
      this.updateRequestFromInputs();
      this.isSavingDetail = true;
      const rpt: ResponseWsDto = await this.transferRequestDetService.Delete(this.buildDetailSaveDto(detail));
      if (rpt.ErrorStatus) {
        this.toastrService.error(rpt.Message || 'No se pudo eliminar el producto');
        return false;
      }

      if (!await this.deletePendingTransferDetail(detail)) {
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

  private getCurrentStoreCod(): string {
    return this.session.getSessionStorageDto().StoreCod;
  }

  async FindAllProduct(Page: number) {
    const destStore = this.cboStoreDest?.nativeElement.value ?? '';
    if (!destStore) {
      this.toastrService.error('Seleccione un local destino para buscar productos');
      return;
    }
    if (destStore === this.getCurrentStoreCod()) {
      this.toastrService.error('No puede seleccionar el mismo local como destino');
      this.productList = [];
      return;
    }

    this.Page = Page;
    this.productSearch.StoreCod = this.getCurrentStoreCod();
    this.productSearch.Page = Page;
    this.productSearch.Query = this.txtSearch?.nativeElement.value ?? '';
    this.productSearch.StockMin = 1;

    const response: ResponseWsDto = await this.productSearchService.query(this.productSearch);

    if (!response.ErrorStatus) {
      this.responsePageSearch = response.Data;
      this.productList = this.responsePageSearch.resultSearch;
    }
  }

  async selectProduct(product: ProductSearchEntity) {
    if (this.isDigitalProduct(product)) {
      this.toastrService.warning('Los productos digitales no pueden utilizarse en transferencias.');
      return;
    }
    this.clearConversionValidationMessage();
    this.productSelect = product;
    this.selectedVisibleQuantity = 0;

    if (this.isTransferWithLots) {
      const transferDet = await this.buildTransferDetail(product, 0);
      this.openLotDispatchModal(transferDet);
      setTimeout(() => {
        (window as any).$('#modalLotDispatch').modal('show');
      });
      return;
    }

    this.txtNumUnit.nativeElement.value = '';
    const existing = this.transferRequestRegister.transferDetList.find(e => e.ProductCod === product.ProductCod && !e.LotNumber);
    if (existing) {
      this.selectedVisibleQuantity = this.toVisibleQuantity(existing.NumUnit, existing.ProductUnitFactor);
      this.txtNumUnit.nativeElement.value = String(this.selectedVisibleQuantity);
    }

    setTimeout(() => {
      (window as any).$('#modalProduct').modal('show');
    });
  }

  isDigitalProduct(product: ProductSearchEntity): boolean {
    return (product?.IsDigital || 'N').trim().toUpperCase() === 'S';
  }

  async AddProduct() {
    if (this.isSavingDetail) return;
    this.clearConversionValidationMessage();
    const product = this.productSelect;
    if (!product || !product.ProductCod) {
      this.toastrService.error('Seleccione un producto');
      return;
    }

    const numUnit = Number(this.txtNumUnit.nativeElement.value);
    if (!numUnit || numUnit <= 0) {
      this.toastrService.error('Ingrese una cantidad válida');
      return;
    }
    if (!await this.confirmInsufficientStockSelection(numUnit, product.ProductUnitName)) {
      return;
    }

    const isNewRequest = !this.transferRequestRegister.transferHead.TransferReqCod;
    const previousDetailList = this.cloneDetailList();
    let transferDet: TransferRequestDetEntity = new TransferRequestDetEntity();
    let transferDetExist: TransferRequestDetEntity | undefined = this.transferRequestRegister.transferDetList.find(
      e => e.ProductCod === product.ProductCod && !e.LotNumber
    );

    if (transferDetExist) {
      transferDet = transferDetExist;
    } else {
      transferDet = await this.buildTransferDetail(product, numUnit);
    }
    const previousNumUnit = transferDet.NumUnit;

    const ProductUnitFactor = transferDet.ProductUnitFactor > 0 ? transferDet.ProductUnitFactor : 1;
    transferDet.NumUnit = numUnit * ProductUnitFactor;

    if(!await this.validateConvertProductBetweenStores(transferDet)){
      transferDet.NumUnit = previousNumUnit;
      return;
    }
    transferDet.NumUnitDispatch = transferDet.NumUnit;

    if (!transferDetExist) {
      this.transferRequestRegister.transferDetList.push(transferDet);
    }

    const saved = isNewRequest
      ? await this.saveInitialRequest()
      : await this.saveDetail(transferDet);
    if (!saved) {
      if (this.transferRequestRegister.transferHead.TransferReqCod) {
        await this.loadRequestData();
      } else {
        this.transferRequestRegister.transferDetList = previousDetailList;
      }
      return;
    }

    this.txtNumUnit.nativeElement.value = '';
    this.selectedVisibleQuantity = 0;
    this.closeModal();
  }

  private async buildTransferDetail(product: ProductSearchEntity, numUnit: number): Promise<TransferRequestDetEntity> {
    const productInfoDto: ProductInfoDto = await this.findDetailById(product.ProductCod);
    const productEntity: ProductEntity = new ProductEntity();
    productEntity.ProductCod = product.ProductCod;
    productEntity.ProductName = product.ProductName;

    const transferDet = new TransferRequestDetEntity();
    transferDet.ProductCod = product.ProductCod;
    transferDet.Variant = productInfoDto.VariantList[0]?.Variant ?? '0000';
    transferDet.NumUnit = numUnit;
    transferDet.ProductUnitName = productInfoDto.Config.ProductUnitName || 'NIU';
    transferDet.ProductUnitFactor = productInfoDto.Config.ProductUnitFactor > 0 ? productInfoDto.Config.ProductUnitFactor : 1;
    transferDet.Product = productEntity;

    return transferDet;
  }

  closeModal() {
    this.clearConversionValidationMessage();
    this.btnCloseModal.nativeElement.click();
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

  editDetail(detail: TransferRequestDetEntity): void {
    this.clearConversionValidationMessage();
    const listedProduct = this.productList.find(product => product.ProductCod === detail.ProductCod);
    if (listedProduct) {
      this.productSelect = listedProduct;
    } else if (this.productSelect.ProductCod !== detail.ProductCod) {
      this.productSelect = new ProductSearchEntity();
      this.productSelect.ProductCod = detail.ProductCod;
      this.productSelect.ProductName = detail.Product?.ProductName || detail.ProductCod;
      this.productSelect.ProductUnitName = detail.ProductUnitName;
      this.productSelect.ProductUnitFactor = detail.ProductUnitFactor;
    }
    void this.refreshSelectedProductStock(detail.ProductCod);

    if (this.isTransferWithLots) {
      this.openLotDispatchModal(detail);
      return;
    }

    this.selectedVisibleQuantity = this.toVisibleQuantity(detail.NumUnit, detail.ProductUnitFactor);
    this.txtNumUnit.nativeElement.value = String(this.selectedVisibleQuantity);
  }

  private async refreshSelectedProductStock(productCod: string): Promise<void> {
    const currentStock = await this.transferStockAvailabilityService.findCurrentPhysicalStock(
      productCod,
      this.getCurrentStoreCod()
    );
    if (currentStock !== null && this.productSelect.ProductCod === productCod) {
      this.productSelect.NumPhysicalStock = currentStock;
    }
  }

  openLotDispatchModal(det: TransferRequestDetEntity) {
    this.clearConversionValidationMessage();
    this.selectedDetail = det;
    this.lotDispatchList = det.LotNumber
      ? [new TransferLotDispatchDto({
        NumUnit: det.NumUnit,
        LotNumber: det.LotNumber,
        ExpirationDate: det.ExpirationDate || ''
      })]
      : [];
    this.clearLotDispatchForm();
  }

  addLotDispatchLine(): void {
    try {
      this.clearConversionValidationMessage();
      const numUnit = Number(this.txtLotNumUnit.nativeElement.value);
      const lotNumber = this.txtLotNumber.nativeElement.value.trim();
      const expirationDate = this.txtExpirationDate.nativeElement.value;

      if (!numUnit || numUnit <= 0) {
        throw new Error('Ingrese una cantidad valida');
      }

      if (!lotNumber) {
        throw new Error('Ingrese el lote');
      }

      if (lotNumber.length > this.maxLotNumberLength) {
        throw new Error('El lote no puede superar 32 caracteres');
      }

      const internalQuantity = numUnit * (this.selectedDetail.ProductUnitFactor > 0 ? this.selectedDetail.ProductUnitFactor : 1);

      if (this.selectedDetail.NumUnit > 0 && (this.getLotDispatchTotal() + internalQuantity) > this.selectedDetail.NumUnit) {
        throw new Error('La cantidad no puede superar la cantidad solicitada');
      }

      const existingLot = this.lotDispatchList.find(e => e.LotNumber === lotNumber && e.ExpirationDate === expirationDate);

      if (existingLot) {
        existingLot.NumUnit += internalQuantity;
      } else {
        this.lotDispatchList.push(new TransferLotDispatchDto({
          NumUnit: internalQuantity,
          LotNumber: lotNumber,
          ExpirationDate: expirationDate
        }));
      }

      this.clearLotDispatchForm();
    } catch (e: any) {
      this.toastrService.error(e.message);
    }
  }

  removeLotDispatchLine(index: number): void {
    this.lotDispatchList.splice(index, 1);
  }

  getLotDispatchTotal(): number {
    return this.lotDispatchList.reduce((total, item) => total + Number(item.NumUnit || 0), 0);
  }

  getLotDispatchPending(): number {
    return Number(this.selectedDetail.NumUnit || 0) - this.getLotDispatchTotal();
  }

  async confirmLotDispatch(): Promise<void> {
    try {
      if (this.isSavingDetail) return;
      if (this.lotDispatchList.length === 0) {
        throw new Error('Debe agregar al menos un lote');
      }

      const otherProductQuantity = this.transferRequestRegister.transferDetList
        .filter(detail => detail !== this.selectedDetail && detail.ProductCod === this.selectedDetail.ProductCod)
        .reduce((total, detail) => total + Number(detail.NumUnit || 0), 0);
      const proposedInternalQuantity = otherProductQuantity + this.getLotDispatchTotal();
      const proposedVisibleQuantity = this.toVisibleQuantity(
        proposedInternalQuantity,
        this.selectedDetail.ProductUnitFactor
      );
      if (!await this.confirmInsufficientStockSelection(
        proposedVisibleQuantity,
        this.selectedDetail.ProductUnitName,
        this.selectedDetail.ProductUnitFactor
      )) {
        return;
      }

      if (this.selectedDetail.NumUnit > 0 && this.getLotDispatchTotal() < this.selectedDetail.NumUnit) {
        this.toastrService.warning('La cantidad despachada es menor a la cantidad solicitada');
      }

      const detailList: TransferRequestDetEntity[] = this.lotDispatchList.map((item, index) =>
        this.createLotDetail(item, index)
      );

      for (const item of detailList) {
        if (!await this.validateConvertProductBetweenStores(item)) {
          return;
        }
      }

      const isNewRequest = !this.transferRequestRegister.transferHead.TransferReqCod;
      const previousDetailList = this.cloneDetailList();
      this.replaceTransferLine(this.selectedDetail, detailList);

      if (isNewRequest) {
        if (!await this.saveInitialRequest()) {
          if (this.transferRequestRegister.transferHead.TransferReqCod) {
            await this.loadRequestData();
          } else {
            this.transferRequestRegister.transferDetList = previousDetailList;
          }
          return;
        }
      } else {
        for (const detail of detailList) {
          if (!await this.saveDetail(detail)) {
            await this.loadRequestData();
            return;
          }
        }
      }

      this.lotDispatchList = [];
      this.clearConversionValidationMessage();
      this.btnCloseLotDispatchModal.nativeElement.click();
    } catch (e: any) {
      this.toastrService.error(e.message);
    }
  }

  private createLotDetail(item: TransferLotDispatchDto, index: number): TransferRequestDetEntity {
    const detail = new TransferRequestDetEntity();
    detail.TransferReqCod = this.selectedDetail.TransferReqCod;
    detail.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
    detail.ProductCod = this.selectedDetail.ProductCod;
    detail.Variant = this.selectedDetail.Variant;
    detail.ItemNumber = index === 0 ? this.selectedDetail.ItemNumber : 0;
    detail.WarehouseCodOrigin = this.selectedDetail.WarehouseCodOrigin;
    detail.WarehouseCodDest = this.selectedDetail.WarehouseCodDest;
    detail.NumUnit = item.NumUnit;
    detail.NumUnitDispatch = item.NumUnit;
    detail.NumUnitReception = 0;
    detail.ProductUnitName = this.selectedDetail.ProductUnitName;
    detail.ProductUnitFactor = this.selectedDetail.ProductUnitFactor;
    detail.LotNumber = item.LotNumber;
    detail.ExpirationDate = item.ExpirationDate;
    detail.Product = this.selectedDetail.Product;

    return detail;
  }

  private replaceTransferLine(origin: TransferRequestDetEntity, detailList: TransferRequestDetEntity[]): void {
    const index = this.transferRequestRegister.transferDetList.indexOf(origin);
    if (index >= 0) {
      this.transferRequestRegister.transferDetList.splice(index, 1, ...detailList);
    } else {
      this.transferRequestRegister.transferDetList.push(...detailList);
    }
  }

  private clearLotDispatchForm(): void {
    setTimeout(() => {
      if (this.txtLotNumUnit) this.txtLotNumUnit.nativeElement.value = '';
      if (this.txtLotNumber) this.txtLotNumber.nativeElement.value = '';
      if (this.txtExpirationDate) this.txtExpirationDate.nativeElement.value = '';
    });
  }

  async searchCarrier() {
    if (!this.txtDriverDocNumber) return;

    const carrierCod = this.txtDriverDocNumber.nativeElement.value.trim();
    if (!carrierCod) {
      this.toastrService.warning('Ingrese el N° doc. conductor');
      return;
    }

    const rpt: ResponseWsDto = await this.carrierService.findById(carrierCod);
    if (rpt.ErrorStatus || !rpt.Data) {
      this.toastrService.warning(rpt.Message || 'Transportista no encontrado');
      return;
    }

    const carrier: CarrierEntity = rpt.Data;
    this.cboDriverDocType.nativeElement.value = carrier.DriverDocType || '';
    this.txtDriverDocNumber.nativeElement.value = carrier.DriverDocNumber || carrier.CarrierCod || carrierCod;
    this.txtDriverLicenseNumber.nativeElement.value = carrier.DriverLicenseNumber || '';
    this.txtVehiclePlate.nativeElement.value = carrier.VehiclePlate || '';
    this.txtCarrierRuc.nativeElement.value = carrier.CarrierRuc || '';
    this.txtCarrierName.nativeElement.value = carrier.CarrierName || '';

    this.toastrService.success('Datos del transportista cargados');
  }

  async Save() {
    try {
      if (this.isSavingDetail) return;
      this.updateRequestFromInputs();

      if (this.transferRequestRegister.transferDetList.length === 0) {
        throw new Error('Debe agregar al menos un producto');
      }

      if (this.isTransferWithLots && this.transferRequestRegister.transferDetList.some(e => !e.LotNumber)) {
        throw new Error('Debe indicar lote para todos los productos');
      }

      if (!this.transferRequestRegister.transferHead.TransferReqCod) {
        const saved = await this.saveInitialRequest();
        if (!saved) return;
      }

      const productByItem = new Map(
        this.transferRequestRegister.transferDetList.map(detail => [detail.ItemNumber, detail.Product])
      );
      this.isSavingDetail = true;
      const rptRequest: ResponseWsDto = await this.transferRequestService.Save(this.transferRequestRegister);
      if (rptRequest.ErrorStatus) {
        this.toastrService.error(rptRequest.Message || 'Ocurrio un error al registrar la solicitud');
        return;
      }
      this.applySavedRequest(rptRequest.Data, productByItem);

      const transferRegister = this.buildTransferRegister();

      const rpt: ResponseWsDto = await this.transferService.Save(transferRegister);

      if (!rpt.ErrorStatus) {
        const stockShortages = await this.transferStockAvailabilityService.findShortages(
          this.transferRequestRegister.transferDetList,
          this.getCurrentStoreCod()
        );
        if (stockShortages.length > 0) {
          const shortageSummary = this.transferStockAvailabilityService.formatShortageSummary(stockShortages);
          await this.alertService.warning(
            `La operación permanece guardada como pendiente, pero todavía no puede cerrarse porque no existe stock suficiente. ${shortageSummary}. Podrá confirmarla cuando ingrese el stock faltante.`,
            'Stock todavía insuficiente'
          );
          return;
        }

        const rptDispatch: ResponseWsDto = await this.dispatchDirectTransfer(transferRegister);
        if (rptDispatch.ErrorStatus) {
          this.toastrService.error(rptDispatch.Message || 'La transferencia se registro, pero no se pudo despachar');
          return;
        }

        const rptApproved: ResponseWsDto = await this.approveTransferRequest(
          this.transferRequestRegister.transferHead.TransferReqCod,
          this.transferRequestRegister.transferHead.Observation
        );
        if (rptApproved.ErrorStatus) {
          this.toastrService.error(rptApproved.Message || 'La transferencia se despacho, pero no se pudo aprobar la solicitud');
          return;
        }
        this.toastrService.success(rpt.Message || 'Envío directo registrado correctamente');
        setTimeout(() => {
          this.router.navigate(
            ['/enterprise/transfer/pages/transferdetail'],
            {
              queryParams: {
                TransferCod: transferRegister.transferHead.TransferCod,
                AutoPrint: 'Y',
                ReturnUrl: '/enterprise/transfer/pages/listtransferdispatch'
              }
            }
          );
        }, 1000);
      } else {
        this.toastrService.error(rpt.Message || 'Ocurrió un error al registrar el envío');
      }
    } catch (e: any) {
      this.toastrService.error(e.message);
    } finally {
      this.isSavingDetail = false;
    }
  }

  private async dispatchDirectTransfer(transferRegister: TransferRegisterBundleDto): Promise<ResponseWsDto> {
    transferRegister.transferDetList = transferRegister.transferDetList.map(det => {
      det.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
      det.TransferCod = transferRegister.transferHead.TransferCod;
      det.NumUnitDispatch = det.NumUnit;
      return det;
    });

    const request = new TransferDispatchDto();
    request.transferCod = transferRegister.transferHead.TransferCod;
    request.user = this.session.getSessionStorageDto().UserCod;
    request.transportModeCod = transferRegister.transferDocument.TransportModeCod;
    request.reasonTransferCod = transferRegister.transferDocument.ReasonTransferCod;
    request.vehiclePlate = transferRegister.transferDocument.VehiclePlate;
    request.driverDocType = transferRegister.transferDocument.DriverDocType;
    request.driverDocNumber = transferRegister.transferDocument.DriverDocNumber;
    request.driverLicenseNumber = transferRegister.transferDocument.DriverLicenseNumber;
    request.carrierRuc = transferRegister.transferDocument.CarrierRuc;
    request.carrierName = transferRegister.transferDocument.CarrierName;
    request.observation = transferRegister.transferHead.Observation;
    request.detailListRequest = transferRegister.transferDetList;

    return await this.transferService.DispatchTransfer(request);
  }

  private async approveTransferRequest(transferReqCod: string, observation: string): Promise<ResponseWsDto> {
    const requestApproved = new TransferReceiveDto();
    requestApproved.transferCod = transferReqCod;
    requestApproved.user = this.session.getSessionStorageDto().UserCod;
    requestApproved.observation = observation;
    requestApproved.typeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
    return await this.transferRequestService.ApprovedTransfer(requestApproved);
  }

  async findDetailById(ProductCod: string): Promise<ProductInfoDto> {
    let productInfoDto: ProductInfoDto = new ProductInfoDto();

    const rpt: ResponseWsDto = await this.productService.findDetailById(
      ProductCod,
      this.getCurrentStoreCod()
    );

    if (!rpt.ErrorStatus) {
      productInfoDto = rpt.Data;
    }

    return productInfoDto;
  }

  isProductSelected(product: ProductSearchEntity): boolean {
    return this.transferRequestRegister.transferDetList.some(
      detail => detail.ProductCod === product.ProductCod
    );
  }

  async createRequestCode(StoreCod: string) {
    const rpt: ResponseWsDto = await this.transferService.CreateCode(StoreCod);
    if (rpt?.ErrorStatus) {
      this.toastrService.error(rpt.Message);
      throw new Error(rpt.Message);
    }
    return String(rpt.Data);
  }

  toVisibleQuantity(internalQuantity: number, ProductUnitFactor: number): number {
    const factor = ProductUnitFactor > 0 ? ProductUnitFactor : 1;
    return internalQuantity / factor;
  }

  onSelectedQuantityChange(value: string): void {
    this.clearConversionValidationMessage();
    const quantity = Number(value);
    this.selectedVisibleQuantity = Number.isFinite(quantity) && quantity > 0 ? quantity : 0;
  }

  getCurrentVisibleStock(productCod: string = this.productSelect?.ProductCod, productUnitFactor?: number): number {
    const product = this.productSelect?.ProductCod === productCod
      ? this.productSelect
      : this.productList.find(item => item.ProductCod === productCod);
    const factor = productUnitFactor || product?.ProductUnitFactor || 1;

    return ProductUnitHelper.toVisibleQuantity(product?.NumPhysicalStock || 0, factor);
  }

  getProjectedVisibleStock(): number {
    return ProductUnitHelper.getVisibleStockAfterMovement(
      this.productSelect?.NumPhysicalStock || 0,
      this.selectedVisibleQuantity,
      this.productSelect?.ProductUnitFactor || 1
    );
  }

  private async confirmInsufficientStockSelection(
    requestedVisibleQuantity: number,
    productUnitName: string,
    productUnitFactor: number = this.productSelect?.ProductUnitFactor || 1
  ): Promise<boolean> {
    const currentVisibleStock = ProductUnitHelper.toVisibleQuantity(
      this.productSelect?.NumPhysicalStock || 0,
      productUnitFactor
    );
    if (requestedVisibleQuantity <= currentVisibleStock) {
      return true;
    }

    const confirmation = await this.alertService.waring(
      `La cantidad indicada (${ProductUnitHelper.formatQuantity(requestedVisibleQuantity)} ${productUnitName || 'NIU'}) supera el stock actual (${ProductUnitHelper.formatQuantity(currentVisibleStock)} ${productUnitName || 'NIU'}). Puede registrarla anticipadamente, pero la operación no podrá cerrarse hasta que exista stock suficiente.`,
      'Stock insuficiente'
    );
    return confirmation.isConfirmed;
  }

  getLotProjectedVisibleStock(): number {
    const currentStock = this.getCurrentVisibleStock(
      this.selectedDetail?.ProductCod,
      this.selectedDetail?.ProductUnitFactor
    );
    const assignedQuantity = this.toVisibleQuantity(
      this.getLotDispatchTotal(),
      this.selectedDetail?.ProductUnitFactor
    );
    const pendingInput = Number(this.txtLotNumUnit?.nativeElement.value || 0);
    const validPendingInput = Number.isFinite(pendingInput) && pendingInput > 0 ? pendingInput : 0;

    return currentStock - assignedQuantity - validPendingInput;
  }

  async validateConvertProductBetweenStores(transferDet: TransferRequestDetEntity){

    const request : ProductConversionRequestDto = new ProductConversionRequestDto();
    
    request.ProductCod = transferDet.ProductCod;
    request.quantityToConvert =  ProductUnitHelper.toVisibleQuantity(transferDet.NumUnit,transferDet.ProductUnitFactor);
    request.StoredCodDestination = this.getStoredCodDestination();
    request.StoredCodOrigin = this.getStoredCodOrigin();

    const rpt : ResponseWsDto = await this.transferRequestService.validateConvertProductBetweenStores(request);

    if(!rpt.ErrorStatus){
      const ProductConversionResult : ProductConversionResultDto = rpt.Data;

      if(!ProductConversionResult.valid){
        this.conversionValidationMessage = this.buildProductConversionErrorMessage(transferDet, ProductConversionResult);
        this.toastrService.error(this.conversionValidationMessage);
      } else {
        this.clearConversionValidationMessage();
      }
      return ProductConversionResult.valid;
    }else{
      this.conversionValidationMessage = "No se pudo evaluar la conversiÃ³n de productos entre locales, intentelo nuevamente.";
      this.toastrService.error("No se pudo evaluar la conversión de productos entre locales, intentelo nuevamente.");
    }
    return false;
  }

  clearConversionValidationMessage(): void {
    this.conversionValidationMessage = '';
  }

  private buildProductConversionErrorMessage(transferDet: TransferRequestDetEntity, result: ProductConversionResultDto): string {
    const productName = transferDet.Product?.ProductCod;
    const visibleQuantity = ProductUnitHelper.toVisibleQuantity(transferDet.NumUnit, transferDet.ProductUnitFactor);
    const originStore = this.getStoredCodOrigin();
    const destinationStore = this.getStoredCodDestination();
    const detail = result?.message ? ` Detalle: ${result.message}` : '';

    return `No se puede transferir ${visibleQuantity} ${transferDet.ProductUnitName || 'NIU'} del producto "${productName}" `
      + `desde el local ${originStore} hacia el local ${destinationStore}. `
      + `La cantidad no coincide con la unidad de venta configurada en el local destino.${detail}`;
  }

  getStoredCodOrigin(){
    return this.getCurrentStoreCod();
  }

  getStoredCodDestination(){
    return this.cboStoreDest.nativeElement.value;
  }


  buildTransferRegister(): TransferRegisterBundleDto {
      const transferRegister = this.buildPendingTransferRegister();

      transferRegister.transferDocument.TransportModeCod = this.cboTransportMode?.nativeElement.value ?? '';
      transferRegister.transferDocument.ReasonTransferCod = this.cboReason?.nativeElement.value ?? '';
      transferRegister.transferDocument.VehiclePlate = this.txtVehiclePlate?.nativeElement.value ?? '';
      transferRegister.transferDocument.DriverDocType = this.cboDriverDocType?.nativeElement.value ?? '';
      transferRegister.transferDocument.DriverDocNumber = this.txtDriverDocNumber?.nativeElement.value ?? '';
      transferRegister.transferDocument.DriverLicenseNumber = this.txtDriverLicenseNumber?.nativeElement.value ?? '';
      transferRegister.transferDocument.CarrierRuc = this.txtCarrierRuc?.nativeElement.value ?? '';
      transferRegister.transferDocument.CarrierName = this.txtCarrierName?.nativeElement.value ?? '';
      return transferRegister;
  }
}
