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
import { TransferDetEntity } from '../../model/entity/TransferDetEntity';
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
  transferRegister: TransferRegisterBundleDto = new TransferRegisterBundleDto();
  responsePageSearch: ResponsePageSearch<ProductSearchEntity> = new ResponsePageSearch();
  productList: ProductSearchEntity[] = [];
  productSelect: ProductSearchEntity = new ProductSearchEntity();
  productSearch: ProductSearchDto = new ProductSearchDto();
  storeList: StoreEntity[] = [];
  selectedDetail: TransferDetEntity = new TransferDetEntity();
  isTransferWithLots: boolean = false;
  lotDispatchList: TransferLotDispatchDto[] = [];
  conversionValidationMessage: string = '';
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
    private carrierService: CarrierService
  ) { }

  ngOnInit(): void {
    this.loadFormData();
  }

  async loadFormData() {
    const rpt: ResponseWsDto = await this.transferService.FindDataForm('');
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
    }

    this.productList = [];
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
    this.clearConversionValidationMessage();
    this.productSelect = product;

    if (this.isTransferWithLots) {
      const transferDet = await this.buildTransferDetail(product, 0);
      this.openLotDispatchModal(transferDet);
      setTimeout(() => {
        (window as any).$('#modalLotDispatch').modal('show');
      });
      return;
    }

    this.txtNumUnit.nativeElement.value = '';
    const existing = this.transferRegister.transferDetList.find(e => e.ProductCod === product.ProductCod && !e.LotNumber);
    if (existing) {
      this.txtNumUnit.nativeElement.value = String(this.toVisibleQuantity(existing.NumUnit, existing.ProductUnitFactor));
    }

    setTimeout(() => {
      (window as any).$('#modalProduct').modal('show');
    });
  }

  async AddProduct() {
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

    let transferDet: TransferDetEntity = new TransferDetEntity();
    let transferDetExist: TransferDetEntity | undefined = this.transferRegister.transferDetList.find(e => e.ProductCod === product.ProductCod && !e.LotNumber);

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

    if (!transferDetExist) {
      this.transferRegister.transferDetList.push(transferDet);
    }

    this.txtNumUnit.nativeElement.value = '';
    this.closeModal();

    if (this.isTransferWithLots) {
      this.openLotDispatchModal(transferDet);
      setTimeout(() => {
        (window as any).$('#modalLotDispatch').modal('show');
      }, 300);
    }
  }

  private async buildTransferDetail(product: ProductSearchEntity, numUnit: number): Promise<TransferDetEntity> {
    const productInfoDto: ProductInfoDto = await this.findDetailById(product.ProductCod);
    const productEntity: ProductEntity = new ProductEntity();
    productEntity.ProductCod = product.ProductCod;
    productEntity.ProductName = product.ProductName;

    const transferDet = new TransferDetEntity();
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

  private sameDetailLine(a: TransferDetEntity, b: TransferDetEntity): boolean {
    if ((a?.ItemNumber ?? 0) > 0 && (b?.ItemNumber ?? 0) > 0) {
      return a.ItemNumber === b.ItemNumber;
    }
    return a.ProductCod === b.ProductCod
      && a.Variant === b.Variant
      && (a.LotNumber ?? '') === (b.LotNumber ?? '')
      && (a.ExpirationDate ?? '') === (b.ExpirationDate ?? '');
  }

  async removeProduct(product: TransferDetEntity) {
    this.transferRegister.transferDetList = this.transferRegister.transferDetList.filter(e => !this.sameDetailLine(e, product));
  }

  openLotDispatchModal(det: TransferDetEntity) {
    this.clearConversionValidationMessage();
    this.selectedDetail = det;
    this.lotDispatchList = [];
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
      if (this.lotDispatchList.length === 0) {
        throw new Error('Debe agregar al menos un lote');
      }

      if (this.selectedDetail.NumUnit > 0 && this.getLotDispatchTotal() < this.selectedDetail.NumUnit) {
        this.toastrService.warning('La cantidad despachada es menor a la cantidad solicitada');
      }

      const detailList : TransferDetEntity[] = this.lotDispatchList.map((item, index) => this.createLotDetail(item, index))

      for(const item of detailList){
        if(!await this.validateConvertProductBetweenStores(item)){
          return;
        }
      }

      this.replaceTransferLine(this.selectedDetail,detailList );
      this.lotDispatchList = [];
      this.clearConversionValidationMessage();
      this.btnCloseLotDispatchModal.nativeElement.click();
    } catch (e: any) {
      this.toastrService.error(e.message);
    }
  }

  private createLotDetail(item: TransferLotDispatchDto, index: number): TransferDetEntity {
    const detail = new TransferDetEntity();
    detail.TransferCod = this.selectedDetail.TransferCod;
    detail.TypeOperation = this.selectedDetail.TypeOperation;
    detail.ProductCod = this.selectedDetail.ProductCod;
    detail.Variant = this.selectedDetail.Variant;
    detail.ItemNumber = index === 0 ? this.selectedDetail.ItemNumber : 0;
    detail.WarehouseCodOrigin = this.selectedDetail.WarehouseCodOrigin;
    detail.WarehouseCodDest = this.selectedDetail.WarehouseCodDest;
    detail.NumUnit = item.NumUnit;
    detail.NumUnitDispatch = item.NumUnit;
    detail.NumUnitReception = 0;
    detail.FlgRequested = this.selectedDetail.FlgRequested;
    detail.ProductUnitName = this.selectedDetail.ProductUnitName;
    detail.ProductUnitFactor = this.selectedDetail.ProductUnitFactor;
    detail.LotNumber = item.LotNumber;
    detail.ExpirationDate = item.ExpirationDate;
    detail.Product = this.selectedDetail.Product;

    return detail;
  }

  private replaceTransferLine(origin: TransferDetEntity, detailList: TransferDetEntity[]): void {
    const index = this.transferRegister.transferDetList.indexOf(origin);
    if (index >= 0) {
      this.transferRegister.transferDetList.splice(index, 1, ...detailList);
    } else {
      this.transferRegister.transferDetList.push(...detailList);
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
      const destStore = this.cboStoreDest.nativeElement.value;
      ValidationHelper.validateIsNotEmpty(destStore, 'Seleccione un local destino');
      if (destStore === this.getCurrentStoreCod()) {
        throw new Error('No puede seleccionar el mismo local como destino');
      }

      if (this.transferRegister.transferDetList.length === 0) {
        throw new Error('Debe agregar al menos un producto');
      }

      if (this.isTransferWithLots && this.transferRegister.transferDetList.some(e => !e.LotNumber)) {
        throw new Error('Debe indicar lote para todos los productos');
      }

      const originStore = this.getCurrentStoreCod();
      const observation = this.txtObservation.nativeElement.value;
      const transferReqCod = await this.createRequestCode(originStore);
      const requestRegister = this.buildTransferRequestRegister(transferReqCod, originStore, destStore, observation);

      const rptRequest: ResponseWsDto = await this.transferRequestService.Save(requestRegister);
      if (rptRequest.ErrorStatus) {
        this.toastrService.error(rptRequest.Message || 'Ocurrio un error al registrar la solicitud');
        return;
      }

      requestRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_SEND;
      requestRegister.transferHead.TransferStatus = TransferConstants.STATUS_PENDING;
      this.transferRegister = requestRegister.buildTransferRegister();

      this.transferRegister.transferDocument.TransportModeCod = this.cboTransportMode?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.ReasonTransferCod = this.cboReason?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.VehiclePlate = this.txtVehiclePlate?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.DriverDocType = this.cboDriverDocType?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.DriverDocNumber = this.txtDriverDocNumber?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.DriverLicenseNumber = this.txtDriverLicenseNumber?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.CarrierRuc = this.txtCarrierRuc?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.CarrierName = this.txtCarrierName?.nativeElement.value ?? '';

      const rpt: ResponseWsDto = await this.transferService.Save(this.transferRegister);

      if (!rpt.ErrorStatus) {
        const rptDispatch: ResponseWsDto = await this.dispatchDirectTransfer(this.transferRegister);
        if (rptDispatch.ErrorStatus) {
          this.toastrService.error(rptDispatch.Message || 'La transferencia se registro, pero no se pudo despachar');
          return;
        }

        const rptApproved: ResponseWsDto = await this.approveTransferRequest(this.transferRegister.transferHead.TransferCod, observation);
        if (rptApproved.ErrorStatus) {
          this.toastrService.error(rptApproved.Message || 'La transferencia se despacho, pero no se pudo aprobar la solicitud');
          return;
        }
        this.toastrService.success(rpt.Message || 'Envío directo registrado correctamente');
        setTimeout(() => {
          this.router.navigate(['/enterprise/transfer/pages/listtransferdispatch']);
        }, 1000);
      } else {
        this.toastrService.error(rpt.Message || 'Ocurrió un error al registrar el envío');
      }
    } catch (e: any) {
      this.toastrService.error(e.message);
    }
  }

  private buildTransferRequestRegister(transferReqCod: string, originStore: string, destStore: string, observation: string): TransferRequestRegisterBundleDto {
    const requestRegister = new TransferRequestRegisterBundleDto();
    requestRegister.transferHead.TransferReqCod = transferReqCod;
    requestRegister.transferHead.StoreCodOrigin = originStore;
    requestRegister.transferHead.StoreCodDest = destStore;
    requestRegister.transferHead.StoreCodRequestedBy = destStore;
    requestRegister.transferHead.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
    requestRegister.transferHead.TransferStatus = TransferConstants.STATUS_PENDING;
    requestRegister.transferHead.Observation = observation;
    requestRegister.allowPartial = false;

    requestRegister.transferDetList = this.transferRegister.transferDetList.map((det, index) => {
      const requestDet = new TransferRequestDetEntity();
      requestDet.TransferReqCod = transferReqCod;
      requestDet.ItemNumber = index + 1;
      requestDet.TypeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
      requestDet.ProductCod = det.ProductCod;
      requestDet.Variant = det.Variant;
      requestDet.WarehouseCodOrigin = det.WarehouseCodOrigin;
      requestDet.WarehouseCodDest = det.WarehouseCodDest;
      requestDet.NumUnit = det.NumUnit;
      requestDet.NumUnitDispatch = det.NumUnit;
      requestDet.NumUnitReception = 0;
      requestDet.ProductUnitName = det.ProductUnitName;
      requestDet.ProductUnitFactor = det.ProductUnitFactor;
      requestDet.LotNumber = det.LotNumber;
      requestDet.ExpirationDate = det.ExpirationDate;
      requestDet.Product = det.Product;
      return requestDet;
    });

    return requestRegister;
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

  async validateConvertProductBetweenStores(transferDet: TransferDetEntity){

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

  private buildProductConversionErrorMessage(transferDet: TransferDetEntity, result: ProductConversionResultDto): string {
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


}
