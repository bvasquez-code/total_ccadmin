import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CurrencyEntity } from 'src/app/enterprise/shared/model/entity/CurrencyEntity';
import { CurrencyService } from '../../service/CurrencyService';

@Component({
  selector: 'app-listcurrency',
  templateUrl: './listcurrency.component.html'
})
export class ListcurrencyComponent implements OnInit, ActionTableService<CurrencyEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<CurrencyEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<CurrencyEntity> = new DataTablaGeneticDto();
  currencySelect: CurrencyEntity = new CurrencyEntity();

  constructor(
    private currencyService: CurrencyService,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<CurrencyEntity>): void {
    const data: DataTablaGeneticDto<CurrencyEntity> = new DataTablaGeneticDto();

    const showEnable = (item: CurrencyEntity) => item.Status !== "A";
    const showDisable = (item: CurrencyEntity) => item.Status === "A";

    data.init(
      [
        { Name: "Codigo", key: "CurrencyCod" },
        { Name: "Abrev.", key: "CurrencyAbbr" },
        { Name: "Simbolo", key: "CurrencySymbol" },
        { Name: "Nombre", key: "CurrencyName" },
        { Name: "Sistema", key: "IsCurrencySystem", FunctionKey: (item: CurrencyEntity) => this.formatYesNo(item.IsCurrencySystem) },
        { Name: "Cambio", key: "NumExchangevalue" },
        { Name: "Modificacion", key: "ModifyDate", IsDate: true },
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
          }
        },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["CurrencyCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/system/pages/createcurrency?CurrencyCod={CurrencyCod}" },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable_currency", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable_currency", Function: showDisable }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de monedas"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.currencyService.findAll(Query, Page);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.currencySelect = item;
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_enable_currency") this.enable();
    if (ModalId === "modal_disable_currency") this.disable();
  }

  private async enable(): Promise<void> {
    const rpt: ResponseWsDto = await this.currencyService.enable(this.currencySelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Moneda habilitada");
      this.filter(1);
    }
  }

  private async disable(): Promise<void> {
    const rpt: ResponseWsDto = await this.currencyService.disable(this.currencySelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Moneda deshabilitada");
      this.filter(1);
    }
  }

  private formatYesNo(value: string): string {
    return value === "Y" || value === "S" ? "Si" : "No";
  }
}
