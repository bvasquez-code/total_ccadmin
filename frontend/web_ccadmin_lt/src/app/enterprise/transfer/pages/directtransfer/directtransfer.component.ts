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
  @ViewChild('txtDriverDocType') txtDriverDocType!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDriverDocNumber') txtDriverDocNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDriverLicenseNumber') txtDriverLicenseNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCarrierRuc') txtCarrierRuc!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCarrierName') txtCarrierName!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseModal') btnCloseModal!: ElementRef<HTMLButtonElement>;

  Page: number = 1;
  transferRegister: TransferRegisterBundleDto = new TransferRegisterBundleDto();
  responsePageSearch: ResponsePageSearch<ProductSearchEntity> = new ResponsePageSearch();
  productList: ProductSearchEntity[] = [];
  productSelect: ProductSearchEntity = new ProductSearchEntity();
  productSearch: ProductSearchDto = new ProductSearchDto();
  storeList: StoreEntity[] = [];

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
    private toastrService: ToastrService
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

  selectProduct(product: ProductSearchEntity) {
    this.txtNumUnit.nativeElement.value = '';
    this.productSelect = product;

    const existing = this.transferRegister.transferDetList.find(e => e.ProductCod === product.ProductCod);
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

    const numUnit = Number(this.txtNumUnit.nativeElement.value);
    if (!numUnit || numUnit <= 0) {
      this.toastrService.error('Ingrese una cantidad válida');
      return;
    }

    let transferDet: TransferDetEntity = new TransferDetEntity();
    let transferDetExist: TransferDetEntity | undefined = this.transferRegister.transferDetList.find(e => e.ProductCod === product.ProductCod);

    if (transferDetExist) {
      transferDet = transferDetExist;
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
      this.transferRegister.transferDetList.push(transferDet);
    }

    this.txtNumUnit.nativeElement.value = '';
    this.closeModal();
  }

  closeModal() {
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

      const originStore = this.getCurrentStoreCod();
      const observation = this.txtObservation.nativeElement.value;
      const transferReqCod = await this.createRequestCode(originStore);
      const requestRegister = this.buildTransferRequestRegister(transferReqCod, originStore, destStore, observation);

      const rptRequest: ResponseWsDto = await this.transferRequestService.RegisterBundle(requestRegister);
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
      this.transferRegister.transferDocument.DriverDocType = this.txtDriverDocType?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.DriverDocNumber = this.txtDriverDocNumber?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.DriverLicenseNumber = this.txtDriverLicenseNumber?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.CarrierRuc = this.txtCarrierRuc?.nativeElement.value ?? '';
      this.transferRegister.transferDocument.CarrierName = this.txtCarrierName?.nativeElement.value ?? '';

      const rpt: ResponseWsDto = await this.transferService.RegisterBundle(this.transferRegister);

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
}
