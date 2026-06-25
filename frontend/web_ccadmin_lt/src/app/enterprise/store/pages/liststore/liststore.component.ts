import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { StoreEntity } from 'src/app/enterprise/shared/model/entity/StoreEntity';
import { StoreService } from '../../service/store.service';

@Component({
  selector: 'app-liststore',
  templateUrl: './liststore.component.html'
})
export class ListstoreComponent implements OnInit, ActionTableService<StoreEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<StoreEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<StoreEntity> = new DataTablaGeneticDto();

  constructor(
    private storeService: StoreService
  ) {
  }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  actionModal(ModalId: string): void {
    console.log(ModalId);
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<StoreEntity>): void {
    const data: DataTablaGeneticDto<StoreEntity> = new DataTablaGeneticDto();

    data.init(
      [
        { Name: "Codigo", key: "StoreCod" },
        { Name: "Nombre", key: "Name" },
        { Name: "Descripcion", key: "Description" },
        { Name: "Direccion", key: "Address" },
        { Name: "Ubigeo", key: "UbigeoCod" },
        { Name: "Modificacion", key: "ModifyDate", IsDate: true },
        {
          Name: "Estado",
          key: "Status",
          IsStatus: true,
          Html: {
            Activo: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            Inactivo: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          }
        },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["StoreCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/store/pages/createstore?StoreCod={StoreCod}" }
          ]
        }
      ],
      {
        data: responsePageSearch
      },
      "Lista de tiendas"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.storeService.FindAll(Query, Page);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;

      if (this.responsePageSearch.resultSearch != null && this.responsePageSearch.resultSearch.length > 0) {
        for (let element of this.responsePageSearch.resultSearch) {
          element.Status = (element.Status === "A") ? "Activo" : "Inactivo";
        }
      }

      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    console.log(item);
  }

}
