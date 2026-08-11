import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ResponsePageSearch } from '../../../shared/model/dto/ResponsePageSearch';
import { SaleDeliveryOrderDto } from '../../model/dto/SaleDeliveryOrderDto';
import { OrderService } from '../../service/order.service';

@Component({
  selector: 'app-orders',
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.css']
})
export class OrdersComponent implements OnInit {

  public OrderPage = new ResponsePageSearch<SaleDeliveryOrderDto>();
  public IsLoading: boolean = false;

  public constructor(
    private orderService: OrderService,
    private router: Router,
    private toastrService: ToastrService
  ) {
  }

  public ngOnInit(): void {
    void this.loadPage(1);
  }

  public async loadPage(page: number): Promise<void> {
    if (page < 1 || (this.OrderPage.TotalPages > 0 && page > this.OrderPage.TotalPages)) return;

    this.IsLoading = true;
    try {
      const response = await this.orderService.findMyOrders(page);
      if (response.ErrorStatus) {
        this.toastrService.error(response.Message || 'No se pudieron consultar tus pedidos.');
        return;
      }
      this.OrderPage = Object.assign(
        new ResponsePageSearch<SaleDeliveryOrderDto>(),
        response.Data || {}
      );
      this.OrderPage.resultSearch = (this.OrderPage.resultSearch || []).map(
        item => Object.assign(new SaleDeliveryOrderDto(), item)
      );
    } finally {
      this.IsLoading = false;
    }
  }

  public resumePayment(order: SaleDeliveryOrderDto): void {
    if (!order.CanResumePayment || !order.OrderToken) return;
    void this.router.navigate(['/checkout'], {
      queryParams: { order: order.OrderToken }
    });
  }

  public paymentStatus(order: SaleDeliveryOrderDto): string {
    if (order.IsPaid === 'S') return 'Pago confirmado';
    if (order.PaymentCount > 0) return 'Pago registrado';
    return 'Pendiente de pago';
  }

  public paymentStatusClass(order: SaleDeliveryOrderDto): string {
    if (order.IsPaid === 'S') return 'status-success';
    if (order.PaymentCount > 0) return 'status-info';
    return 'status-warning';
  }

  public saleStatus(order: SaleDeliveryOrderDto): string {
    const status: Record<string, string> = {
      P: 'Pedido recibido',
      C: 'Venta confirmada',
      R: 'Pedido rechazado',
      F: 'Pedido finalizado',
      X: 'Pedido cancelado'
    };
    return status[order.SaleStatus] || order.SaleStatus || 'Sin estado';
  }

  public deliveryStatus(order: SaleDeliveryOrderDto): string {
    const status: Record<string, string> = {
      P: 'Pendiente de preparación',
      S: 'Entrega programada',
      R: 'Preparando pedido',
      L: 'Listo para recoger',
      D: 'Pedido despachado',
      E: 'Pedido entregado',
      X: 'Entrega cancelada',
      F: 'No se pudo entregar'
    };
    return status[order.DeliveryStatus] || 'Pendiente de coordinación';
  }

  public deliveryStatusClass(order: SaleDeliveryOrderDto): string {
    if (order.SaleStatus === 'X' || order.DeliveryStatus === 'X' || order.DeliveryStatus === 'F') {
      return 'tracking-danger';
    }
    if (order.DeliveryStatus === 'E') return 'tracking-success';
    return 'tracking-active';
  }

  public trackingProgress(order: SaleDeliveryOrderDto): number {
    if (order.SaleStatus === 'X' || order.DeliveryStatus === 'X' || order.DeliveryStatus === 'F') {
      return 100;
    }
    const progress: Record<string, number> = {
      P: 20,
      S: 35,
      R: 55,
      L: 75,
      D: 82,
      E: 100
    };
    return progress[order.DeliveryStatus] || 20;
  }
}
