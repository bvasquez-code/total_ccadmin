import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TransferDetailDto } from '../../model/dto/TransferDetailDto';
import { TransferReceiveDto } from '../../model/dto/TransferReceiveDto';
import { TransferRequestService } from '../../service/TransferRequestService';
import { TransferService } from '../../service/TransferService';
import { TransferDetEntity } from '../../model/entity/TransferDetEntity';
import { AlertService } from 'src/app/enterprise/shared/service/AlertService';
import { ProductService } from 'src/app/enterprise/product/service/product.service';
import { ProductEntity } from 'src/app/enterprise/product/model/entity/ProductEntity';
import { ProductInfoDto } from 'src/app/enterprise/product/model/dto/ProductInfoDto';
import { WarehouseEntity } from 'src/app/enterprise/shared/model/entity/WarehouseEntity';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { TransferDetRegisterMassiveDto } from '../../model/dto/TransferDetRegisterMassiveDto';
import { TransferRequestDetailDto } from '../../model/dto/TransferRequestDetailDto';

@Component({
  selector: 'app-receivetransfer',
  templateUrl: './receivetransfer.component.html'
})
export class ReceivetransferComponent implements OnInit, ActionModalConfirmService {

  @ViewChild('txtObservation') txtObservation!: ElementRef<HTMLTextAreaElement>;

  TransferReqCod: string = '';
  transferDetail: TransferRequestDetailDto = new TransferRequestDetailDto();
  detailList: TransferDetEntity[] = [];
  selectedDetail: TransferDetEntity | null = null;
  warehouseList: WarehouseEntity[] = [];
  storeList: StoreEntity[] = [];

  @ViewChild('txtEditQty') txtEditQty!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseModalEdit') btnCloseModalEdit!: ElementRef<HTMLButtonElement>;

  @ViewChild('txtSearchBarcode') txtSearchBarcode!: ElementRef<HTMLInputElement>;
  @ViewChild('txtSearchBarcodeModal') txtSearchBarcodeModal!: ElementRef<HTMLInputElement>;
  @ViewChild('btnCloseScanModal') btnCloseScanModal!: ElementRef<HTMLButtonElement>;

  scanSearchQuery: string = '';
  scannedProduct: ProductEntity | null = null;
  scanCounter: number = 0;

  constructor(
    private transferRequestService: TransferRequestService,
    private transferService: TransferService,
    private session: DataSesionService,
    private router: Router,
    private toastrService: ToastrService,
    private alertService: AlertService,
    private productService: ProductService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.TransferReqCod = (urlTree.queryParams['TransferReqCod']) ? urlTree.queryParams['TransferReqCod'] : '';
  }

  ngOnInit(): void {
    this.loadDetail();
  }

  actionModal(ModalId: string): void {
    if (ModalId === 'modal_receive') {
      this.receiveTransfer();
    }
  }

  async loadDetail() {
    const rpt: ResponseWsDto = await this.transferRequestService.FindDataForm(this.TransferReqCod);
    if (!rpt.ErrorStatus) {
      this.transferDetail = rpt.Data ?? rpt.DataAdditional?.find(e => e.Name === 'transferDetail')?.Data ?? new TransferDetailDto();
      this.detailList = this.transferDetail.transferDetList || [];
      this.warehouseList = rpt.DataAdditional?.find(e => e.Name === 'warehouseList')?.Data ?? [];
      this.storeList = rpt.DataAdditional?.find(e => e.Name === 'storeList')?.Data ?? [];
    }
  }

  async confirmAll() {
    const confirm = await this.alertService.waring('¿Estás seguro de verificar todos los productos?');
    if (confirm.isConfirmed) {
      this.detailList.forEach(d => d.NumUnitReception = d.NumUnit);
      const rpt: TransferDetRegisterMassiveDto = await this.saveDet(TransferDetRegisterMassiveDto.build(this.detailList));
      if (rpt.transferDetList.length > 0) {
        this.detailList = rpt.transferDetList;
      }
    }
  }

