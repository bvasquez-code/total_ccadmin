import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { TrxPaymentEntity } from '../../model/entity/TrxPaymentEntity';
import { TrxPaymentService } from '../../service/TrxPaymentService';

@Component({
  selector: 'app-listtrxpayment',
  templateUrl: './listtrxpayment.component.html'
})
export class ListtrxpaymentComponent implements OnInit, ActionTableService<TrxPaymentEntity> {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<TrxPaymentEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<TrxPaymentEntity> = new DataTablaGeneticDto();

  constructor(
    private trxPaymentService: TrxPaymentService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<TrxPaymentEntity>): void {
    const data: DataTablaGeneticDto<TrxPaymentEntity> = new DataTablaGeneticDto();

    const viewMovement = (trxPayment: TrxPaymentEntity) => trxPayment.TypeMovement;
    const urlView = (trxPayment: TrxPaymentEntity) => `/enterprise/trxpayment/pages/viewtrxpayment?TrxPaymentId=${trxPayment.TrxPaymentId}`;

    data.init(
      [
        { Name: "Codigo", key: "TrxPaymentId" },
        { Name: "Transaccion", key: "TransactionId" },
        { Name: "Metodo", key: "PaymentMethodCod" },
        { Name: "Plataforma", key: "PaymentPlatform" },
        { Name: "Moneda", key: "CurrencyCod" },
        { Name: "Monto pagado", key: "AmountPaid", IsMoney: true },
        {
          Name: "Movimiento",
          key: "TypeMovement",
          IsStatus: true,
          Html: {
            I: 'badge badge-sm bgc-green-d1 text-white pb-1 px-25',
            E: 'badge badge-sm bgc-orange-d1 text-white pb-1 px-25'
          },
          Mask: {
            I: "Ingreso",
            E: "Egreso"
          },
          FunctionKey: viewMovement
        },
        {
          Name: "Estado",
          key: "PaymentStatus",
          IsStatus: true,
          Html: {
            OK: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            P: 'badge badge-sm bgc-orange-d1 text-white pb-1 px-25',
            F: 'badge badge-sm bgc-red-d1 text-white pb-1 px-25'
          },
          Mask: {
            OK: "OK",
            P: "Pendiente",
            F: "Fallido"
          }
        },
        { Name: "Fecha", key: "CreationDate", IsDate: true },
        {
          Name: "Opciones",
          ColumnAction: true,
          Id: ["TrxPaymentId"],
          Options: [
            { Type: "Url", Name: "fa fa-search", Url: "#", FunctionUrl: urlView }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de pagos"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.trxPaymentService.findAll(Query, Page);
    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
  }
}
