import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { BrandService } from '../../service/brand.service';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { BrandEntity } from '../../model/entity/BrandEntity';

@Component({
  selector: 'app-listbrand',
  templateUrl: './listbrand.component.html'
})
export class ListbrandComponent implements OnInit,ActionTableService<BrandEntity>,ActionModalConfirmService{

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  responsePageSearch : ResponsePageSearch<BrandEntity> = new ResponsePageSearch();
  dataTablaGenetic : DataTablaGeneticDto<BrandEntity> = new DataTablaGeneticDto();

  constructor(
    private brandService : BrandService
  )
  {

  }

  actionModal(ModalId: string): void {
    throw new Error('Method not implemented.');
  }
  filter(Page: number): void {
    this.findAll(Page,this.txtSearch.nativeElement.value);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<BrandEntity>): void {
    const data : DataTablaGeneticDto<BrandEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "BrandCod" } ,
        { Name :  "Descripción" , key : "BrandName"} ,
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
          Id : ["BrandCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-pencil-alt" , Url : "/enterprise/product/pages/createBrand?BrandCod={BrandCod}" },
            { Type : "Url" , Name : "fa fa-trash-alt" , Url : "#" },
            { Type : "Url" , Name : "fa fa-check" , Url : "#" }
          ] 
        }
      ],
      {
        data : responsePageSearch
      },
      "Lista de marcas"
    );

    this.dataTablaGenetic = data;
  }
  async findAll(Page: number, Query: string): Promise<void> {

    const rpt = await this.brandService.FindAll(Query,Page);

    if(!rpt.ErrorStatus)
    {
      this.responsePageSearch = rpt.Data;

      if(  this.responsePageSearch.resultSearch != null && this.responsePageSearch.resultSearch.length > 0 )
          {
              const resultSearch : BrandEntity[] = this.responsePageSearch.resultSearch;

              for(let element of resultSearch)
              {
                element.Status = (element.Status === "A") ? "Activo" : "Inactivo";
              }
          }

      this.loadingTable(this.responsePageSearch);
    }
  }
  getDataRow(item: any): void {
    console.log(item);
  }
  ngOnInit(): void {
    this.findAll(1,"");
  }

}