  async toggleProductCheck(det: TransferDetEntity, event: any) {
    const isChecking = event.target.checked;
    const NumUnitReceptionOld: number = det.NumUnitReception;
    if (isChecking) {
      const confirm = await this.alertService.waring('¿Estás seguro de verificar este producto?');
      if (confirm.isConfirmed) {
        det.NumUnitReception = det.NumUnit;
        const rpt: TransferDetRegisterMassiveDto = await this.saveDet(TransferDetRegisterMassiveDto.buildSimple(det));
        if (rpt.transferDetList.length > 0) {
          det.NumUnitReception = rpt.transferDetList[0].NumUnitReception;
        } else {
          det.NumUnitReception = NumUnitReceptionOld;
          event.target.checked = false;
        }
      } else {
        event.target.checked = false;
      }
    } else {
      det.NumUnitReception = 0;
      const rpt: TransferDetRegisterMassiveDto = await this.saveDet(TransferDetRegisterMassiveDto.buildSimple(det));
      if (rpt.transferDetList.length > 0) {
        det.NumUnitReception = rpt.transferDetList[0].NumUnitReception;
      } else {
        det.NumUnitReception = NumUnitReceptionOld;
        event.target.checked = true;
      }
    }
  }

  async findDetailById(ProductCod: string): Promise<ProductInfoDto | null> {
    const rpt: ResponseWsDto = await this.productService.findDetailById(ProductCod, this.session.getSessionStorageDto().StoreCod);
    if (!rpt.ErrorStatus) {
      const productInfoDto: ProductInfoDto = rpt.Data;
      return productInfoDto;
    }
    return null;
  }

  private findReceptionLineByProduct(productCod: string): TransferDetEntity | undefined {
    return this.detailList.find(d => d.ProductCod === productCod && (d.NumUnitReception ?? 0) < (d.NumUnit ?? 0))
      ?? this.detailList.find(d => d.ProductCod === productCod);
  }

