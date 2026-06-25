import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { PaymentMethodEntity } from 'src/app/enterprise/shared/model/entity/PaymentMethodEntity';
import { PaymentMethodService } from '../../service/PaymentMethodService';

@Component({
  selector: 'app-listpaymentmethod',
  templateUrl: './listpaymentmethod.component.html'
})
export class ListpaymentmethodComponent implements OnInit, ActionTableService<PaymentMethodEntity>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<PaymentMethodEntity> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<PaymentMethodEntity> = new DataTablaGeneticDto();
  paymentMethodSelect: PaymentMethodEntity = new PaymentMethodEntity();

  constructor(
    private paymentMethodService: PaymentMethodService,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.findAll(1, "");
  }

  filter(Page: number): void {
    this.findAll(Page, this.txtSearch.nativeElement.value);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<PaymentMethodEntity>): void {
    const data: DataTablaGeneticDto<PaymentMethodEntity> = new DataTablaGeneticDto();

    const showEnable = (item: PaymentMethodEntity) => item.Status !== "A";
    const showDisable = (item: PaymentMethodEntity) => item.Status === "A";

    data.init(
      [
        { Name: "Codigo", key: "PaymentMethodCod" },
        { Name: "Nombre", key: "Name" },
        { Name: "Descripcion", key: "Description" },
        { Name: "Tipo", key: "PaymentMethodType" },
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
          Id: ["PaymentMethodCod"],
          Options: [
            { Type: "Url", Name: "fa fa-pencil-alt", Url: "/enterprise/system/pages/createpaymentmethod?PaymentMethodCod={PaymentMethodCod}" },
            { Type: "Modal", Name: "fa fa-check", Url: "#", ID: "modal_enable_payment_method", Function: showEnable },
            { Type: "Modal", Name: "fa fa-ban", Url: "#", ID: "modal_disable_payment_method", Function: showDisable }
          ]
        }
      ],
      { data: responsePageSearch },
      "Lista de metodos de pago"
    );

    this.dataTablaGenetic = data;
  }

  async findAll(Page: number, Query: string): Promise<void> {
    const rpt: ResponseWsDto = await this.paymentMethodService.findAll(Query, Page);

    if (!rpt.ErrorStatus) {
      this.responsePageSearch = rpt.Data;
      this.loadingTable(this.responsePageSearch);
    }
  }

  getDataRow(item: any): void {
    this.paymentMethodSelect = item;
  }

  actionModal(ModalId: string): void {
    if (ModalId === "modal_enable_payment_method") this.enable();
    if (ModalId === "modal_disable_payment_method") this.disable();
  }

  private async enable(): Promise<void> {
    const rpt: ResponseWsDto = await this.paymentMethodService.enable(this.paymentMethodSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Metodo de pago habilitado");
      this.filter(1);
    }
  }

  private async disable(): Promise<void> {
    const rpt: ResponseWsDto = await this.paymentMethodService.disable(this.paymentMethodSelect);
    if (!rpt.ErrorStatus) {
      this.toastrService.success("Metodo de pago deshabilitado");
      this.filter(1);
    }
  }
}
