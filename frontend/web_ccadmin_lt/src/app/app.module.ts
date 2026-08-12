import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { PruebaComponent } from './pages/prueba/prueba.component';
import { RegistropreventaComponent } from './enterprise/venta/pages/registropreventa/registropreventa.component';
import { MainComponent } from './enterprise/main/main.component';
import { HeaderComponent } from './enterprise/main/header/header.component';
import { MenusidebarComponent } from './enterprise/main/menusidebar/menusidebar.component';
import { FooterComponent } from './enterprise/main/footer/footer.component';
import { LoginComponent } from './enterprise/login/login.component';
import { SigninComponent } from './enterprise/login/signin/signin.component';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { BasicAuthHtppInterceptorService } from './interceptors/BasicAuthHtppInterceptorService';
import { ModalDetalleProductoVentaComponent } from './enterprise/venta/pages/modal-detalle-producto-venta/modal-detalle-producto-venta.component';
import { ModdetalleprodventaComponent } from './enterprise/venta/pages/moddetalleprodventa/moddetalleprodventa.component';
import { CreatepresaleComponent } from './enterprise/sale/pages/createpresale/createpresale.component';
import { CreateproductComponent } from './enterprise/product/pages/createproduct/createproduct.component';
import { CreateproductconfigComponent } from './enterprise/product/pages/createproductconfig/createproductconfig.component';
import { ListproductComponent } from './enterprise/product/pages/listproduct/listproduct.component';
import { CreatemenuComponent } from './enterprise/menu/pages/createmenu/createmenu.component';
import { ListmenuComponent } from './enterprise/menu/pages/listmenu/listmenu.component';
import { TableComponent } from './enterprise/shared/component/table/table.component';
import { CommonModule, DatePipe } from '@angular/common';
import { ListuserComponent } from './enterprise/user/pages/listuser/listuser.component';
import { CreateuserComponent } from './enterprise/user/pages/createuser/createuser.component';
import { ListprofileComponent } from './enterprise/user/pages/listprofile/listprofile.component';
import { CreateprofileComponent } from './enterprise/user/pages/createprofile/createprofile.component';
import { SpinnerComponent } from './enterprise/shared/component/spinner/spinner.component';
import { SpinnerInterceptor } from './interceptors/SpinnerInterceptor';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastrModule } from 'ngx-toastr';
import { CreatesaleComponent } from './enterprise/sale/pages/createsale/createsale.component';
import { ViewsaleComponent } from './enterprise/sale/pages/viewsale/viewsale.component';
import { CreatesaledocumentComponent } from './enterprise/sale/pages/createsaledocument/createsaledocument.component';
import { SalepaymentdetailComponent } from './enterprise/sale/component/salepaymentdetail/salepaymentdetail.component';
import { ListclientComponent } from './enterprise/client/pages/listclient/listclient.component';
import { CreateclientComponent } from './enterprise/client/pages/createclient/createclient.component';
import { ModalalertComponent } from './enterprise/shared/component/modalalert/modalalert.component';
import { ModalsearchclientComponent } from './enterprise/client/pages/modalsearchclient/modalsearchclient.component';
import { ListsupplierComponent } from './enterprise/supplier/pages/listsupplier/listsupplier.component';
import { CreatesupplierComponent } from './enterprise/supplier/pages/createsupplier/createsupplier.component';
import { ModalsearchsupplierComponent } from './enterprise/supplier/pages/modalsearchsupplier/modalsearchsupplier.component';
import { ListpresaleComponent } from './enterprise/sale/pages/listpresale/listpresale.component';
import { ListbrandComponent } from './enterprise/product/pages/listbrand/listbrand.component';
import { CreatebrandComponent } from './enterprise/product/pages/createbrand/createbrand.component';
import { CreatecategoryComponent } from './enterprise/product/pages/createcategory/createcategory.component';
import { ListcategoryComponent } from './enterprise/product/pages/listcategory/listcategory.component';
import { CreatepucharseComponent } from './enterprise/pucharse/pages/createpucharse/createpucharse.component';
import { ListpucharseComponent } from './enterprise/pucharse/pages/listpucharse/listpucharse.component';
import { FormatoMonedaPeruanaPipe } from './enterprise/shared/pipe/FormatoMonedaPeruana.pipe';
import { ModalconfirmComponent } from './enterprise/shared/component/modalconfirm/modalconfirm.component';
import { ConfirmpucharseComponent } from './enterprise/pucharse/pages/confirmpucharse/confirmpucharse.component';
import { ListreceptionComponent } from './enterprise/pucharse/pages/listreception/listreception.component';
import { CreatetrxpaymentComponent } from './enterprise/trxpayment/pages/createtrxpayment/createtrxpayment.component';
import { ListtrxpaymentComponent } from './enterprise/trxpayment/pages/listtrxpayment/listtrxpayment.component';
import { ViewtrxpaymentComponent } from './enterprise/trxpayment/pages/viewtrxpayment/viewtrxpayment.component';
import { ListsaleComponent } from './enterprise/sale/pages/listsale/listsale.component';
import { ListsalewebComponent } from './enterprise/sale/pages/listsaleweb/listsaleweb.component';
import { ListkardexComponent } from './enterprise/product/pages/listkardex/listkardex.component';
import { ListproductstockComponent } from './enterprise/product/pages/listproductstock/listproductstock.component';
import { AppfileComponent } from './enterprise/system/pages/appfile/appfile.component';
import { CreatecurrencyComponent } from './enterprise/system/pages/createcurrency/createcurrency.component';
import { CreatepaymentmethodComponent } from './enterprise/system/pages/createpaymentmethod/createpaymentmethod.component';
import { CreatetaxComponent } from './enterprise/system/pages/createtax/createtax.component';
import { CreatetaxaffectationComponent } from './enterprise/system/pages/createtaxaffectation/createtaxaffectation.component';
import { CreatestoresequenceComponent } from './enterprise/system/pages/createstoresequence/createstoresequence.component';
import { CreatetablesequenceComponent } from './enterprise/system/pages/createtablesequence/createtablesequence.component';
import { ListcurrencyComponent } from './enterprise/system/pages/listcurrency/listcurrency.component';
import { ListpaymentmethodComponent } from './enterprise/system/pages/listpaymentmethod/listpaymentmethod.component';
import { ListstoresequenceComponent } from './enterprise/system/pages/liststoresequence/liststoresequence.component';
import { ListtablesequenceComponent } from './enterprise/system/pages/listtablesequence/listtablesequence.component';
import { ListtaxComponent } from './enterprise/system/pages/listtax/listtax.component';
import { ListtaxaffectationComponent } from './enterprise/system/pages/listtaxaffectation/listtaxaffectation.component';
import { ListcreditnoteComponent } from './enterprise/sale/pages/listcreditnote/listcreditnote.component';
import { CreatecreditnoteComponent } from './enterprise/sale/pages/createcreditnote/createcreditnote.component';
import { ReturnstockcreditnoteComponent } from './enterprise/sale/pages/returnstockcreditnote/returnstockcreditnote.component';
import { SaleStatusPipePipe } from './enterprise/sale/model/pipes/SaleStatusPipe.pipe';
import { ProductinfosalemodalComponent } from './enterprise/sale/modal/productinfosalemodal/productinfosalemodal.component';
import { ViewcreditnoteComponent } from './enterprise/sale/pages/viewcreditnote/viewcreditnote.component';
import { ListcashregisterComponent } from './enterprise/cash/pages/listcashregister/listcashregister.component';
import { CreatecashregisterComponent } from './enterprise/cash/pages/createcashregister/createcashregister.component';
import { OpencashsessionComponent } from './enterprise/cash/pages/opencashsession/opencashsession.component';
import { ViewcashsessionComponent } from './enterprise/cash/pages/viewcashsession/viewcashsession.component';
import { FormsModule } from '@angular/forms';
import { ListcounterfoilComponent } from './enterprise/cash/pages/listcounterfoil/listcounterfoil.component';
import { CreatecounterfoilComponent } from './enterprise/cash/pages/createcounterfoil/createcounterfoil.component';
import { ListtransferrequestComponent } from './enterprise/transfer/pages/listtransferrequest/listtransferrequest.component';
import { CreatetransferrequestComponent } from './enterprise/transfer/pages/createtransferrequest/createtransferrequest.component';
import { TransferdetailComponent } from './enterprise/transfer/pages/transferdetail/transferdetail.component';
import { ReceivetransferComponent } from './enterprise/transfer/pages/receivetransfer/receivetransfer.component';
import { ListtransferdispatchComponent } from './enterprise/transfer/pages/listtransferdispatch/listtransferdispatch.component';
import { DispatchtransferComponent } from './enterprise/transfer/pages/dispatchtransfer/dispatchtransfer.component';
import { DirecttransferComponent } from './enterprise/transfer/pages/directtransfer/directtransfer.component';
import { ViewpucharseComponent } from './enterprise/pucharse/pages/viewpucharse/viewpucharse.component';
import { ListstoreComponent } from './enterprise/store/pages/liststore/liststore.component';
import { CreatestoreComponent } from './enterprise/store/pages/createstore/createstore.component';
import { CreatestorevirtualconfigComponent } from './enterprise/store/pages/createstorevirtualconfig/createstorevirtualconfig.component';
import { ListbusinessconfiggroupComponent } from './enterprise/businessconfiggroup/pages/listbusinessconfiggroup/listbusinessconfiggroup.component';
import { CreatebusinessconfiggroupComponent } from './enterprise/businessconfiggroup/pages/createbusinessconfiggroup/createbusinessconfiggroup.component';
import { CreatebusinessconfigComponent } from './enterprise/businessconfiggroup/pages/createbusinessconfig/createbusinessconfig.component';
import { PermissiondeniedComponent } from './enterprise/shared/component/permissiondenied/permissiondenied.component';
import { CancelpresalepaymentsComponent } from './enterprise/sale/pages/cancelpresalepayments/cancelpresalepayments.component';
import { ListStockEntryComponent } from './enterprise/inventory/pages/liststockentry/liststockentry.component';
import { CreateStockEntryComponent } from './enterprise/inventory/pages/createstockentry/createstockentry.component';
import { ViewStockEntryComponent } from './enterprise/inventory/pages/viewstockentry/viewstockentry.component';
import { ResolveStockEntryComponent } from './enterprise/inventory/pages/resolvestockentry/resolvestockentry.component';
import { ListStockExitComponent } from './enterprise/inventory/pages/liststockexit/liststockexit.component';
import { CreateStockExitComponent } from './enterprise/inventory/pages/createstockexit/createstockexit.component';
import { ViewStockExitComponent } from './enterprise/inventory/pages/viewstockexit/viewstockexit.component';
import { ResolveStockExitComponent } from './enterprise/inventory/pages/resolvestockexit/resolvestockexit.component';
import { StockMovementListComponent } from './enterprise/inventory/components/stock-movement-list/stock-movement-list.component';
import { StockMovementFormComponent } from './enterprise/inventory/components/stock-movement-form/stock-movement-form.component';
import { StockMovementHeaderFormComponent } from './enterprise/inventory/components/stock-movement-header-form/stock-movement-header-form.component';
import { StockMovementDetailEditorComponent } from './enterprise/inventory/components/stock-movement-detail-editor/stock-movement-detail-editor.component';
import { StockQuantitySummaryComponent } from './enterprise/inventory/components/stock-quantity-summary/stock-quantity-summary.component';
import { StockResolutionEditorComponent } from './enterprise/inventory/components/stock-resolution-editor/stock-resolution-editor.component';
import { StockMovementViewComponent } from './enterprise/inventory/components/stock-movement-view/stock-movement-view.component';
import { ListBulkLoadComponent } from './enterprise/bulkload/pages/listbulkload/listbulkload.component';
import { CreateBulkLoadComponent } from './enterprise/bulkload/pages/createbulkload/createbulkload.component';
import { ViewBulkLoadComponent } from './enterprise/bulkload/pages/viewbulkload/viewbulkload.component';

