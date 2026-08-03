import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { SaleHeadEntity } from '../../model/entity/SaleHeadEntity';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { SaleService } from '../../service/sale.service';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { ToastrService } from 'ngx-toastr';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { Router } from '@angular/router';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { PresaleCancellationDetailDto } from '../../model/dto/PresaleCancellationDetailDto';
import { PresaleService } from '../../service/presale.service';

@Component({
  selector: 'app-listsale',
  templateUrl: './listsale.component.html'
})
export class ListsaleComponent implements OnInit,ActionTableService<SaleHeadEntity>,ActionModalConfirmService{

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch : ResponsePageSearch<SaleHeadEntity> = new ResponsePageSearch();
  dataTablaGenetic : DataTablaGeneticDto<SaleHeadEntity> = new DataTablaGeneticDto();
  SaleHeadSelect : SaleHeadEntity = new SaleHeadEntity();

  constructor(
    private saleService : SaleService,
    private dataSesionService : DataSesionService,
    private toastrService : ToastrService,
    private presaleService: PresaleService,
    private router: Router
  ){
  }

  ngOnInit(): void {
    this.findAll(1,"");
  }
  actionModal(ModalId: string): void {
    if (ModalId === 'modal_cancel_pending_sale') this.cancelPendingSale();
  }
  filter(Page: number): void {
    this.findAll(Page,this.txtSearch.nativeElement.value);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<SaleHeadEntity>): void {
    
    const data : DataTablaGeneticDto<SaleHeadEntity> = new DataTablaGeneticDto();

    const showConfirmSale = (SaleHead : SaleHeadEntity) =>{
      return (SaleHead.SaleStatus === "P");
    }
    const showViewSale = (SaleHead : SaleHeadEntity) =>{
      return (SaleHead.SaleStatus === "C");
    }
    const showIssueFiscalDocument = (SaleHead : SaleHeadEntity) =>{
      return SaleHead.SaleStatus === "C" && SaleHead.HasFiscalDocument !== "S";
    }
    const viewFiscalStatus = (SaleHead: SaleHeadEntity) => {
      return SaleHead.SaleStatus === "X" ? "X" : SaleHead.HasFiscalDocument;
    }

    const viewClient = (SaleHead : SaleHeadEntity) =>{
      if(SaleHead.ClientCod !== null && SaleHead.ClientCod !== ""){
        if(SaleHead.Client.Person.PersonType === "01"){
          return SaleHead.Client.ClientCod + " - " + SaleHead.Client.Person.Names + " " + SaleHead.Client.Person.LastNames;
        }else{
          return SaleHead.Client.ClientCod + " - " + SaleHead.Client.Person.BusinessName;
        }
      }
      if(SaleHead.ClientCod === null){
        return "";
      }
      return "";
    }

    data.init(
      [
        { Name :  "Codigo" , key : "SaleCod" } ,
        { Name :  "Cliente" , key : "viewClient" , FunctionKey : viewClient } ,
        { Name :  "Monto total" , key : "NumTotalPrice", IsMoney : true } ,
        { Name :  "Vendedor" , key : "CreationUser"} ,
        { Name :  "Fecha de venta", key : "CreationDate" , IsDate : true },
        {
          Name: "Facturación",
          key: "HasFiscalDocument",
          FunctionKey: viewFiscalStatus,
          IsStatus: true,
          Html: {
            S: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            N: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
            X: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask: {
            S: 'Boleta/Factura emitida',
            N: 'Pendiente de facturar',
            X: 'No aplica'
          }
        },
        { Name :  "Estado" , 
          key : "SaleStatus" , 
          IsStatus : true,
          Html : {
            P : 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            C : 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            X : 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask : {
            P : "Pendiente",
            C : "Confirmado",
            X : "Anulada"
          },
        },
        { Name :  "Opciones" , 
          ColumnAction : true , 
          Id : ["SaleCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-check" , Title: "Procesar venta pendiente", Url : "/enterprise/sale/pages/createsale?SaleCod={SaleCod}", Function :showConfirmSale  },
            { Type : "Modal" , Name : "fa fa-ban" , Title: "Anular venta pendiente", Url : "#", ID: "modal_cancel_pending_sale", Function: showConfirmSale },
            { Type : "Url" , Name : "fa fa-file-invoice" , Title: "Emitir boleta o factura", Url : "/enterprise/sale/pages/createsaledocument?SaleCod={SaleCod}", Function :showIssueFiscalDocument  },
            { Type : "Url" , Name : "fa fa-search" , Title: "Ver venta confirmada", Url : "/enterprise/sale/pages/viewsale?SaleCod={SaleCod}", Function :showViewSale  }
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
    const rpt = await this.saleService.FindAll(search);

    if( !rpt.ErrorStatus )
    {
      this.responsePageSearch = rpt.Data;  

      this.loadingTable(this.responsePageSearch);
    }
  }
  getDataRow(item: any): void {
    this.SaleHeadSelect = item;
  }

  async cancelPendingSale(): Promise<void> {
    const detailResponse: ResponseWsDto = await this.presaleService.cancellationDetail(
      this.SaleHeadSelect.PresaleCod
    );
    if (detailResponse.ErrorStatus) {
      this.toastrService.error(detailResponse.Message);
      return;
    }

    const detail: PresaleCancellationDetailDto = detailResponse.Data;
    if (!detail.HasStockReservation) {
      this.toastrService.warning(
        'Esta venta pertenece a una preventa antigua sin reserva. La anulacion forzada solo puede realizarse desde la lista de preventas.'
      );
      return;
    }

    if (Number(detail.PendingPaymentAmount || 0) > 0) {
      this.toastrService.warning('La venta tiene pagos pendientes de anular.');
      await this.router.navigate(
        ['/enterprise/sale/pages/cancelpresalepayments'],
        {
          queryParams: {
            PresaleCod: this.SaleHeadSelect.PresaleCod,
            Mode: 'regular',
            ReturnUrl: '/enterprise/sale/pages/listsale'
          }
        }
      );
      return;
    }

    const response: ResponseWsDto = await this.presaleService.cancel(this.SaleHeadSelect.PresaleCod);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message);
      return;
    }

    this.toastrService.success('Venta pendiente anulada correctamente.');
    await this.findAll(1, this.txtSearch.nativeElement.value);
  }


}
