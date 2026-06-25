import { Injectable } from "@angular/core";
import { DataTablaGeneticDto } from "../model/dto/DataTablaGeneticDto";
import { ActionTableService } from "../interface/ActionTableService";

@Injectable({
    providedIn: 'root'
})

export class TableService<T>
{
    dataTablaGenetic : DataTablaGeneticDto<T> = new DataTablaGeneticDto();

    actionTableService? : ActionTableService<T>;

    funcParam : any;

    constructor() {
    }

    public setActions(actionTableService : ActionTableService<T>)
    {
        this.actionTableService = actionTableService;
    }

    public get(): DataTablaGeneticDto<T> {
        return this.dataTablaGenetic;
    }

    public set(dataTablaGenetic: DataTablaGeneticDto<T>): void {
        this.dataTablaGenetic = new DataTablaGeneticDto();
        this.dataTablaGenetic = dataTablaGenetic;
    }
 
}