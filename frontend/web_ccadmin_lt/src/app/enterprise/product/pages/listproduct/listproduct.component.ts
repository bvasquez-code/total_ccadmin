import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ProductService } from '../../service/product.service';
import { ProductEntity } from '../../model/entity/ProductEntity';

@Component({
  selector: 'app-listproduct',
  templateUrl: './listproduct.component.html'
})
export class ListproductComponent implements OnInit,ActionTableService<ProductEntity>,ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  
  responsePageSearch : ResponsePageSearch<ProductEntity> = new ResponsePageSearch();

  dataTablaGenetic : DataTablaGeneticDto<ProductEntity> = new DataTablaGeneticDto();

  constructor(
    private productService : ProductService
  ) { }

  actionModal(ModalId: string): void {
    throw new Error('Method not implemented.');
  }

  filter(Page: number): void {
    const Query = (this.txtSearch?.nativeElement?.value) ? this.txtSearch.nativeElement.value : "";
    this.findAll(Page,Query);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<ProductEntity>): void {
    const data : DataTablaGeneticDto<ProductEntity> = new DataTablaGeneticDto();
    data.init(
      [
        { Name :  "Codigo" , key : "ProductCod" } ,
        { Name :  "Descripción" , key : "ProductName"} ,
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
          Id : ["ProductCod"] , 
          Options : [
            { Type : "Url" , Name : "fa fa-pencil-alt" , Url : "/enterprise/product/pages/createProduct?ProductCod={ProductCod}" },
            { Type : "Url" , Name : "fa fa-trash-alt" , Url : "#" },
            { Type : "Url" , Name : "fa fa-check" , Url : "#" }
          ] 
        }
      ],
      {
        data : responsePageSearch
      },
      "Lista de productos"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
  
    const rpt = await this.productService.FindAll(Query,Page);

    if(!rpt.ErrorStatus)
    {
      this.responsePageSearch = rpt.Data;

      if(  this.responsePageSearch.resultSearch != null && this.responsePageSearch.resultSearch.length > 0 )
          {
              const resultSearch : ProductEntity[] = this.responsePageSearch.resultSearch;

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
