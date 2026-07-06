import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TaxEntity } from '../../model/entity/TaxEntity';
import { TaxService } from '../../service/TaxService';

@Component({
  selector: 'app-listtax',
  templateUrl: './listtax.component.html'
})
export class ListtaxComponent implements OnInit, ActionTableService<TaxEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<TaxEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<TaxEntity> = new DataTablaGeneticDto();
  taxSelect: TaxEntity = new TaxEntity();

  constructor(
    private taxService: TaxService,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<TaxEntity>): void {
    const data: DataTablaGeneticDto<TaxEntity> = new DataTablaGeneticDto();
    const showEnable = (item: TaxEntity) => item.Status !== "A";
    const showDisable = (item: TaxEntity) => item.Status === "A";

    data.init(
      [
        { Name: "Codigo", key: "TaxCod" },
        { Name: "SUNAT", key: "SunatTaxCod" },
        { Name: "Nombre", key: "Name" },
        { Name: "Tipo", key: "TaxCalculationType", Mask: { P: "Porcentaje", F: "Monto fijo", N: "No aplica" } },
        { Name: "Tasa", key: "TaxRateValue", IsMoney: true },
        { Name: "Monto fijo", key: "FixedUnitAmount", IsMoney: true },
        { Name: "Informativo", key: "IsInformative", Mask: { S: "Si", N: "No" } },
        { Name: "Orden", key: "CalculationOrder" },
        {
          Name: "Estado",
          key: "Status",
          IsStatus: true,
          Html: {
            A: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            I: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          },
          Mask: { A: "Activo", I: "Inactivo" }
        },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["TaxCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/system/pages/createtax?TaxCod={TaxCod}" },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable_tax", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable_tax", Function: showDisable }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de tributos"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.taxService.findAll(Query, Page);
    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.taxSelect = item;
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_enable_tax") this.enable();
    if (ModalId === "modal_disable_tax") this.disable();
  }

  private async enable(): Promise<void> {
    const rpt: ResponseWsDto = await this.taxService.enable(this.taxSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Tributo habilitado");
      this.filter(1);
    }
  }

  private async disable(): Promise<void> {
    const rpt: ResponseWsDto = await this.taxService.disable(this.taxSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Tributo deshabilitado");
      this.filter(1);
    }
  }
}
