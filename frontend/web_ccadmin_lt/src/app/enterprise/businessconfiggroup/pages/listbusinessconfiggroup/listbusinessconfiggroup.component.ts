import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { BusinessConfigGroupEntity } from '../../model/entity/BusinessConfigGroupEntity';
import { BusinessConfigGroupService } from '../../service/BusinessConfigGroupService';

@Component({
  selector: 'app-listbusinessconfiggroup',
  templateUrl: './listbusinessconfiggroup.component.html'
})
export class ListbusinessconfiggroupComponent implements OnInit, ActionTableService<BusinessConfigGroupEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<BusinessConfigGroupEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<BusinessConfigGroupEntity> = new DataTablaGeneticDto();
  businessConfigGroupSelect: BusinessConfigGroupEntity = new BusinessConfigGroupEntity();

  constructor(
    private businessConfigGroupService: BusinessConfigGroupService,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<BusinessConfigGroupEntity>): void {
    const data: DataTablaGeneticDto<BusinessConfigGroupEntity> = new DataTablaGeneticDto();

    const showEnable = (item: BusinessConfigGroupEntity) => item.Status !== "A";
    const showDisable = (item: BusinessConfigGroupEntity) => item.Status === "A";

    data.init(
      [
        { Name: "Id", key: "GroupId" },
        { Name: "Codigo", key: "GroupCod" },
        { Name: "Nombre", key: "GroupName" },
        { Name: "Descripcion", key: "GroupDesc" },
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
          Id: ["GroupCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/businessconfiggroup/pages/createbusinessconfiggroup?GroupCod={GroupCod}" },
            { Type: "Url", Name: "fa fa-cogs", Url: "/enterprise/businessconfiggroup/pages/createbusinessconfig?GroupCod={GroupCod}" },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable_business_config_group", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable_business_config_group", Function: showDisable }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de grupos de configuracion"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.businessConfigGroupService.findAll(Query, Page);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.businessConfigGroupSelect = item;
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_enable_business_config_group") this.enable();
    if (ModalId === "modal_disable_business_config_group") this.disable();
  }

  private async enable(): Promise<void> {
    const rpt: ResponseWsDto = await this.businessConfigGroupService.enable(this.businessConfigGroupSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Grupo habilitado");
      this.filter(1);
    }
  }

  private async disable(): Promise<void> {
    const rpt: ResponseWsDto = await this.businessConfigGroupService.disable(this.businessConfigGroupSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Grupo deshabilitado");
      this.filter(1);
    }
  }
}
