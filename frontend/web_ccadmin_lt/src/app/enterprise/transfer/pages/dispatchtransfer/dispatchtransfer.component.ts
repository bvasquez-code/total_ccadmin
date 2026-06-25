import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TransferDetailDto } from '../../model/dto/TransferDetailDto';
import { TransferDispatchDto } from '../../model/dto/TransferDispatchDto';
import { TransferDetEntity } from '../../model/entity/TransferDetEntity';
import { TransferService } from '../../service/TransferService';
import { TransferReceiveDto } from '../../model/dto/TransferReceiveDto';
import { TransferConstants } from '../../model/constants/TransferConstants';
import { TransferRequestService } from '../../service/TransferRequestService';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import { ProductEntity } from 'src/app/enterprise/product/model/entity/ProductEntity';
import { CarrierService } from '../../service/CarrierService';
import { CarrierEntity } from '../../model/entity/CarrierEntity';

@Component({
  selector: 'app-dispatchtransfer',
  templateUrl: './dispatchtransfer.component.html'
})
export class DispatchtransferComponent implements OnInit, ActionModalConfirmService {

  @ViewChild('cboTransportMode') cboTransportMode!: ElementRef<HTMLSelectElement>;
  @ViewChild('cboReason') cboReason!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtVehiclePlate') txtVehiclePlate!: ElementRef<HTMLInputElement>;
  @ViewChild('cboDriverDocType') cboDriverDocType!: ElementRef<HTMLSelectElement>;
  @ViewChild('txtDriverDocNumber') txtDriverDocNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtDriverLicenseNumber') txtDriverLicenseNumber!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCarrierRuc') txtCarrierRuc!: ElementRef<HTMLInputElement>;
  @ViewChild('txtCarrierName') txtCarrierName!: ElementRef<HTMLInputElement>;
  @ViewChild('txtObservation') txtObservation!: ElementRef<HTMLTextAreaElement>;

  TransferCod: string = '';
  transferDetail: TransferDetailDto = new TransferDetailDto();
  detailList: TransferDetEntity[] = [];
  selectedDetail: TransferDetEntity = new TransferDetEntity();

  @ViewChild('txtEditQty') txtEditQty!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseModalEdit') btnCloseModalEdit!: ElementRef<HTMLButtonElement>;

  @ViewChild('txtSearchBarcode') txtSearchBarcode!: ElementRef<HTMLInputElement>;
  @ViewChild('txtSearchBarcodeModal') txtSearchBarcodeModal!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseScanModal') btnCloseScanModal!: ElementRef<HTMLButtonElement>;

  scanSearchQuery: string = '';
  scannedProduct: ProductEntity | null = null;
  scanCounter: number = 0;

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
    private session: DataSesionService,
    private router: Router,
    private toastrService: ToastrService,
    private alertService: AlertService,
    private productService: ProductService,
    private carrierService: CarrierService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.TransferCod = (urlTree.queryParams['TransferCod']) ? urlTree.queryParams['TransferCod'] : '';
  }

  ngOnInit(): void {
    this.loadDetail();
  }

  async confirmAll() {
    const confirm = await this.alertService.waring('¿Estás seguro de verificar todos los productos?');
    if (confirm.isConfirmed) {
      this.detailList.forEach(d => d.NumUnitDispatch = d.NumUnit);
    }
  }

  openEditModal(det: TransferDetEntity) {
    this.selectedDetail = det;
    setTimeout(() => {
      if (this.txtEditQty) this.txtEditQty.nativeElement.value = String(det.NumUnit);
    }, 100);
  }

  saveQuantity() {
    if (!this.selectedDetail) return;
    const qty = Number(this.txtEditQty.nativeElement.value);
    if (qty < 0) {
      this.toastrService.error('La cantidad no puede ser negativa');
      return;
    }
    this.selectedDetail.NumUnitDispatch = qty;
    this.btnCloseModalEdit.nativeElement.click();
  }

  async checkQuantity(det: TransferDetEntity, event: any) {
    if (!det) return;
    const isChecking = event.target.checked;

    if (isChecking) {
      const confirm = await this.alertService.waring('¿Estás seguro de verificar este producto?');
      if (confirm.isConfirmed) {
        det.NumUnitDispatch = det.NumUnit;
      } else {
        event.target.checked = false;
      }
    } else {
      det.NumUnitDispatch = 0;
    }
  }

  private findDispatchLineByProduct(productCod: string): TransferDetEntity | undefined {
    return this.detailList.find(d => d.ProductCod === productCod && (d.NumUnitDispatch ?? 0) < (d.NumUnit ?? 0))
      ?? this.detailList.find(d => d.ProductCod === productCod);
  }

  async searchBarcode() {
    if (!this.txtSearchBarcode) return;
    const query = this.txtSearchBarcode.nativeElement.value.trim();
    if (!query) return;

    this.txtSearchBarcode.nativeElement.value = '';

    const rpt: ResponseWsDto = await this.productService.FindAll(query, 1);
    if (!rpt.ErrorStatus && rpt.Data?.resultSearch?.length > 0) {
      const foundProduct = rpt.Data.resultSearch.find((p: ProductEntity) => p.ProductCod === query) || rpt.Data.resultSearch[0];

      const inTransfer = this.findDispatchLineByProduct(foundProduct.ProductCod);
      if (!inTransfer) {
        this.toastrService.error('El producto escaneado no forma parte de los productos solicitados para despachar.');
        return;
      }

      this.scannedProduct = foundProduct;
      this.scanCounter = 1;

      (window as any).$('#modalScanBarcode').modal('show');

      setTimeout(() => {
        if (this.txtSearchBarcodeModal) this.txtSearchBarcodeModal.nativeElement.focus();
      }, 500);

    } else {
      this.toastrService.warning('Producto no encontrado');
    }
  }

  searchBarcodeInModal() {
    if (!this.txtSearchBarcodeModal || !this.scannedProduct) return;
    const query = this.txtSearchBarcodeModal.nativeElement.value.trim();
    if (!query) return;

    this.txtSearchBarcodeModal.nativeElement.value = '';

    if (query === this.scannedProduct.ProductCod) {
      this.scanCounter++;
    } else {
      this.toastrService.warning('Código diferente. Guarde el progreso actual o cierre el modal para escanear otro producto.');
    }
  }

  saveScanQuantity() {
    if (!this.scannedProduct) return;
    const det = this.findDispatchLineByProduct(this.scannedProduct.ProductCod);
    if (det) {
      det.NumUnitDispatch += this.scanCounter;
      // You can limit here if necessary: if(det.NumUnitDispatch > det.NumUnit) det.NumUnitDispatch = det.NumUnit;
    }
    this.btnCloseScanModal.nativeElement.click();
    setTimeout(() => {
      if (this.txtSearchBarcode) this.txtSearchBarcode.nativeElement.focus();
    }, 500);
  }

  numUnitDispatchConfirm(det: TransferDetEntity): boolean {
    return det.NumUnitDispatch > 0;
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

  preDispatch() {
    const unconfirmed = this.detailList.filter(d => !this.numUnitDispatchConfirm(d));
    if (unconfirmed.length > 0) {
      this.toastrService.warning('Debe confirmar todos los productos antes de despachar');
      return;
    }
    (window as any).$('#modal_dispatch').modal('show');
  }

  actionModal(ModalId: string): void {
    if (ModalId === 'modal_dispatch') {
      this.dispatchTransfer();
    }
  }

  async loadDetail() {
    const rpt: ResponseWsDto = await this.transferService.FindDataForm(this.TransferCod);
    if (!rpt.ErrorStatus) {

      this.transferDetail = rpt.Data ?? rpt.DataAdditional?.find((e: any) => e.Name === 'transferDetail')?.Data ?? new TransferDetailDto();
      this.detailList = (this.transferDetail.transferDetTsList || []).map((d: any) => {
        d.Confirmed = false;
        return d;
      });

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
  }

  async dispatchTransfer() {
    const request: TransferDispatchDto = new TransferDispatchDto();
    request.transferCod = this.TransferCod;
    request.user = this.session.getSessionStorageDto().UserCod;
    request.transportModeCod = this.cboTransportMode?.nativeElement.value ?? '';
    request.reasonTransferCod = this.cboReason?.nativeElement.value ?? '';
    request.vehiclePlate = this.txtVehiclePlate?.nativeElement.value ?? '';
    request.driverDocType = this.cboDriverDocType?.nativeElement.value ?? '';
    request.driverDocNumber = this.txtDriverDocNumber?.nativeElement.value ?? '';
    request.driverLicenseNumber = this.txtDriverLicenseNumber?.nativeElement.value ?? '';
    request.carrierRuc = this.txtCarrierRuc?.nativeElement.value ?? '';
    request.carrierName = this.txtCarrierName?.nativeElement.value ?? '';
    request.observation = this.txtObservation?.nativeElement.value ?? '';
    request.detailListRequest = this.detailList;

    const rpt: ResponseWsDto = await this.transferService.DispatchTransfer(request);

    if (!rpt.ErrorStatus) {

      this.toastrService.success(rpt.Message || 'Transferencia despachada correctamente');

      const requestApproved: TransferReceiveDto = new TransferReceiveDto();
      requestApproved.transferCod = this.TransferCod;
      requestApproved.user = this.session.getSessionStorageDto().UserCod;
      requestApproved.observation = this.txtObservation?.nativeElement.value ?? '';
      requestApproved.typeOperation = TransferConstants.TYPE_OPERATION_REQUEST;
      const rptApproved: ResponseWsDto = await this.transferRequestService.ApprovedTransfer(requestApproved);

      if (!rptApproved.ErrorStatus) {
        this.toastrService.success(rptApproved.Message || 'Transferencia aprobada correctamente');
        setTimeout(() => {
          this.router.navigate(['/enterprise/transfer/pages/listtransferdispatch']);
        }, 1000);
      }

    } else {
      this.toastrService.error(rpt.Message || 'No se pudo despachar la transferencia');
    }
  }
}
