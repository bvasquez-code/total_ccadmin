import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { PresaleService } from '../../service/presale.service';
import { SearchDto } from '../../../shared/model/dto/SearchDto';
import { PresaleHeadEntity } from '../../model/entity/PresaleHeadEntity';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { PresaleRegisterDto } from '../../model/dto/PresaleRegisterDto';
import { ToastrService } from 'ngx-toastr';
import { Router } from '@angular/router';
import { PresaleCancellationDetailDto } from '../../model/dto/PresaleCancellationDetailDto';

@Component({
  selector: 'app-listpresale',
  templateUrl: './listpresale.component.html'
})
export class ListpresaleComponent implements OnInit,ActionTableService<PresaleHeadEntity>,ActionModalConfirmService{


  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  
  responsePageSearch : ResponsePageSearch<PresaleHeadEntity> = new ResponsePageSearch();
  
  dataTablaGenetic : DataTablaGeneticDto<PresaleHeadEntity> = new DataTablaGeneticDto();

  PresaleHeadSelect : PresaleHeadEntity = new PresaleHeadEntity();

  constructor(
    private presaleService : PresaleService,
    private dataSesionService : DataSesionService,
    private toastrService : ToastrService,
    private router: Router
  )
  {
    
  }

  ngOnInit(): void {
    this.findAll(1,"");
  }

  filter(Page: number): void {
    this.findAll(Page,this.txtSearch.nativeElement.value);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<PresaleHeadEntity>): void {
    const showPending = (presale: PresaleHeadEntity): boolean => presale.SaleStatus === 'P';
    const hasClosedSale = (presale: PresaleHeadEntity): boolean =>
      presale.RelatedSaleStatus === 'C';
    const showCancellable = (presale: PresaleHeadEntity): boolean =>
      (presale.SaleStatus === 'P' || presale.SaleStatus === 'C') && !hasClosedSale(presale);
    const showForceCancellation = (presale: PresaleHeadEntity): boolean =>
      presale.SaleStatus === 'C' && !hasClosedSale(presale);
    const viewRelatedSale = (presale: PresaleHeadEntity): string => {
      if (!presale.RelatedSaleCod) return '';
      const status: Record<string, string> = {
        P: 'Pendiente',
        C: 'Confirmada',
        X: 'Anulada'
      };
      return `${presale.RelatedSaleCod} - ${status[presale.RelatedSaleStatus] || presale.RelatedSaleStatus}`;
    };

    const data : DataTablaGeneticDto<PresaleHeadEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "PresaleCod" } ,
        { Name :  "Monto total" , key : "NumTotalPrice", IsMoney : true } ,
        { Name :  "Venta relacionada" , key : "RelatedSaleCod", FunctionKey: viewRelatedSale } ,
        { Name :  "Vendedor" , key : "CreationUser"} ,
        { Name :  "Fecha de venta", key : "CreationDate" , IsDate : true },
        { Name :  "Estado" , 
          key : "SaleStatus" , 
          IsStatus : true,
          Html : {
            P : 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            C : 'badge badge-sm bgc-success-d1 text-white pb-1 px-25'
          },
          Mask : {
            P : "Pendiente",
            C : "Confirmado"
          },
        },
        { Name :  "Opciones" , 
          ColumnAction : true , 
          Id : ["PresaleCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-pencil-alt" , Title: "Editar preventa", Url : "/enterprise/sale/pages/createpresale?PresaleCod={PresaleCod}", Function: showPending },
            { Type : "Modal" , Name : "fa fa-check" , Title: "Confirmar preventa", Url : "#", ID : "modal_confirm", Function: showPending },
            { Type : "Modal" , Name : "fa fa-ban" , Title: "Anulacion regular", Url : "#", ID : "modal_cancel", Function: showCancellable },
            { Type : "Modal" , Name : "fa fa-exclamation-triangle" , Title: "Forzar anulacion sin reserva", Url : "#", ID : "modal_force_cancel", Function: showForceCancellation }
          ] 
        }
      ],
      {
        data : responsePageSearch
      },
      "Lista de solicitudes de venta"
    );

    this.dataTablaGenetic = data;

  }
  async findAll(Page: number, Query: string): Promise<void> {
    
    const search : SearchDto = new SearchDto();
    search.Page = Page;
    search.StoreCod = this.dataSesionService.getSessionStorageDto().StoreCod;
    search.Query = Query;
    const rpt = await this.presaleService.findAll(search);

    if( !rpt.ErrorStatus )
    {
      this.responsePageSearch = rpt.Data;  

      this.loadingTable(this.responsePageSearch);
    }

  }
  getDataRow(item: any): void {
    this.PresaleHeadSelect = item;
  }
  actionModal(ModalId: string): void {

    if(ModalId === "modal_confirm") this.Confirm();
    if(ModalId === "modal_cancel") this.cancelPresale(false);
    if(ModalId === "modal_force_cancel") this.cancelPresale(true);
  
  }


  async Confirm(){

    const PresaleRegister : PresaleRegisterDto = new PresaleRegisterDto();

    PresaleRegister.Headboard.PresaleCod = this.PresaleHeadSelect.PresaleCod;

    const rpt : ResponseWsDto = await this.presaleService.confirm(PresaleRegister);

    if(!rpt.ErrorStatus){
      this.toastrService.success("Solicitud de venta confirmada");
      this.findAll(1,"");
    }

  }

  async cancelPresale(forced: boolean): Promise<void> {
    const presaleCod: string = this.PresaleHeadSelect.PresaleCod;
    const detailResponse: ResponseWsDto = await this.presaleService.cancellationDetail(presaleCod);

    if (detailResponse.ErrorStatus) {
      this.toastrService.error(detailResponse.Message);
      return;
    }

    const detail: PresaleCancellationDetailDto = detailResponse.Data;
    if (Number(detail.PendingPaymentAmount || 0) > 0) {
      this.toastrService.warning("La venta tiene pagos pendientes de anular.");
      await this.router.navigate(
        ['/enterprise/sale/pages/cancelpresalepayments'],
        { queryParams: { PresaleCod: presaleCod, Mode: forced ? 'forced' : 'regular' } }
      );
      return;
    }

    const response: ResponseWsDto = forced
      ? await this.presaleService.forceCancel(presaleCod)
      : await this.presaleService.cancel(presaleCod);

    if (response.ErrorStatus) {
      this.toastrService.error(response.Message);
      return;
    }

    this.toastrService.success(forced
      ? "Preventa anulada de manera forzada"
      : "Preventa anulada correctamente");
    await this.findAll(1, this.txtSearch.nativeElement.value);
  }

}
