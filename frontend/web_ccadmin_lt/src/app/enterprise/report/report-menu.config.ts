import { SidebarMenuConfigDto } from '../menu/model/dto/SidebarMenuConfigDto';

export const REPORT_MENU = new SidebarMenuConfigDto({
    permission: 'RP000000',
    label: 'Reportes',
    icon: 'nav-icon fa fa-chart-bar',
    children: [
        { permission: 'RP000001', label: 'Reporte de ventas', url: 'enterprise/report/pages/salesreport', icon: 'nav-icon fa fa-shopping-bag' },
        { permission: 'RP000002', label: 'Reporte de productos vendidos', url: 'enterprise/report/pages/soldproductsreport', icon: 'nav-icon fa fa-box-open' },
        { permission: 'RP000003', label: 'Reporte de inventario / stock', url: 'enterprise/report/pages/stockreport', icon: 'nav-icon fa fa-layer-group' },
        { permission: 'RP000004', label: 'Reporte de medios de pago', url: 'enterprise/report/pages/paymentmethodsreport', icon: 'nav-icon fa fa-credit-card' },
        { permission: 'RP000005', label: 'Reporte de comprobantes', url: 'enterprise/report/pages/documentsreport', icon: 'nav-icon fa fa-file-invoice' },
        { permission: 'RP000006', label: 'Reporte de clientes', url: 'enterprise/report/pages/clientsreport', icon: 'nav-icon fa fa-users' },
        { permission: 'RP000007', label: 'Reporte de pedidos', url: 'enterprise/report/pages/ordersreport', icon: 'nav-icon fa fa-clipboard-list' },
        { permission: 'RP000008', label: 'Reporte de devoluciones y notas de crédito', url: 'enterprise/report/pages/creditnotesreport', icon: 'nav-icon fa fa-undo' },
        { permission: 'RP000010', label: 'Compras vs. ventas', url: 'enterprise/report/pages/purchasesalesreport', icon: 'nav-icon fa fa-balance-scale' },
        { permission: 'RP000009', label: 'Utilidad por venta', url: 'enterprise/report/pages/profitreport', icon: 'nav-icon fa fa-chart-line' }
    ]
});
