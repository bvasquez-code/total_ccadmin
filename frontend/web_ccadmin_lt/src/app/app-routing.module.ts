import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './enterprise/login/login.component';
import { MainComponent } from './enterprise/main/main.component';
import { CreatemenuComponent } from './enterprise/menu/pages/createmenu/createmenu.component';
import { ListmenuComponent } from './enterprise/menu/pages/listmenu/listmenu.component';
import { CreatepresaleComponent } from './enterprise/sale/pages/createpresale/createpresale.component';
import { RegistropreventaComponent } from './enterprise/venta/pages/registropreventa/registropreventa.component';
import { PruebaComponent } from './pages/prueba/prueba.component';
import { ListuserComponent } from './enterprise/user/pages/listuser/listuser.component';
import { CreateuserComponent } from './enterprise/user/pages/createuser/createuser.component';
import { ListprofileComponent } from './enterprise/user/pages/listprofile/listprofile.component';
import { CreateprofileComponent } from './enterprise/user/pages/createprofile/createprofile.component';
import { CreatesaleComponent } from './enterprise/sale/pages/createsale/createsale.component';
import { ListclientComponent } from './enterprise/client/pages/listclient/listclient.component';
import { CreateclientComponent } from './enterprise/client/pages/createclient/createclient.component';
import { ListsupplierComponent } from './enterprise/supplier/pages/listsupplier/listsupplier.component';
import { CreatesupplierComponent } from './enterprise/supplier/pages/createsupplier/createsupplier.component';
import { ListpresaleComponent } from './enterprise/sale/pages/listpresale/listpresale.component';
import { ListproductComponent } from './enterprise/product/pages/listproduct/listproduct.component';
import { CreateproductComponent } from './enterprise/product/pages/createproduct/createproduct.component';
import { ListbrandComponent } from './enterprise/product/pages/listbrand/listbrand.component';
import { CreatebrandComponent } from './enterprise/product/pages/createbrand/createbrand.component';
import { ListcategoryComponent } from './enterprise/product/pages/listcategory/listcategory.component';
import { CreatecategoryComponent } from './enterprise/product/pages/createcategory/createcategory.component';
import { ListpucharseComponent } from './enterprise/pucharse/pages/listpucharse/listpucharse.component';
import { CreatepucharseComponent } from './enterprise/pucharse/pages/createpucharse/createpucharse.component';
import { ConfirmpucharseComponent } from './enterprise/pucharse/pages/confirmpucharse/confirmpucharse.component';
import { ListreceptionComponent } from './enterprise/pucharse/pages/listreception/listreception.component';
import { CreatetrxpaymentComponent } from './enterprise/trxpayment/pages/createtrxpayment/createtrxpayment.component';
import { ListtrxpaymentComponent } from './enterprise/trxpayment/pages/listtrxpayment/listtrxpayment.component';
import { ViewtrxpaymentComponent } from './enterprise/trxpayment/pages/viewtrxpayment/viewtrxpayment.component';
import { ListsaleComponent } from './enterprise/sale/pages/listsale/listsale.component';
import { ListkardexComponent } from './enterprise/product/pages/listkardex/listkardex.component';
import { AppfileComponent } from './enterprise/system/pages/appfile/appfile.component';
import { CreatecurrencyComponent } from './enterprise/system/pages/createcurrency/createcurrency.component';
import { CreatepaymentmethodComponent } from './enterprise/system/pages/createpaymentmethod/createpaymentmethod.component';
import { ListcurrencyComponent } from './enterprise/system/pages/listcurrency/listcurrency.component';
import { ListpaymentmethodComponent } from './enterprise/system/pages/listpaymentmethod/listpaymentmethod.component';
import { ListcreditnoteComponent } from './enterprise/sale/pages/listcreditnote/listcreditnote.component';
import { CreatecreditnoteComponent } from './enterprise/sale/pages/createcreditnote/createcreditnote.component';
import { ReturnstockcreditnoteComponent } from './enterprise/sale/pages/returnstockcreditnote/returnstockcreditnote.component';
import { ViewcreditnoteComponent } from './enterprise/sale/pages/viewcreditnote/viewcreditnote.component';
import { ClosecashsessionComponent } from './enterprise/cash/pages/closecashsession/closecashsession.component';
import { ListcashregisterComponent } from './enterprise/cash/pages/listcashregister/listcashregister.component';
import { CreatecashregisterComponent } from './enterprise/cash/pages/createcashregister/createcashregister.component';
import { OpencashsessionComponent } from './enterprise/cash/pages/opencashsession/opencashsession.component';
import { ViewcashsessionComponent } from './enterprise/cash/pages/viewcashsession/viewcashsession.component';
import { ListcounterfoilComponent } from './enterprise/cash/pages/listcounterfoil/listcounterfoil.component';
import { CreatecounterfoilComponent } from './enterprise/cash/pages/createcounterfoil/createcounterfoil.component';
import { ListtransferrequestComponent } from './enterprise/transfer/pages/listtransferrequest/listtransferrequest.component';
import { CreatetransferrequestComponent } from './enterprise/transfer/pages/createtransferrequest/createtransferrequest.component';
import { TransferdetailComponent } from './enterprise/transfer/pages/transferdetail/transferdetail.component';
import { ReceivetransferComponent } from './enterprise/transfer/pages/receivetransfer/receivetransfer.component';
import { ListtransferdispatchComponent } from './enterprise/transfer/pages/listtransferdispatch/listtransferdispatch.component';
import { DispatchtransferComponent } from './enterprise/transfer/pages/dispatchtransfer/dispatchtransfer.component';
import { DirecttransferComponent } from './enterprise/transfer/pages/directtransfer/directtransfer.component';
import { CreateproductmassiveComponent } from './enterprise/product/pages/createproductmassive/createproductmassive.component';
import { CreatecategorymassiveComponent } from './enterprise/product/pages/createcategorymassive/createcategorymassive.component';
import { CreatebrandmassiveComponent } from './enterprise/product/pages/createbrandmassive/createbrandmassive.component';
import { ViewpucharseComponent } from './enterprise/pucharse/pages/viewpucharse/viewpucharse.component';
import { ListstoreComponent } from './enterprise/store/pages/liststore/liststore.component';
import { CreatestoreComponent } from './enterprise/store/pages/createstore/createstore.component';
import { ListbusinessconfiggroupComponent } from './enterprise/businessconfiggroup/pages/listbusinessconfiggroup/listbusinessconfiggroup.component';
import { CreatebusinessconfiggroupComponent } from './enterprise/businessconfiggroup/pages/createbusinessconfiggroup/createbusinessconfiggroup.component';
import { CreatebusinessconfigComponent } from './enterprise/businessconfiggroup/pages/createbusinessconfig/createbusinessconfig.component';