@NgModule({
  declarations: [
    AppComponent,
    MainComponent,
    HeaderComponent,
    MenusidebarComponent,
    FooterComponent,
    PruebaComponent,
    LoginComponent,
    SigninComponent,
    RegistropreventaComponent,
    ModalDetalleProductoVentaComponent,
    ModdetalleprodventaComponent,
    CreatepresaleComponent,
    CreateproductComponent,
    CreateproductconfigComponent,
    ListproductComponent,
    CreatemenuComponent,
    ListmenuComponent,
    TableComponent,
    ListuserComponent,
    CreateuserComponent,
    ListprofileComponent,
    CreateprofileComponent,
    SpinnerComponent,
    CreatesaleComponent,
    ViewsaleComponent,
    CreatesaledocumentComponent,
    SalepaymentdetailComponent,
    ListclientComponent,
    CreateclientComponent,
    ModalalertComponent,
    ModalsearchclientComponent,
    ListsupplierComponent,
    CreatesupplierComponent,
    ModalsearchsupplierComponent,
    ListpresaleComponent,
    CancelpresalepaymentsComponent,
    ListbrandComponent,
    CreatebrandComponent,
    CreatecategoryComponent,
    ListcategoryComponent,
    CreatepucharseComponent,
    ListpucharseComponent,
    FormatoMonedaPeruanaPipe,
    ModalconfirmComponent,
    ConfirmpucharseComponent,
    ListreceptionComponent,
    CreatetrxpaymentComponent,
    ListtrxpaymentComponent,
    ViewtrxpaymentComponent,
    ListsaleComponent,
    ListsalewebComponent,
    ListkardexComponent,
    ListproductstockComponent,
    AppfileComponent,
    ListcurrencyComponent,
    CreatecurrencyComponent,
    ListpaymentmethodComponent,
    CreatepaymentmethodComponent,
    ListtaxComponent,
    CreatetaxComponent,
    ListtaxaffectationComponent,
    CreatetaxaffectationComponent,
    ListstoresequenceComponent,
    CreatestoresequenceComponent,
    ListtablesequenceComponent,
    CreatetablesequenceComponent,
    ListcreditnoteComponent,
    CreatecreditnoteComponent,
    ReturnstockcreditnoteComponent,
    SaleStatusPipePipe,
    ProductinfosalemodalComponent,
    ViewcreditnoteComponent,
    ListcashregisterComponent,
    CreatecashregisterComponent,
    OpencashsessionComponent,
    ViewcashsessionComponent,
    ListcounterfoilComponent,
    CreatecounterfoilComponent,
    ListtransferrequestComponent,
    CreatetransferrequestComponent,
    TransferdetailComponent,
    ReceivetransferComponent,
    ListtransferdispatchComponent,
    DispatchtransferComponent,
    DirecttransferComponent,
    ViewpucharseComponent,
    ListstoreComponent,
    CreatestoreComponent,
    CreatestorevirtualconfigComponent,
    ListbusinessconfiggroupComponent,
    CreatebusinessconfiggroupComponent,
    CreatebusinessconfigComponent,
    PermissiondeniedComponent,
    ListStockEntryComponent,
    CreateStockEntryComponent,
    ViewStockEntryComponent,
    ResolveStockEntryComponent,
    ListStockExitComponent,
    CreateStockExitComponent,
    ViewStockExitComponent,
    ResolveStockExitComponent,
    StockMovementListComponent,
    StockMovementFormComponent,
    StockMovementHeaderFormComponent,
    StockMovementDetailEditorComponent,
    StockQuantitySummaryComponent,
    StockResolutionEditorComponent,
    StockMovementViewComponent,
    ListBulkLoadComponent,
    CreateBulkLoadComponent,
    ViewBulkLoadComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    CommonModule,
    BrowserAnimationsModule,
    ToastrModule.forRoot(),
    FormsModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: BasicAuthHtppInterceptorService,
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: SpinnerInterceptor,
      multi: true,
    },
    DatePipe
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
