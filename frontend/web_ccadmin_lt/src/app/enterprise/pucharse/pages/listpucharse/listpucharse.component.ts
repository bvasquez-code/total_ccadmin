import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { PucharseRequestHeadService } from '../../service/PucharseRequestHeadService';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { PucharseRequestHeadEntity } from '../../model/entity/PucharseRequestHeadEntity';
import { PucharseService } from '../../service/PucharseService';
import { PucharseRegisterDto } from '../../model/dto/PucharseRegisterDto';

@Component({
  selector: 'app-listpucharse',
  templateUrl: './listpucharse.component.html'
})
export class ListpucharseComponent implements OnInit, ActionTableService<PucharseRequestHeadEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<PucharseRequestHeadEntity> = new ResponsePageSearch();

  dataTablaGenetic: DataTablaGeneticDto<PucharseRequestHeadEntity> = new DataTablaGeneticDto();

  pucharseRequestHeadSelect: PucharseRequestHeadEntity = new PucharseRequestHeadEntity();

  constructor(
    private pucharseRequestHeadService: PucharseRequestHeadService,
    private pucharseService: PucharseService,
    private session: DataSesionService
  ) {

  }


  ngOnInit(): void {
    this.findAll(1, "");
  }
  filter(Page: number): void {

    const Query: string = this.txtSearch.nativeElement.value;

    this.findAll(Page, Query);
  }
  loadingTable(responsePageSearch: ResponsePageSearch<PucharseRequestHeadEntity>): void {

    const data: DataTablaGeneticDto<PucharseRequestHeadEntity> = new DataTablaGeneticDto();

    const showConfirmPucharse = (pucharseRequestHead: PucharseRequestHeadEntity) => {
      return (pucharseRequestHead.PurchaseStatus !== "F");
    }

    data.init(
      [
        { Name: "Codigo", key: "PucharseReqCod" },
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
          Id: ["PucharseReqCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/pucharse/pages/createpucharse?PucharseReqCod={PucharseReqCod}", Function: showConfirmPucharse },
            { Type: "Modal", Name: "fa fa-trash-alt", Url: "#", ID: "Eliminar", Function: showConfirmPucharse },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "Confirmar", Function: showConfirmPucharse }
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

    const rpt: ResponseWsDto = await this.pucharseRequestHeadService.FindAll(Search);
    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }

  }
  getDataRow(item: any): void {

    this.pucharseRequestHeadSelect = item;
    console.log(this.pucharseRequestHeadSelect);

  }
  actionModal(ModalId: string): void {
    console.log({ ModalId: ModalId });

    if (ModalId === "Confirmar") this.SavePucharse();

  }

  async SavePucharse() {
    const PucharseRegister: PucharseRegisterDto = new PucharseRegisterDto();

    PucharseRegister.PucharseReqCod = this.pucharseRequestHeadSelect.PucharseReqCod;

    const rpt: ResponseWsDto = await this.pucharseService.Save(PucharseRegister);

    if (!rpt.ErrorStatus) {
      this.filter(1);
    }
  }



}