const routes: Routes = [
  {
    path: '',
    component: MainComponent
  },
  {
    path: '',
    component: MainComponent,
    children: [
      {
        path: 'pages/prueba',
        component: PruebaComponent
      }
    ]
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/sale/pages/createpresale',
        component: CreatepresaleComponent
      },
      {
        path: 'enterprise/sale/pages/listpresale',
        component: ListpresaleComponent
      },
      {
        path: 'enterprise/sale/pages/listsale',
        component: ListsaleComponent
      },
      {
        path: 'enterprise/sale/pages/createsale',
        component: CreatesaleComponent
      },
      {
        path: 'enterprise/sale/pages/listcreditnote',
        component: ListcreditnoteComponent
      },
      {
        path: 'enterprise/sale/pages/createcreditnote',
        component: CreatecreditnoteComponent
      },
      {
        path: 'enterprise/sale/pages/returnstockcreditnote',
        component: ReturnstockcreditnoteComponent
      },
      {
        path: 'enterprise/sale/pages/viewcreditnote',
        component: ViewcreditnoteComponent
      }
    ]
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/menu/pages/listmenu',
        component: ListmenuComponent
      },
      {
        path: 'enterprise/menu/pages/createmenu',
        component: CreatemenuComponent
      },
      {
        path: 'enterprise/store/pages/liststore',
        component: ListstoreComponent
      },
      {
        path: 'enterprise/store/pages/createstore',
        component: CreatestoreComponent
      },
      {
        path: 'enterprise/businessconfiggroup/pages/listbusinessconfiggroup',
        component: ListbusinessconfiggroupComponent
      },
      {
        path: 'enterprise/businessconfiggroup/pages/createbusinessconfiggroup',
        component: CreatebusinessconfiggroupComponent
      },
      {
        path: 'enterprise/businessconfiggroup/pages/createbusinessconfig',
        component: CreatebusinessconfigComponent
      }
    ]
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/user/pages/listuser',
        component: ListuserComponent
      },
      {
        path: 'enterprise/user/pages/createuser',
        component: CreateuserComponent
      },
      {
        path: 'enterprise/user/pages/listprofile',
        component: ListprofileComponent
      },
      {
        path: 'enterprise/user/pages/createprofile',
        component: CreateprofileComponent
      }
    ]
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/client/pages/listclient',
        component: ListclientComponent
      },
      {
        path: 'enterprise/client/pages/createclient',
        component: CreateclientComponent
      }
    ]
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/supplier/pages/listsupplier',
        component: ListsupplierComponent
      },
      {
        path: 'enterprise/supplier/pages/createsupplier',
        component: CreatesupplierComponent
      }
    ]
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/product/pages/listProduct',
        component: ListproductComponent
      },
      {
        path: 'enterprise/product/pages/createProduct',
        component: CreateproductComponent
      },
      {
        path: 'enterprise/product/pages/listBrand',
        component: ListbrandComponent
      },
      {
        path: 'enterprise/product/pages/createBrand',
        component: CreatebrandComponent
      },
      {
        path: 'enterprise/product/pages/listCategory',
        component: ListcategoryComponent
      },
      {
        path: 'enterprise/product/pages/createCategory',
        component: CreatecategoryComponent
      },
      {
        path: 'enterprise/product/pages/listkardex',
        component: ListkardexComponent
      },
      {
        path: 'enterprise/product/pages/createproductmassive',
        component: CreateproductmassiveComponent
      },
      {
        path: 'enterprise/product/pages/createcategorymassive',
        component: CreatecategorymassiveComponent
      },
      {
        path: 'enterprise/product/pages/createbrandmassive',
        component: CreatebrandmassiveComponent
      }
    ]
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/pucharse/pages/listpucharse',
        component: ListpucharseComponent
      },
      {
        path: 'enterprise/pucharse/pages/createpucharse',
        component: CreatepucharseComponent
      },
      {
        path: 'enterprise/pucharse/pages/confirmpucharse',
        component: ConfirmpucharseComponent
      }
      ,
      {
        path: 'enterprise/pucharse/pages/listreception',
        component: ListreceptionComponent
      },
      {
        path: 'enterprise/pucharse/pages/viewpucharse',
        component: ViewpucharseComponent
      }
    ]
  },
  {
    path: '',
    children: [
      { path: 'enterprise/cash/pages/listcashregister', component: ListcashregisterComponent },
      { path: 'enterprise/cash/pages/createcashregister', component: CreatecashregisterComponent },

      { path: 'enterprise/cash/pages/opencashsession', component: OpencashsessionComponent },
      { path: 'enterprise/cash/pages/closecashsession', component: ClosecashsessionComponent },
      { path: 'enterprise/cash/pages/viewcashsession', component: ViewcashsessionComponent },

      { path: 'enterprise/cash/pages/listcounterfoil', component: ListcounterfoilComponent },
      { path: 'enterprise/cash/pages/createcounterfoil', component: CreatecounterfoilComponent },

    ]
  },

  {
    path: '',
    children: [
      {
        path: 'enterprise/trxpayment/pages/listtrxpayment',
        component: ListtrxpaymentComponent
      },
      {
        path: 'enterprise/trxpayment/pages/viewtrxpayment',
        component: ViewtrxpaymentComponent
      },
      {
        path: 'enterprise/trxpayment/pages/createtrxpayment',
        component: CreatetrxpaymentComponent
      }
    ]
  },
  {
    path: '',
    children: [
      {
        path: 'enterprise/system/pages/appfile',
        component: AppfileComponent
      },
      {
        path: 'enterprise/system/pages/listcurrency',
        component: ListcurrencyComponent
      },
      {
        path: 'enterprise/system/pages/createcurrency',
        component: CreatecurrencyComponent
      },
      {
        path: 'enterprise/system/pages/listpaymentmethod',
        component: ListpaymentmethodComponent
      },
      {
        path: 'enterprise/system/pages/createpaymentmethod',
        component: CreatepaymentmethodComponent
      }
    ]
  },
  {
    path: '',
    children: [
      { path: 'enterprise/transfer/pages/listtransferrequest', component: ListtransferrequestComponent },
      { path: 'enterprise/transfer/pages/createtransferrequest', component: CreatetransferrequestComponent },
      { path: 'enterprise/transfer/pages/transferdetail', component: TransferdetailComponent },
      { path: 'enterprise/transfer/pages/receivetransfer', component: ReceivetransferComponent },
      { path: 'enterprise/transfer/pages/listtransferdispatch', component: ListtransferdispatchComponent },
      { path: 'enterprise/transfer/pages/dispatchtransfer', component: DispatchtransferComponent },
      { path: 'enterprise/transfer/pages/directtransfer', component: DirecttransferComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
