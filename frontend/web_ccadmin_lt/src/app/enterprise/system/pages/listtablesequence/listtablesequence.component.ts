import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TableSequenceEntity } from 'src/app/enterprise/shared/model/entity/TableSequenceEntity';
import { TableSequenceService } from '../../service/TableSequenceService';

@Component({
  selector: 'app-listtablesequence',
  templateUrl: './listtablesequence.component.html'
})
export class ListtablesequenceComponent implements OnInit, ActionTableService<TableSequenceEntity> {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<TableSequenceEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<TableSequenceEntity> = new DataTablaGeneticDto();

  constructor(
    private tableSequenceService: TableSequenceService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<TableSequenceEntity>): void {
    const data: DataTablaGeneticDto<TableSequenceEntity> = new DataTablaGeneticDto();

    data.init(
      [
        { Name: "Tabla", key: "SequenceTableType" },
        { Name: "Secuencia", key: "SequenceTrx" },
        { Name: "Prefijo", key: "Prefix" },
        { Name: "Longitud", key: "length" },
        {
          Name: "Usa prefijo",
          key: "UsePrefix",
          IsStatus: true,
          Html: {
            S: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            N: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask: {
            S: "Si",
            N: "No"
          }
        },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["SequenceTrx"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/system/pages/createtablesequence?SequenceTrx={SequenceTrx}" }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de secuencias globales"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.tableSequenceService.findAll(Query, Page);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
  }
}
