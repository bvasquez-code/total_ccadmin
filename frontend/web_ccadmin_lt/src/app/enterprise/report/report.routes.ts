import { Routes } from '@angular/router';
import { SalesReportComponent } from './pages/salesreport/salesreport.component';
import { SoldProductsReportComponent } from './pages/soldproductsreport/soldproductsreport.component';
import { StockReportComponent } from './pages/stockreport/stockreport.component';
import { PaymentMethodsReportComponent } from './pages/paymentmethodsreport/paymentmethodsreport.component';
import { DocumentsReportComponent } from './pages/documentsreport/documentsreport.component';
import { ClientsReportComponent } from './pages/clientsreport/clientsreport.component';
import { OrdersReportComponent } from './pages/ordersreport/ordersreport.component';
import { CreditNotesReportComponent } from './pages/creditnotesreport/creditnotesreport.component';
import { ProfitReportComponent } from './pages/profitreport/profitreport.component';

import { PurchaseSalesReportComponent } from './pages/purchasesalesreport/purchasesalesreport.component';

export const REPORT_ROUTES: Routes = [
    { path: 'enterprise/report/pages/purchasesalesreport', component: PurchaseSalesReportComponent },
    { path: 'enterprise/report/pages/salesreport', component: SalesReportComponent },
    { path: 'enterprise/report/pages/soldproductsreport', component: SoldProductsReportComponent },
    { path: 'enterprise/report/pages/stockreport', component: StockReportComponent },
    { path: 'enterprise/report/pages/paymentmethodsreport', component: PaymentMethodsReportComponent },
    { path: 'enterprise/report/pages/documentsreport', component: DocumentsReportComponent },
    { path: 'enterprise/report/pages/clientsreport', component: ClientsReportComponent },
    { path: 'enterprise/report/pages/ordersreport', component: OrdersReportComponent },
    { path: 'enterprise/report/pages/creditnotesreport', component: CreditNotesReportComponent },
    { path: 'enterprise/report/pages/profitreport', component: ProfitReportComponent }
];
