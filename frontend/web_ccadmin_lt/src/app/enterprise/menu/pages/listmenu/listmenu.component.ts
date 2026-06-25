import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { AppMenuEntity } from '../../model/entity/AppMenuEntity';
import { AppMenuService } from '../../service/appmenu.service';

@Component({
  selector: 'app-listmenu',
  templateUrl: './listmenu.component.html'
})
export class ListmenuComponent implements OnInit, ActionTableService<AppMenuEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<AppMenuEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<AppMenuEntity> = new DataTablaGeneticDto();
  AppMenuSelectionClick: AppMenuEntity = new AppMenuEntity();

  constructor(
    private appMenuService: AppMenuService,
    private toastrService: ToastrService
  ) {
  }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  getDataRow(item: AppMenuEntity): void {
    this.AppMenuSelectionClick = item;
  }

  filter(Page: number): void {
    const Query = (this.txtSearch?.nativeElement?.value) ? this.txtSearch.nativeElement.value : "";
    this.findAll(Page, Query);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<AppMenuEntity>): void {
    const data: DataTablaGeneticDto<AppMenuEntity> = new DataTablaGeneticDto();
    const showEnable = (item: AppMenuEntity) => item.Status !== "A";
    const showDisable = (item: AppMenuEntity) => item.Status === "A";

    data.init(
      [
        { Name: "Codigo", key: "MenuCod" },
        { Name: "Descripcion", key: "Name" },
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
          Id: ["MenuCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/menu/pages/createmenu?MenuCod={MenuCod}" },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable_menu", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable_menu", Function: showDisable }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de menus del sistema"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const search: SearchDto = new SearchDto();
    search.Page = Page;
    search.Query = Query;

    const rpt: ResponseWsDto = await this.appMenuService.findAll(search);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_disable_menu") this.deactivateById();
    if (ModalId === "modal_enable_menu") this.activateById();
  }

  async deactivateById(): Promise<void> {
    this.AppMenuSelectionClick.Status = "I";
    const rpt: ResponseWsDto = await this.appMenuService.updateStatus(this.AppMenuSelectionClick);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Menu deshabilitado");
      this.filter(1);
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

  async activateById(): Promise<void> {
    this.AppMenuSelectionClick.Status = "A";
    const rpt: ResponseWsDto = await this.appMenuService.updateStatus(this.AppMenuSelectionClick);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Menu habilitado");
      this.filter(1);
    } else {
      this.toastrService.error(rpt.Message);
    }
  }

}
