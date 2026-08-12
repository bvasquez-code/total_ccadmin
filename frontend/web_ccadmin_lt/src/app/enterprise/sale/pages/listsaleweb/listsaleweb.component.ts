import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ActionModalConfirmService } from 'src/app/enterprise/shared/interface/ActionModalConfirmService';
import { ActionTableService } from 'src/app/enterprise/shared/interface/ActionTableService';
import { DataTablaGeneticDto } from 'src/app/enterprise/shared/model/dto/DataTablaGeneticDto';
import { ResponsePageSearch } from 'src/app/enterprise/shared/model/dto/ResponsePageSearch';
import { ResponseWsDto } from 'src/app/enterprise/shared/model/dto/ResponseWsDto';
import { PresaleCancellationDetailDto } from '../../model/dto/PresaleCancellationDetailDto';
import { SaleDeliveryStatusChangeDto } from '../../model/dto/SaleDeliveryStatusChangeDto';
import { SaleWebOrderDto } from '../../model/dto/SaleWebOrderDto';
import { PresaleService } from '../../service/presale.service';
import { SaleWebService } from '../../service/sale-web.service';

@Component({
  selector: 'app-listsaleweb',
  templateUrl: './listsaleweb.component.html'
})
export class ListsalewebComponent implements OnInit,
  ActionTableService<SaleWebOrderDto>, ActionModalConfirmService {

  @ViewChild('txtSearch') txtSearch!: ElementRef<HTMLInputElement>;

  responsePageSearch: ResponsePageSearch<SaleWebOrderDto> = new ResponsePageSearch();
  dataTablaGenetic: DataTablaGeneticDto<SaleWebOrderDto> = new DataTablaGeneticDto();
  selectedOrder: SaleWebOrderDto = new SaleWebOrderDto();
  deliveryTypeCod = '';
  deliveryStatus = '';
  failureCommenter = '';

  readonly pending = 'P';
  readonly preparing = 'R';
  readonly readyForPickup = 'L';
  readonly dispatched = 'D';
  readonly delivered = 'E';
  readonly failed = 'F';

  constructor(
    private saleWebService: SaleWebService,
    private presaleService: PresaleService,
    private router: Router,
    private toastrService: ToastrService
  ) {}

  ngOnInit(): void {
    this.findAll(1, '');
  }

  filter(page: number): void {
    this.findAll(page, this.txtSearch?.nativeElement?.value ?? '');
  }

  async findAll(page: number, query: string): Promise<void> {
    if (page < 1) return;
    const response = await this.saleWebService.findAll(
      query.trim(),
      page,
      this.deliveryTypeCod,
      this.deliveryStatus
    );
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message);
      return;
    }
    this.responsePageSearch = response.Data ?? new ResponsePageSearch<SaleWebOrderDto>();
    this.loadingTable(this.responsePageSearch);
  }

  loadingTable(responsePageSearch: ResponsePageSearch<SaleWebOrderDto>): void {
    const data = new DataTablaGeneticDto<SaleWebOrderDto>();
    data.init(
      [
        { Name: 'Pedido', key: 'SaleCod' },
        { Name: 'Cliente', key: 'ClientName' },
        {
          Name: 'Tipo de entrega',
          key: 'DeliveryTypeCod',
          FunctionKey: (order: SaleWebOrderDto) => order.DeliveryTypeCod || 'NONE',
          IsStatus: true,
          Html: {
            DELIVERY: 'badge badge-sm bgc-purple-d1 text-white pb-1 px-25',
            STORE_PICKUP: 'badge badge-sm bgc-pink-d1 text-white pb-1 px-25',
            SCHEDULED_DELIVERY: 'badge badge-sm bgc-brown-d1 text-white pb-1 px-25',
            NONE: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask: {
            DELIVERY: 'Delivery cercano',
            STORE_PICKUP: 'Recojo en tienda',
            SCHEDULED_DELIVERY: 'Entrega programada',
            NONE: 'Sin modalidad'
          }
        },
        {
          Name: 'Estado de entrega',
          key: 'DeliveryStatus',
          IsStatus: true,
          Html: {
            P: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
            S: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            R: 'badge badge-sm bgc-primary-d1 text-white pb-1 px-25',
            L: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            D: 'badge badge-sm bgc-info-d1 text-white pb-1 px-25',
            E: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            X: 'badge badge-sm bgc-secondary text-white pb-1 px-25',
            F: 'badge badge-sm bgc-danger-d1 text-white pb-1 px-25'
          },
          Mask: {
            P: 'Pendiente',
            S: 'Programada',
            R: 'En preparación',
            L: 'Lista para recojo',
            D: 'Despachada',
            E: 'Entregada',
            X: 'Cancelada',
            F: 'Entrega fallida'
          }
        },
        {
          Name: 'Facturación',
          key: 'SaleStatus',
          FunctionKey: (order: SaleWebOrderDto) => this.getFiscalStatus(order),
          IsStatus: true,
          Html: {
            P: 'badge badge-sm bgc-warning-d1 text-white pb-1 px-25',
            C: 'badge badge-sm bgc-success-d1 text-white pb-1 px-25',
            X: 'badge badge-sm bgc-secondary text-white pb-1 px-25'
          },
          Mask: {
            P: 'Pendiente de facturar',
            C: 'Boleta/Factura emitida',
            X: 'No aplica'
          }
        },
        { Name: 'Total', key: 'NumTotalPrice', IsMoney: true },
        { Name: 'Fecha', key: 'CreationDate', IsDate: true },
        {
          Name: 'Opciones',
          ColumnAction: true,
          Id: ['SaleCod'],
          Options: [
            {
              Type: 'Modal', Name: 'fa fa-box-open', Title: 'Iniciar preparación',
              ID: 'modal_start_web_preparation', Function: (order: SaleWebOrderDto) => this.canStartPreparation(order)
            },
            {
              Type: 'Url', Name: 'fa fa-cash-register', Title: 'Pickear y facturar',
              FunctionUrl: (order: SaleWebOrderDto) => this.getProcessSaleUrl(order),
              Function: (order: SaleWebOrderDto) => this.canProcessSale(order)
            },
            {
              Type: 'Modal', Name: 'fa fa-box', Title: 'Marcar lista para recojo',
              ID: 'modal_ready_web_order', Function: (order: SaleWebOrderDto) => this.canMarkReady(order)
            },
            {
              Type: 'Modal', Name: 'fa fa-truck', Title: 'Marcar despachada',
              ID: 'modal_dispatch_web_order', Function: (order: SaleWebOrderDto) => this.canDispatch(order)
            },
            {
              Type: 'Modal', Name: 'fa fa-check-circle', Title: 'Marcar entregada',
              ID: 'modal_deliver_web_order',
              Function: (order: SaleWebOrderDto) => this.canDeliverPickup(order) || this.canCompleteShipment(order)
            },
            {
              Type: 'Modal', Name: 'fa fa-exclamation-triangle', Title: 'Entrega fallida',
              ID: 'modal_failed_web_order', Function: (order: SaleWebOrderDto) => this.canCompleteShipment(order)
            },
            {
              Type: 'Modal', Name: 'fa fa-ban', Title: 'Cancelar pedido',
              ID: 'modal_cancel_web_order', Function: (order: SaleWebOrderDto) => this.canCancel(order)
            },
            {
              Type: 'Url', Name: 'fa fa-file-invoice-dollar', Title: 'Generar nota de crédito total',
              FunctionUrl: (order: SaleWebOrderDto) => this.getCreditNoteUrl(order),
              Function: (order: SaleWebOrderDto) => this.canCreateCreditNote(order)
            },
            {
              Type: 'Url', Name: 'fa fa-search', Title: 'Ver venta',
              FunctionUrl: (order: SaleWebOrderDto) => this.getViewSaleUrl(order)
            }
          ]
        }
      ],
      { data: responsePageSearch },
      'Lista de pedidos web'
    );
    this.dataTablaGenetic = data;
  }

  getDataRow(item: SaleWebOrderDto): void {
    this.selectedOrder = item;
    this.failureCommenter = '';
  }

  actionModal(modalId: string): void {
    if (modalId === 'modal_start_web_preparation') {
      void this.executeStatusChange(this.selectedOrder, this.preparing, '');
    } else if (modalId === 'modal_ready_web_order') {
      void this.executeStatusChange(this.selectedOrder, this.readyForPickup, '');
    } else if (modalId === 'modal_dispatch_web_order') {
      void this.executeStatusChange(this.selectedOrder, this.dispatched, '');
    } else if (modalId === 'modal_deliver_web_order') {
      void this.executeStatusChange(this.selectedOrder, this.delivered, '');
    } else if (modalId === 'modal_cancel_web_order') {
      void this.cancelPendingSale(this.selectedOrder);
    }
  }

  async confirmFailedDelivery(): Promise<void> {
    const commenter = this.failureCommenter.trim();
    if (!commenter) {
      this.toastrService.error('Debe indicar el motivo de la entrega fallida.');
      return;
    }
    const updated = await this.executeStatusChange(this.selectedOrder, this.failed, commenter);
    if (!updated) return;
    const jquery = (window as any).$;
    jquery?.('#modal_failed_web_order')?.modal('hide');
    this.failureCommenter = '';
  }

  async executeStatusChange(
    order: SaleWebOrderDto,
    targetStatus: string,
    commenter: string
  ): Promise<boolean> {
    const request = new SaleDeliveryStatusChangeDto();
    request.SaleCod = order.SaleCod;
    request.TargetStatus = targetStatus;
    request.Commenter = commenter;
    const response = await this.saleWebService.changeDeliveryStatus(request);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message);
      return false;
    }
    if (targetStatus === this.preparing) {
      this.toastrService.success('El pedido ya está disponible para pickear y facturar.');
    } else {
      this.toastrService.success('Estado de entrega actualizado correctamente.');
    }
    await this.findAll(this.responsePageSearch.Page || 1, this.txtSearch?.nativeElement?.value ?? '');
    return true;
  }

  async cancelPendingSale(order: SaleWebOrderDto): Promise<void> {
    const detailResponse: ResponseWsDto = await this.presaleService.cancellationDetail(order.PresaleCod);
    if (detailResponse.ErrorStatus) {
      this.toastrService.error(detailResponse.Message);
      return;
    }
    const detail: PresaleCancellationDetailDto = detailResponse.Data;
    if (!detail.HasStockReservation) {
      this.toastrService.warning(
        'El pedido no tiene una reserva de stock que pueda liberarse mediante la anulación regular.'
      );
      return;
    }
    if (Number(detail.PendingPaymentAmount || 0) > 0) {
      await this.router.navigate(
        ['/enterprise/sale/pages/cancelpresalepayments'],
        {
          queryParams: {
            PresaleCod: order.PresaleCod,
            Mode: 'regular',
            ReturnUrl: '/enterprise/sale/pages/listsaleweb'
          }
        }
      );
      return;
    }
    const response = await this.presaleService.cancel(order.PresaleCod);
    if (response.ErrorStatus) {
      this.toastrService.error(response.Message);
      return;
    }
    this.toastrService.success('Pedido web cancelado correctamente.');
    await this.findAll(this.responsePageSearch.Page || 1, this.txtSearch?.nativeElement?.value ?? '');
  }

  getFiscalStatus(order: SaleWebOrderDto): string {
    if (order.SaleStatus === 'X') return 'X';
    return order.HasFiscalDocument === 'S' ? 'C' : 'P';
  }

  getProcessSaleUrl(order: SaleWebOrderDto): string {
    const returnUrl = encodeURIComponent('/enterprise/sale/pages/listsaleweb');
    return `/enterprise/sale/pages/createsale?SaleCod=${order.SaleCod}&ReturnUrl=${returnUrl}`;
  }

  getViewSaleUrl(order: SaleWebOrderDto): string {
    const returnUrl = encodeURIComponent('/enterprise/sale/pages/listsaleweb');
    return `/enterprise/sale/pages/viewsale?SaleCod=${order.SaleCod}&ReturnUrl=${returnUrl}`;
  }

  getCreditNoteUrl(order: SaleWebOrderDto): string {
    return `/enterprise/sale/pages/createcreditnote?SaleCod=${order.SaleCod}&Mode=failed-delivery`;
  }

  canStartPreparation(order: SaleWebOrderDto): boolean {
    return order.DeliveryStatus === this.pending && order.IsPaid === 'S' && order.SaleStatus === 'P';
  }

  canProcessSale(order: SaleWebOrderDto): boolean {
    return order.DeliveryStatus === this.preparing && order.SaleStatus === 'P';
  }

  canMarkReady(order: SaleWebOrderDto): boolean {
    return order.DeliveryStatus === this.preparing
      && order.SaleStatus === 'C'
      && order.HasFiscalDocument === 'S';
  }

  canDeliverPickup(order: SaleWebOrderDto): boolean {
    return order.DeliveryTypeCod === 'STORE_PICKUP' && order.DeliveryStatus === this.readyForPickup;
  }

  canDispatch(order: SaleWebOrderDto): boolean {
    return this.isShipment(order) && order.DeliveryStatus === this.readyForPickup;
  }

  canCompleteShipment(order: SaleWebOrderDto): boolean {
    return this.isShipment(order) && order.DeliveryStatus === this.dispatched;
  }

  canCancel(order: SaleWebOrderDto): boolean {
    return order.SaleStatus === 'P'
      && (order.DeliveryStatus === this.pending || order.DeliveryStatus === this.preparing);
  }

  canCreateCreditNote(order: SaleWebOrderDto): boolean {
    return order.DeliveryStatus === this.failed
      && order.SaleStatus === 'C'
      && order.HasFiscalDocument === 'S'
      && order.HasCreditNote !== 'S';
  }

  private isShipment(order: SaleWebOrderDto): boolean {
    return order.DeliveryTypeCod === 'DELIVERY'
      || order.DeliveryTypeCod === 'SCHEDULED_DELIVERY';
  }
}
