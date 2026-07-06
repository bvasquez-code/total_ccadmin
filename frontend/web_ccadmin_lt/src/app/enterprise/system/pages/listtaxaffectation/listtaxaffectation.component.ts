import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TaxAffectationEntity } from '../../model/entity/TaxAffectationEntity';
import { TaxAffectationService } from '../../service/TaxAffectationService';

@Component({
  selector: 'app-listtaxaffectation',
  templateUrl: './listtaxaffectation.component.html'
})
export class ListtaxaffectationComponent implements OnInit, ActionTableService<TaxAffectationEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<TaxAffectationEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<TaxAffectationEntity> = new DataTablaGeneticDto();
  taxAffectationSelect: TaxAffectationEntity = new TaxAffectationEntity();

  constructor(
    private taxAffectationService: TaxAffectationService,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<TaxAffectationEntity>): void {
    const data: DataTablaGeneticDto<TaxAffectationEntity> = new DataTablaGeneticDto();
    const showEnable = (item: TaxAffectationEntity) => item.Status !== "A";
    const showDisable = (item: TaxAffectationEntity) => item.Status === "A";

    data.init(
      [
        { Name: "Codigo", key: "TaxAffectationCod" },
        { Name: "Nombre", key: "Name" },
        { Name: "Descripcion", key: "Description" },
        { Name: "Tributo", key: "TaxCod" },
        { Name: "Gravado", key: "IsTaxed", Mask: { S: "Si", N: "No" } },
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
          Id: ["TaxAffectationCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/system/pages/createtaxaffectation?TaxAffectationCod={TaxAffectationCod}" },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable_tax_affectation", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable_tax_affectation", Function: showDisable }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de afectaciones tributarias"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.taxAffectationService.findAll(Query, Page);
    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.taxAffectationSelect = item;
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_enable_tax_affectation") this.enable();
    if (ModalId === "modal_disable_tax_affectation") this.disable();
  }

  private async enable(): Promise<void> {
    const rpt: ResponseWsDto = await this.taxAffectationService.enable(this.taxAffectationSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Afectacion habilitada");
      this.filter(1);
    }
  }

  private async disable(): Promise<void> {
    const rpt: ResponseWsDto = await this.taxAffectationService.disable(this.taxAffectationSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Afectacion deshabilitada");
      this.filter(1);
    }
  }
}