  async searchBarcode() {
    if (!this.txtSearchBarcode) return;
    const query = this.txtSearchBarcode.nativeElement.value.trim();
    if (!query) return;

    this.txtSearchBarcode.nativeElement.value = '';

    const rpt: ResponseWsDto = await this.productService.FindAll(query, 1);
    if (!rpt.ErrorStatus && rpt.Data?.resultSearch?.length > 0) {
      const foundProduct: ProductEntity = rpt.Data.resultSearch.find((p: ProductEntity) => p.ProductCod === query) || rpt.Data.resultSearch[0];

      let inTransfer = this.findReceptionLineByProduct(foundProduct.ProductCod);
      if (!inTransfer) {
        const confirmResult = await this.alertService.waring('El producto escaneado no fue solicitado en esta transferencia. ¿Desea agregarlo y recibirlo de todas formas?');
        if (confirmResult.isConfirmed) {

          const productInfoDto = await this.findDetailById(foundProduct.ProductCod);

          console.log({ transferDetail: this.transferDetail });

          inTransfer = new TransferDetEntity();
          inTransfer.ItemNumber = this.detailList.length + 1;
          inTransfer.TransferCod = this.transferDetail.transferHead.TransferCod;
          inTransfer.TypeOperation = this.transferDetail.transferHead.TypeOperation;
          inTransfer.ProductCod = foundProduct.ProductCod;
          inTransfer.Variant = productInfoDto?.VariantList[0].Variant || "0000";
          inTransfer.Product = foundProduct;
          inTransfer.WarehouseCodOrigin = this.getWarehouseMain(this.transferDetail.transferHead.StoreCodOrigin).WarehouseCod;
          inTransfer.WarehouseCodDest = this.getWarehouseMain(this.transferDetail.transferHead.StoreCodDest).WarehouseCod;
          inTransfer.NumUnit = 0;
          inTransfer.NumUnitReception = 0;
          inTransfer.FlgRequested = 'N';

          if (await this.saveDet(TransferDetRegisterMassiveDto.buildSimple(inTransfer))) {
            this.detailList.push(inTransfer);
          }
        } else {
          return;
        }
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
    const det = this.findReceptionLineByProduct(this.scannedProduct.ProductCod);
    if (det) {
      det.NumUnitReception = (det.NumUnitReception || 0) + this.scanCounter;
    }
    this.btnCloseScanModal.nativeElement.click();
    setTimeout(() => {
      if (this.txtSearchBarcode) this.txtSearchBarcode.nativeElement.focus();
    }, 500);
  }

  openEditModal(det: TransferDetEntity) {
    this.selectedDetail = det;
    setTimeout(() => {
      if (this.txtEditQty) this.txtEditQty.nativeElement.value = String(det.NumUnitReception > 0 ? det.NumUnitReception : det.NumUnit);
    }, 100);
  }

  async saveQuantity() {
    if (!this.selectedDetail) return;
    const qty = Number(this.txtEditQty.nativeElement.value);
    if (qty < 0) {
      this.toastrService.error('La cantidad no puede ser negativa');
      return;
    }
    const NumUnitReceptionOld: number = this.selectedDetail.NumUnitReception;
    this.selectedDetail.NumUnitReception = qty;
    const rpt: TransferDetRegisterMassiveDto = await this.saveDet(TransferDetRegisterMassiveDto.buildSimple(this.selectedDetail));
    if (rpt.transferDetList.length > 0) {
      this.selectedDetail.NumUnitReception = rpt.transferDetList[0].NumUnitReception;
    } else {
      this.selectedDetail.NumUnitReception = NumUnitReceptionOld;
    }
    this.btnCloseModalEdit.nativeElement.click();
  }

  preReceive() {
    const unconfirmed = this.detailList.filter(d => d.NumUnitReception <= 0);
    if (unconfirmed.length > 0) {
      this.toastrService.warning('Debe confirmar (recepcionar más de 0) todos los productos antes de recepcionar la transferencia');
      return;
    }
    (window as any).$('#modal_receive').modal('show');
  }

  async receiveTransfer() {
    const request: TransferReceiveDto = new TransferReceiveDto();
    request.transferCod = this.TransferReqCod;
    request.user = this.session.getSessionStorageDto().UserCod;
    request.observation = this.txtObservation?.nativeElement.value ?? '';
    request.detailListReceive = this.detailList;

    const rpt: ResponseWsDto = await this.transferService.ReceiveTransfer(request);

    if (!rpt.ErrorStatus) {

      const rpt2: ResponseWsDto = await this.transferRequestService.ConfirmedTransfer(request);

      if (!rpt2.ErrorStatus) {
        this.toastrService.success(rpt2.Message || 'Transferencia recibida correctamente');
        setTimeout(() => {
          this.router.navigate(['/enterprise/transfer/pages/listtransferrequest']);
        }, 1000);
      } else {
        this.toastrService.error(rpt2.Message || 'No se pudo recepcionar la transferencia');
      }

    } else {
      this.toastrService.error(rpt.Message || 'No se pudo recepcionar la transferencia');
    }
  }

  getWarehouseMain(StoreCod: String): WarehouseEntity {
    return this.warehouseList.filter(w => w.StoreCod === StoreCod)[0];
  }

  async saveDet(det: TransferDetRegisterMassiveDto): Promise<TransferDetRegisterMassiveDto> {
    const rpt: ResponseWsDto = await this.transferService.SaveDet(det);
    if (!rpt.ErrorStatus) {
      this.toastrService.success('Detalle guardado correctamente');
      const result: TransferDetRegisterMassiveDto = rpt.Data;
      return result;
    } else {
      this.toastrService.error('No se pudo guardar el detalle');
      return new TransferDetRegisterMassiveDto();
    }
  }

}
