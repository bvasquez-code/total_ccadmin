import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { AppProfileService } from '../../service/appprofile.service';
import { AppProfileEntity } from '../../model/entity/AppProfileEntity';

@Component({
  selector: 'app-listprofile',
  templateUrl: './listprofile.component.html'
})
export class ListprofileComponent implements OnInit,ActionTableService<AppProfileEntity>,ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  
  responsePageSearch : ResponsePageSearch<AppProfileEntity> = new ResponsePageSearch();
  
  dataTablaGenetic : DataTablaGeneticDto<AppProfileEntity> = new DataTablaGeneticDto();
  
  constructor(private appProfileService : AppProfileService) { }

  actionModal(ModalId: string): void {

  }
  filter(Page: number): void {
    const Query = (this.txtSearch?.nativeElement?.value) ? this.txtSearch.nativeElement.value : "";
    this.findAll(Page,Query);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<AppProfileEntity>): void {

    const data : DataTablaGeneticDto<AppProfileEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "ProfileCod" } ,
        { Name :  "Nombre" , key : "Name" } ,
        { Name :  "Descripción" , key : "Description"} ,
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
          Id : ["ProfileCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-pencil-alt" , Url : "/enterprise/user/pages/createprofile?ProfileCod={ProfileCod}" },
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

      const rpt : ResponseWsDto = await this.appProfileService.findAll(search);

      if( !rpt.ErrorStatus )
      {
          this.responsePageSearch = rpt.Data;  
          
          if(  this.responsePageSearch.resultSearch != null && this.responsePageSearch.resultSearch.length > 0 )
          {
            const resultSearch : AppProfileEntity[] = this.responsePageSearch.resultSearch;

            for(let element of resultSearch)
            {
              element.Status = (element.Status === "A") ? "Activo" : "Inactivo";
            }

            this.responsePageSearch.resultSearch = resultSearch;

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
