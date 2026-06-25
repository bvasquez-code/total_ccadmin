import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { KardexDto } from '../../model/dto/KardexDto';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { KardexService } from '../../service/KardexService';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';

@Component({
  selector: 'app-listkardex',
  templateUrl: './listkardex.component.html'
})
export class ListkardexComponent implements OnInit,ActionTableService<KardexDto>,ActionModalConfirmService{

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;
  
  responsePageSearch : ResponsePageSearch<KardexDto> = new ResponsePageSearch();
  dataTablaGenetic : DataTablaGeneticDto<KardexDto> = new DataTablaGeneticDto();
  KardexSelect : KardexDto = new KardexDto();


  constructor(
    private kardexService : KardexService,
    private dataSesionService : DataSesionService,
  ){

  }

  ngOnInit(): void {
    this.findAll(1,"");
  }

  actionModal(ModalId: string): void {
    throw new Error('Method not implemented.');
  }
  filter(Page: number): void {
    this.findAll(Page,this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<KardexDto>): void {
    
    const data : DataTablaGeneticDto<KardexDto> = new DataTablaGeneticDto();

    const viewkardexID = (kardexDto : KardexDto) =>{
      return kardexDto.kardex.kardexID;
    }
    const viewProduct = (kardexDto : KardexDto) =>{
      return kardexDto.product.ProductCod +" - "+kardexDto.product.ProductName;
    }
    const viewCreationDate = (kardexDto : KardexDto) =>{
      return kardexDto.kardex.CreationDate;
    }
    const viewOperationCod = (kardexDto : KardexDto) =>{
      return kardexDto.kardex.OperationCod;
    }
    const viewNumStockMoved = (kardexDto : KardexDto) =>{

      let signo = (kardexDto.kardex.TypeOperation === "R") ? "-" : "+";

      return signo + kardexDto.kardex.NumStockMoved;
    }

    const viewNumStockBefore = (kardexDto : KardexDto) =>{

      return kardexDto.kardex.NumStockBefore;
    }
    const viewNumStockAfter = (kardexDto : KardexDto) =>{

      return kardexDto.kardex.NumStockAfter;
    }
    const viewTypeOperation = (kardexDto : KardexDto) =>{

      return kardexDto.dataTypeOperation.ConfigVal;
    }

    data.init(
      [
        { Name :  "Id" , key : "viewkardexID", FunctionKey : viewkardexID } ,
        { Name :  "Producto" , key : "viewProduct" , FunctionKey : viewProduct } ,
        { Name :  "Stock Anter" , key : "viewNumStockBefore" , FunctionKey : viewNumStockBefore } ,
        { Name :  "Movimiento Stock" , key : "viewNumStockMoved" , FunctionKey : viewNumStockMoved } ,
        { Name :  "Stock Resultante" , key : "NumStockAfter" , FunctionKey : viewNumStockAfter } ,
        { Name :  "Tipo de operación" , key : "viewTypeOperation" , FunctionKey : viewTypeOperation} ,
        { Name :  "Cod. Operación" , key : "viewOperationCod" , FunctionKey : viewOperationCod} ,
        { Name :  "Fecha de venta", key : "viewCreationDate" , FunctionKey : viewCreationDate , IsDate : true },
        { Name :  "Opciones" , 
          ColumnAction : true , 
          Id : ["Id"] , 
          Options : [
            { Type : "Modal" , Name : "fa fa-search", ID : "modal_operation"  }
          ] 
        }
      ],
      {
        data : responsePageSearch
      },
      "Movimientos de kardex"
    );

    this.dataTablaGenetic = data;

  }
  
  async findAll(Page: number, Query: string): Promise<void> {

    const search: SearchDto = new SearchDto();
    search.Page = Page;
    search.Query = Query;
    search.StoreCod = this.dataSesionService.getSessionStorageDto().StoreCod;

    const rpt = await this.kardexService.FindAll(search);

    if( !rpt.ErrorStatus )
    {
      this.responsePageSearch = rpt.Data;  

      this.loadingTable(this.responsePageSearch);
    }
  }
  getDataRow(item: any): void {
    console.log({ item : item });
  }
  

}
