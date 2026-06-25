import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { AppUserService } from '../../service/appuser.service';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { AppUserEntity } from '../../model/entity/AppUserEntity';

@Component({
  selector: 'app-listuser',
  templateUrl: './listuser.component.html'
})
export class ListuserComponent implements OnInit,ActionTableService<AppUserEntity>,ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  
  responsePageSearch : ResponsePageSearch<AppUserEntity> = new ResponsePageSearch();
  
  dataTablaGenetic : DataTablaGeneticDto<AppUserEntity> = new DataTablaGeneticDto();
  
  constructor(
    private appUserService : AppUserService
  ) 
  { 

  }

  actionModal(ModalId: string): void {
    
  }

  filter(Page: number): void {
    const Query = (this.txtSearch?.nativeElement?.value) ? this.txtSearch.nativeElement.value : "";
    this.findAll(Page,Query);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<AppUserEntity>): void {

    let data : DataTablaGeneticDto<AppUserEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "UserCod" } ,
        { Name :  "Nombres" , key : "Names" } ,
        { Name :  "Email" , key : "Email"} ,
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
          Id : ["UserCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-pencil-alt" , Url : "/enterprise/user/pages/createuser?UserCod={UserCod}" },
            { Type : "Url" , Name : "fa fa-trash-alt" , Url : "#" },
            { Type : "Url" , Name : "fa fa-check" , Url : "#" }
          ] 
        }
      ],
      {
        data : responsePageSearch
      },
      "Lista de menus del sistema"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    
    let search : SearchDto = new SearchDto();
      search.Page = Page;
      search.Query = Query;

      const rpt : ResponseWsDto = await this.appUserService.findAll(search);

      if( !rpt.ErrorStatus )
      {
          this.responsePageSearch = rpt.Data;  

          if(  this.responsePageSearch.resultSearch != null && this.responsePageSearch.resultSearch.length > 0 )
          {
              const resultSearch : AppUserEntity[] = this.responsePageSearch.resultSearch;
              const resultSearchFormat : any[] = [];

              for(let element of resultSearch)
              {
                element.Status = (element.Status === "A") ? "Activo" : "Inactivo";

                let elementFormat : any = element;

                elementFormat.Names = element.Person.Names + " " + element.Person.LastNames;

                resultSearchFormat.push(elementFormat);
              }

              this.responsePageSearch.resultSearch = resultSearchFormat;
          }
          
          this.loadingTable(this.responsePageSearch);
      }
  }
  getDataRow(item: any): void {

  }

  ngOnInit(): void {
    this.findAll(1,"");
  }

}
