import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { PucharseHeadEntity } from '../../model/entity/PucharseHeadEntity';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { PucharseService } from '../../service/PucharseService';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';

@Component({
  selector: 'app-listreception',
  templateUrl: './listreception.component.html'
})
export class ListreceptionComponent implements OnInit, ActionTableService<PucharseHeadEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<PucharseHeadEntity> = new ResponsePageSearch();

  dataTablaGenetic: DataTablaGeneticDto<PucharseHeadEntity> = new DataTablaGeneticDto();

  PucharseHeadSelect: PucharseHeadEntity = new PucharseHeadEntity();

  constructor(
    private pucharseService: PucharseService,
    private session: DataSesionService
  ) {

  }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  actionModal(ModalId: string): void {

  }

  filter(Page: number): void {
    const Query: string = this.txtSearch.nativeElement.value;

    this.findAll(Page, Query);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<PucharseHeadEntity>): void {

    const data: DataTablaGeneticDto<PucharseHeadEntity> = new DataTablaGeneticDto();

    const showConfirmPucharse = (PucharseHead: PucharseHeadEntity) => {
      return (PucharseHead.PurchaseStatus !== "F");
    }

    data.init(
      [
        { Name: "Codigo", key: "PucharseCod" },
        { Name: "Monto total", key: "NumTotalPrice", IsMoney: true },
        { Name: "Proveedor", key: "DealerCod" },
        { Name: "Modificación", key: "ModifyDate", IsDate: true },
        {
          Name: "Estado",
          key: "PurchaseStatus",
          IsStatus: true,
          Html: {
            F: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            P: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          },
          Mask: {
            F: "Finalizado",
            P: "Pendiente"
          }
        },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["PucharseCod"],
          Options: [
            { Type: "Url", Name: "fa fa-search", Url: "/enterprise/pucharse/pages/viewpucharse?PucharseCod={PucharseCod}" },
            { Type: "Url", Name: "fa fa-shopping-cart", Url: "/enterprise/pucharse/pages/confirmpucharse?PucharseCod={PucharseCod}", Function: showConfirmPucharse }
          ]
        }
      ],
      {
        data: responsePageSearch
      },
      "Lista de solicitudes de compra"
    );

    this.dataTablaGenetic = data;

  }

  async findAll(Page: number, Query: string): Promise<void> {
    const Search: SearchDto = new SearchDto();
    Search.Page = Page;
    Search.Query = Query;
    Search.StoreCod = this.session.getSessionStorageDto().StoreCod;

    const rpt: ResponseWsDto = await this.pucharseService.FindAll(Search);
    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.PucharseHeadSelect = item;
  }


}
