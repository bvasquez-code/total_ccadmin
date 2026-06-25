import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CreditNoteHeadDto } from '../../model/dto/CreditNoteHeadDto';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { TableDto } from 'src/app/enterprise/shared/model/dto/TableDto';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { CreditNoteService } from '../../service/CreditNote.service';
import { SearchDto } from 'src/app/enterprise/shared/model/dto/SearchDto';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { DataSesionService } from 'src/app/enterprise/compartido/service/datasesion.service';
import { CreditNoteRegisterDto } from '../../model/dto/CreditNoteRegisterDto';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-listcreditnote',
  templateUrl: './listcreditnote.component.html'
})
export class ListcreditnoteComponent implements OnInit, ActionTableService<CreditNoteHeadDto>, ActionModalConfirmService {


  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  table: TableDto<CreditNoteHeadDto> = new TableDto();

  constructor(
    private creditNoteService: CreditNoteService,
    private session: DataSesionService,
    private toastrService: ToastrService
  ) {

  }

  ngOnInit(): void {
    this.findAll(1, "");
  }


  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<CreditNoteHeadDto>): void {

    const data: DataTablaGeneticDto<CreditNoteHeadDto> = new DataTablaGeneticDto();

    const viewCreditNoteCod = (creditNoteHead: CreditNoteHeadDto) => {
      return creditNoteHead.CreditNoteHead.CreditNoteCod;
    }

    const viewCreationDate = (creditNoteHead: CreditNoteHeadDto) => {
      return creditNoteHead.CreditNoteHead.CreationDate;
    }

    const viewDocumentCod = (creditNoteHead: CreditNoteHeadDto) => {
      return creditNoteHead?.CreditNoteDocument?.DocumentCod;
    }

    const viewCreditNoteStatus = (creditNoteHead: CreditNoteHeadDto) => {
      return creditNoteHead.CreditNoteHead.CreditNoteStatus;
    }

    const viewCreationUser = (creditNoteHead: CreditNoteHeadDto) => {
      return creditNoteHead.CreditNoteHead.CreationUser;
    }

    const urlCreatecreditnote = (creditNoteHead: CreditNoteHeadDto) => {
      return `/enterprise/sale/pages/createcreditnote?CreditNoteCod=${creditNoteHead.CreditNoteHead.CreditNoteCod}`;
    }

    const urlReturnStockCreditnote = (creditNoteHead: CreditNoteHeadDto) => {
      return `/enterprise/sale/pages/returnstockcreditnote?CreditNoteCod=${creditNoteHead.CreditNoteHead.CreditNoteCod}`;
    }

    const urlViewCreditNote = (creditNoteHead: CreditNoteHeadDto) => {
      return `/enterprise/sale/pages/viewcreditnote?CreditNoteCod=${creditNoteHead.CreditNoteHead.CreditNoteCod}`;
    }

    const showEditCreditNote = (creditNoteHead: CreditNoteHeadDto) => {
      return (creditNoteHead.CreditNoteHead.CreditNoteStatus === "P");
    }

    const showConfirmCreditNote = (creditNoteHead: CreditNoteHeadDto) => {
      return (creditNoteHead.CreditNoteHead.CreditNoteStatus === "P");
    }

    const showReturnStock = (creditNoteHead: CreditNoteHeadDto) => {
      return (creditNoteHead.CreditNoteHead.CreditNoteStatus === "C" && creditNoteHead.CreditNoteHead.IsStockReturned !== 'S');
    }

    const showDelete = (creditNoteHead: CreditNoteHeadDto) => {
      return (creditNoteHead.CreditNoteHead.CreditNoteStatus === "P");
    }

    const showViewCreditNote = (creditNoteHead: CreditNoteHeadDto) => {
      return (creditNoteHead.CreditNoteHead.CreditNoteStatus === "C");
    }


    data.init(
      [
        { Name: "Codigo", key: "CreditNoteCod", FunctionKey: viewCreditNoteCod },
        { Name: "Nota de credito", key: "DocumentCod", FunctionKey: viewDocumentCod },
        { Name: "Vendedor", key: "CreationUser", FunctionKey: viewCreationUser },
        { Name: "Fecha de venta", key: "CreationDate", IsDate: true, FunctionKey: viewCreationDate },
        {
          Name: "Estado",
          key: "CreditNoteStatus",
          IsStatus: true,
          Html: {
            P: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            C: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          },
          Mask: {
            P: "Pendiente",
            C: "Confirmado"
          },
          FunctionKey: viewCreditNoteStatus
        },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["CreditNoteCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "#", FunctionUrl: urlCreatecreditnote, Function: showEditCreditNote },
            { Type: "Url", Name: "fa fa-trash-alt", Url: "#", Function: showDelete },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_confirm", Function: showConfirmCreditNote },
            { Type: "Url", Name: "fa fa-share", Url: "#", FunctionUrl: urlReturnStockCreditnote, Function: showReturnStock },
            { Type: "Url", Name: "fa fa-search", Url: "#", FunctionUrl: urlViewCreditNote, Function: showViewCreditNote },
          ]
        }
      ],
      {
        data: responsePageSearch
      },
      "Lista de solicitudes de venta"
    );

    this.table.dataTablaGenetic = data;

  }

  async findAll(Page: number, Query: string): Promise<void> {
    const Search: SearchDto = new SearchDto();
    Search.Page = Page;
    Search.Query = Query;
    Search.StoreCod = this.session.getSessionStorageDto().StoreCod;

    const rpt: ResponseWsDto = await this.creditNoteService.FindAll(Search);
    if (!rpt.ErrorStatus) {
      this.table.responsePageSearch = rpt.Data;
      this.loadingTable(this.table.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.table.itemTableSelect = item;
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_confirm") this.Confirm();
  }

  async Confirm() {

    console.log(this.table.itemTableSelect);

    const CreditNoteRegister: CreditNoteRegisterDto = new CreditNoteRegisterDto();

    if (this.table.itemTableSelect) {
      CreditNoteRegister.Headboard = this.table.itemTableSelect?.CreditNoteHead;
    }

    const rpt: ResponseWsDto = await this.creditNoteService.Confirm(CreditNoteRegister);

    if (!rpt.ErrorStatus) {
      this.toastrService.success("Nota de credito confirmada");
      this.findAll(1, "");
    }

  }


}
