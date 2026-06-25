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
    private toastrService : ToastrService
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
    
    const data : DataTablaGeneticDto<PresaleHeadEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "PresaleCod" } ,
        { Name :  "Monto total" , key : "NumTotalPrice", IsMoney : true } ,
        { Name :  "Vendedor" , key : "CreationUser"} ,
        { Name :  "Fecha de venta", key : "CreationDate" , IsDate : true },
        { Name :  "Estado" , 
          key : "SaleStatus" , 
          IsStatus : true,
          Html : {
            P : 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            C : 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
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
            { Type : "Url" , Name : "fa fa-pencil-alt" , Url : "/enterprise/sale/pages/createpresale?PresaleCod={PresaleCod}" },
            { Type : "Url" , Name : "fa fa-trash-alt" , Url : "#" },
            { Type : "Modal" , Name : "fa fa-check" , Url : "#", ID : "modal_confirm" }
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

}
