import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { SupplierService } from '../../service/supplier.service';
import { ResponseWsDto } from '../../../shared/model/dto/ResponseWsDto';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { SupplierEntity } from '../../model/entity/SupplierEntity';

@Component({
  selector: 'app-listsupplier',
  templateUrl: './listsupplier.component.html'
})
export class ListsupplierComponent implements OnInit,ActionTableService<SupplierEntity>,ActionModalConfirmService{

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  
  responsePageSearch : ResponsePageSearch<SupplierEntity> = new ResponsePageSearch();
  
  dataTablaGenetic : DataTablaGeneticDto<SupplierEntity> = new DataTablaGeneticDto();

  public constructor(private supplierService : SupplierService)
  {

  }

  
  actionModal(ModalId: string): void {
  
    console.log(ModalId);

  }
  filter(Page: number): void {
    this.findAll(Page,this.txtSearch.nativeElement.value);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<SupplierEntity>): void {
    
    const data : DataTablaGeneticDto<SupplierEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "SupplierCod" } ,
        { Name :  "Documento identidad" , key : "DocumentNum" } ,
        { Name :  "Nombres" , key : "Names"} ,
        { Name :  "Modificacion", key : "ModifyDate" , IsDate : true },
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
          Id : ["SupplierCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-pencil-alt" , Url : "/enterprise/supplier/pages/createsupplier?SupplierCod={SupplierCod}" },
            { Type : "Url" , Name : "fa fa-trash-alt" , Url : "#" },
            { Type : "Url" , Name : "fa fa-check" , Url : "#" }
          ] 
        }
      ],
      {
        data : responsePageSearch
      },
      "Lista de proveedores"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
  
    let search : SearchDto = new SearchDto();
    search.Page = Page;
    search.Query = Query;

    const rpt : ResponseWsDto = await this.supplierService.findAll(search);

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
