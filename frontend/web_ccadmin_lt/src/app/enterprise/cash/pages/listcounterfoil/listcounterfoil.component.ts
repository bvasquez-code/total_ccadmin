import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';

import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { TableDto } from 'src/app/enterprise/shared/model/dto/TableDto';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';

import { CounterfoilEntity } from '../../model/entity/CounterfoilEntity';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CounterfoilService } from '../../service/CounterfoilService';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';

@Component({
  selector: 'app-listcounterfoil',
  templateUrl: './listcounterfoil.component.html'
})
export class ListcounterfoilComponent implements OnInit, ActionTableService<CounterfoilEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  table: TableDto<CounterfoilEntity> = new TableDto();

  constructor(
    private counterfoilService: CounterfoilService,
    private toastrService: ToastrService,
    private sessionService: DataSesionService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<CounterfoilEntity>): void {

    const data: DataTablaGeneticDto<CounterfoilEntity> = new DataTablaGeneticDto();

    const viewCod = (e: CounterfoilEntity) => e.CounterfoilCod;
    const viewDoc = (e: CounterfoilEntity) => e.DocumentType;
    const viewSeries = (e: CounterfoilEntity) => e.Series;
    const viewCorr = (e: CounterfoilEntity) => e.Correlative;
    const viewAuto = (e: CounterfoilEntity) => e.IsAutomatic;
    const viewStatus = (e: CounterfoilEntity) => e.Status;

    const urlEdit = (e: CounterfoilEntity) => `/enterprise/cash/pages/createcounterfoil?CounterfoilCod=${e.CounterfoilCod}`;

    const showEnable = (e: CounterfoilEntity) => (e.Status !== 'A');
    const showDisable = (e: CounterfoilEntity) => (e.Status === 'A');

    data.init(
      [
        { Name: "Código", key: "CounterfoilCod", FunctionKey: viewCod },
        { Name: "Tipo Doc", key: "DocumentType", FunctionKey: viewDoc },
        { Name: "Serie", key: "Series", FunctionKey: viewSeries },
        { Name: "Correlativo", key: "Correlative", FunctionKey: viewCorr },
        {
          Name: "Automático",
          key: "IsAutomatic",
          IsStatus: true,
          Html: {
            S: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            N: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          },
          Mask: {
            S: "Sí",
            N: "No"
          },
          FunctionKey: viewAuto
        },
        {
          Name: "Estado",
          key: "Status",
          IsStatus: true,
          Html: {
            A: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            I: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          },
          Mask: {
            A: "Activo",
            I: "Inactivo"
          },
          FunctionKey: viewStatus
        },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["CounterfoilCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "#", FunctionUrl: urlEdit, Function: (_) => true },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable", Function: showDisable },
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de Counterfoil"
    );

    this.table.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.counterfoilService.findAll(
      Query,
      Page,
      this.sessionService.getSessionStorageDto().StoreCod
    );

    if (!rpt.ErrorStatus) {
      this.table.responsePageSearch = rpt.Data;
      this.loadingTable(this.table.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.table.itemTableSelect = item;
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_enable") this.enable();
    if (ModalId === "modal_disable") this.disable();
  }

  private async enable() {
    const row: CounterfoilEntity | any = this.table.itemTableSelect;
    if (!row) return;

    const rpt: ResponseWsDto = await this.counterfoilService.enable(row.CounterfoilCod);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Counterfoil habilitado");
      this.findAll(1, "");
    }
  }

  private async disable() {
    const row: CounterfoilEntity | any = this.table.itemTableSelect;
    if (!row) return;

    const rpt: ResponseWsDto = await this.counterfoilService.disable(row.CounterfoilCod);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Counterfoil deshabilitado");
      this.findAll(1, "");
    }
  }
}
