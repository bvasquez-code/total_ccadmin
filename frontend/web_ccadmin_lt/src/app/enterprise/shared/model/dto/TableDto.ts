import { DataTablaGeneticDto } from "./DataTablaGeneticDto";
import { ResponsePageSearch } from "./ResponsePageSearch";

export class TableDto <T>{

    public responsePageSearch : ResponsePageSearch<T>;

    public dataTablaGenetic : DataTablaGeneticDto<T>;
  
    public itemTableSelect : T | undefined;

    constructor(){
        this.responsePageSearch  = new ResponsePageSearch();
        this.dataTablaGenetic = new DataTablaGeneticDto();
    }
}
