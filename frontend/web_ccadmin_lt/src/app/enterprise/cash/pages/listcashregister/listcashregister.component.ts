import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';

import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { TableDto } from 'src/app/enterprise/shared/model/dto/TableDto';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';

import { CashRegisterEntity } from '../../model/entity/CashRegisterEntity';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CashregisterService } from '../../service/CashregisterService';

@Component({
  selector: 'app-listcashregister',
  templateUrl: './listcashregister.component.html'
})
export class ListcashregisterComponent implements OnInit, ActionTableService<CashRegisterEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  table: TableDto<CashRegisterEntity> = new TableDto();

  constructor(
    private cashRegisterService: CashregisterService,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<CashRegisterEntity>): void {

    const data: DataTablaGeneticDto<CashRegisterEntity> = new DataTablaGeneticDto();

    const viewRegisterCod = (r: CashRegisterEntity) => r.RegisterCod;
    const viewStoreCod = (r: CashRegisterEntity) => r.StoreCod;
    const viewName = (r: CashRegisterEntity) => r.Name;
    const viewSerial = (r: CashRegisterEntity) => r.SerialNumber;
    const viewStatus = (r: CashRegisterEntity) => r.Status;

    const urlCreate = (r: CashRegisterEntity) => `/enterprise/cash/pages/createcashregister?RegisterCod=${r.RegisterCod}`;
    const urlOpen = (r: CashRegisterEntity) => `/enterprise/cash/pages/opencashsession?RegisterCod=${r.RegisterCod}&StoreCod=${r.StoreCod}`;

    const showOpen = (r: CashRegisterEntity) => (r.Status === 'A');
    const showEnable = (r: CashRegisterEntity) => (r.Status !== 'A');
    const showDisable = (r: CashRegisterEntity) => (r.Status === 'A');

    data.init(
      [
        { Name: "Código", key: "RegisterCod", FunctionKey: viewRegisterCod },
        { Name: "Tienda", key: "StoreCod", FunctionKey: viewStoreCod },
        { Name: "Nombre", key: "Name", FunctionKey: viewName },
        { Name: "Serie", key: "SerialNumber", FunctionKey: viewSerial },
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
          Id: ["RegisterCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "#", FunctionUrl: urlCreate, Function: (_) => true },
            { Type: "Url", Name: "fa fa-door-open", Url: "#", FunctionUrl: urlOpen, Function: showOpen },

            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable", Function: showDisable },
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de Cajas"
    );

    this.table.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.cashRegisterService.findAll(Query, Page);
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
    const row: CashRegisterEntity | any = this.table.itemTableSelect;
    if (!row) return;

    const rpt: ResponseWsDto = await this.cashRegisterService.enable(row.RegisterCod);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Caja habilitada");
      this.findAll(1, "");
    }
  }

  private async disable() {
    const row: CashRegisterEntity | any = this.table.itemTableSelect;
    if (!row) return;

    const rpt: ResponseWsDto = await this.cashRegisterService.disable(row.RegisterCod);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Caja deshabilitada");
      this.findAll(1, "");
    }
  }
}
