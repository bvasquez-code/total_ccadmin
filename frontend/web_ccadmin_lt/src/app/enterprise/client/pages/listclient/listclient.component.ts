import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ClientService } from '../../service/client.service';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ClientEntity } from '../../model/entity/ClientEntity';

@Component({
  selector: 'app-listclient',
  templateUrl: './listclient.component.html'
})
export class ListclientComponent implements OnInit,ActionTableService<ClientEntity>,ActionModalConfirmService{

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  
  responsePageSearch : ResponsePageSearch<ClientEntity> = new ResponsePageSearch();
  
  dataTablaGenetic : DataTablaGeneticDto<ClientEntity> = new DataTablaGeneticDto();

  public constructor(private clientService : ClientService)
  {

  }

  
  actionModal(ModalId: string): void {
  
    console.log(ModalId);

  }
  filter(Page: number): void {
    this.findAll(Page,this.txtSearch.nativeElement.value);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<ClientEntity>): void {
    
    const data : DataTablaGeneticDto<ClientEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "ClientCod" } ,
        { Name :  "Documento identidad" , key : "DocumentNum" } ,
        { Name :  "Nombres" , key : "Names"} ,
        { Name :  "Modificación", key : "ModifyDate" , IsDate : true },
        { Name :  "Estado" , 
          key : "Status" , 
          IsStatus : true,
          Html : {
            Activo : 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            Inactivo : 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          }
        },
        { Name :  "Opciones" , 
          ColumnAction : true , 
          Id : ["ClientCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-pencil-alt" , Url : "/enterprise/client/pages/createclient?ClientCod={ClientCod}" },
            { Type : "Url" , Name : "fa fa-trash-alt" , Url : "#" },
            { Type : "Url" , Name : "fa fa-check" , Url : "#" }
          ] 
        }
      ],
      {
        data : responsePageSearch
      },
      "Lista de clientes"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
  
    let search : SearchDto = new SearchDto();
    search.Page = Page;
    search.Query = Query;

    const rpt : ResponseWsDto = await this.clientService.findAll(search);

    if( !rpt.ErrorStatus )
    {
      this.responsePageSearch = rpt.Data;  

      if( this.responsePageSearch.resultSearch != null && this.responsePageSearch.resultSearch.length > 0 )
      {
        for(let Item of this.responsePageSearch.resultSearch)
        {
          (Item as any).DocumentNum = Item.Person.DocumentNum;
          (Item as any).Names = Item.Person.PersonType === "04"
            ? Item.Person.BusinessName
            : Item.Person.Names + " " + Item.Person.LastNames;
        }

        this.loadingTable(this.responsePageSearch);
      }
    }

  }
  getDataRow(item: any): void {
  }
  ngOnInit(): void {
    this.findAll(1,"");
  }

}
