import { ResponsePageSearch } from "../model/dto/ResponsePageSearch";

export interface ActionTableService<T>
{
    filter(Page : number) : void;

    loadingTable(responsePageSearch : ResponsePageSearch<T>):void;

    findAll(Page : number,Query : string): Promise<void>;

    getDataRow(item : any) :void;
}