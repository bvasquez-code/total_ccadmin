import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { TableDto } from 'src/app/enterprise/shared/model/dto/TableDto';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';

import { CashSessionItemEntity } from '../../model/entity/CashSessionItemEntity';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { CashsessionService } from '../../service/CashsessionService';

@Component({
  selector: 'app-viewcashsession',
  templateUrl: './viewcashsession.component.html'
})
export class ViewcashsessionComponent implements OnInit, ActionTableService<CashSessionItemEntity> {

  CashSessionID: number = 0;

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  table: TableDto<CashSessionItemEntity> = new TableDto();

  constructor(
    private cashSessionService: CashsessionService,
    private router: Router,
    private toastrService: ToastrService
  ) {
    let urlTree: any = this.router.parseUrl(this.router.url);
    this.CashSessionID = Number(urlTree.queryParams['CashSessionID'] ?? 0);
  }
  findAll(Page: number, Query: string): Promise<void> {
    throw new Error('Method not implemented.');
  }

  ngOnInit(): void {
    this.findItems();
  }

  filter(_Page: number): void {
    // no hay paginado aquí, es listado por sesión
    this.findItems();
  }

  async findItems() {
    const rpt: ResponseWsDto = await this.cashSessionService.getItems(this.CashSessionID);

    if (!rpt.ErrorStatus) {
      // Para reutilizar tu tabla shared (aunque no haya paginado),
      // armamos un ResponsePageSearch "fake"
      const response: ResponsePageSearch<CashSessionItemEntity> = new ResponsePageSearch<CashSessionItemEntity>();
      response.resultSearch = rpt.Data ?? [];
      response.Page = 1;
      response.TotalPages = 1;
      response.TotalResult = response.resultSearch.length;

      this.table.responsePageSearch = response;
      this.loadingTable(this.table.responsePageSearch);
    }
  }

  loadingTable(responsePageSearch: ResponsePageSearch<CashSessionItemEntity>): void {
    const data: DataTablaGeneticDto<CashSessionItemEntity> = new DataTablaGeneticDto();

    const viewType = (i: CashSessionItemEntity) => i.ItemType;
    const viewDenom = (i: CashSessionItemEntity) => i.Denomination;
    const viewQty = (i: CashSessionItemEntity) => i.Qty;
    const viewPay = (i: CashSessionItemEntity) => i.PaymentMethodCod;
    const viewMov = (i: CashSessionItemEntity) => i.MovementType;
    const viewRef = (i: CashSessionItemEntity) => i.ReferenceCod;
    const viewAmount = (i: CashSessionItemEntity) => i.Amount;
    const viewCurr = (i: CashSessionItemEntity) => i.CurrencyCod;

    data.init(
      [
        { Name: "Tipo", key: "ItemType", FunctionKey: viewType },
        { Name: "Denom", key: "Denomination", FunctionKey: viewDenom },
        { Name: "Qty", key: "Qty", FunctionKey: viewQty },
        { Name: "Pago", key: "PaymentMethodCod", FunctionKey: viewPay },
        { Name: "Mov", key: "MovementType", FunctionKey: viewMov },
        { Name: "Ref", key: "ReferenceCod", FunctionKey: viewRef },
        { Name: "Monto", key: "Amount", FunctionKey: viewAmount },
        { Name: "Moneda", key: "CurrencyCod", FunctionKey: viewCurr },
      ],
      { data: responsePageSearch },
      "Items de sesión"
    );

    this.table.dataTablaGenetic = data;
  }

  getDataRow(item: any): void {
    this.table.itemTableSelect = item;
  }

  goClose() {
    this.router.navigate(["enterprise/cash/pages/closecashsession"], { queryParams: { CashSessionID: this.CashSessionID } });
  }
}
