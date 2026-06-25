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
    private toastrService : ToastrService
  ){
  }

  ngOnInit(): void {
    this.findAll(1,"");
  }
  actionModal(ModalId: string): void {
    
  }
  filter(Page: number): void {
    this.findAll(Page,this.txtSearch.nativeElement.value);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<SaleHeadEntity>): void {
    
    const data : DataTablaGeneticDto<SaleHeadEntity> = new DataTablaGeneticDto();

    const showConfirmSale = (SaleHead : SaleHeadEntity) =>{
      return (SaleHead.SaleStatus !== "C");
    }
    const showViewSale = (SaleHead : SaleHeadEntity) =>{
      return (SaleHead.SaleStatus === "C");
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
          Id : ["SaleCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-check" , Url : "/enterprise/sale/pages/createsale?SaleCod={SaleCod}", Function :showConfirmSale  },
            { Type : "Url" , Name : "fa fa-search" , Url : "/enterprise/sale/pages/createsale?SaleCod={SaleCod}", Function :showViewSale  }
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
    
  }


}
