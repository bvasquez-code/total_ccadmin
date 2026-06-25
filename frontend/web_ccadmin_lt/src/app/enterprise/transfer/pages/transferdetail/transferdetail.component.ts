import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TransferDetailDto } from '../../model/dto/TransferDetailDto';
import { TransferRequestService } from '../../service/TransferRequestService';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { TransferRequestDetailDto } from '../../model/dto/TransferRequestDetailDto';
import { TransferService } from '../../service/TransferService';
import { TicketSunatService } from 'src/app/enterprise/sale/service/TicketSunatService';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-transferdetail',
  templateUrl: './transferdetail.component.html'
})
export class TransferdetailComponent implements OnInit {

  TransferCod: string = '';
  transferDetail: TransferRequestDetailDto = new TransferRequestDetailDto();
  transferDetailPrintData: ResponseWsDto = new ResponseWsDto();
  storeList: StoreEntity[] = [];

  statusHtml: any = {
    P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25',
    C: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
    D: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
    F: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
    R: 'badge badge-sm bgc-dark text-white pb-1 px-25',
    X: 'badge badge-sm bgc-secondary text-white pb-1 px-25',
    A: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
  };

  statusMask: any = {
    P: 'Pendiente',
    C: 'Confirmada',
    D: 'Despachada',
    F: 'Finalizada',
    R: 'Rechazada',
    X: 'Anulada',
    A: 'Aprobada'
  };

  constructor(
    private transferRequestService: TransferRequestService,
    private ticketSunatService: TicketSunatService,
    private toastrService: ToastrService,
    private router: Router
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.TransferCod = (urlTree.queryParams['TransferCod']) ? urlTree.queryParams['TransferCod'] : '';
  }

  ngOnInit(): void {
    this.loadDetail();
  }

  async loadDetail() {

    const rpt: ResponseWsDto = await this.transferRequestService.FindDataForm(this.TransferCod);
    if (!rpt.ErrorStatus) {
      this.transferDetail = rpt.DataAdditional?.find(e => e.Name === 'transferDetail')?.Data ?? new TransferDetailDto();
      this.storeList = rpt.DataAdditional?.find(e => e.Name === 'storeList')?.Data ?? [];
    }
  }

  getStoreDescription(storeCod: string): string {
    const store: StoreEntity | any = this.storeList.find(e => e.StoreCod === storeCod);
    return store?.StoreCod + ' - ' + store?.Name;
  }


  async findDataPrint() {
    const rpt: ResponseWsDto = await this.transferRequestService.FindDataPrint(this.TransferCod);
    if (!rpt.ErrorStatus) {
      this.transferDetailPrintData = rpt;
    } else {
      this.toastrService.error(rpt.Message || 'No se pudo obtener la información de impresión');
    }
  }

  async print() {
    await this.findDataPrint();
    if (!this.transferDetailPrintData?.ErrorStatus) {
      await this.ticketSunatService.printTransferReferralGuide(this.transferDetailPrintData);
    }
  }

}
