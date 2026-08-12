import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastrModule } from 'ngx-toastr';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CartComponent } from './enterprise/cart/pages/cart/cart.component';
import { CartProductModalComponent } from './enterprise/catalog/component/cart-product-modal/cart-product-modal.component';
import { CatalogComponent } from './enterprise/catalog/pages/catalog/catalog.component';
import { ProductDetailComponent } from './enterprise/catalog/pages/product-detail/product-detail.component';
import { LoginComponent } from './enterprise/client/pages/login/login.component';
import { RegisterComponent } from './enterprise/client/pages/register/register.component';
import { ProfileComponent } from './enterprise/client/pages/profile/profile.component';
import { AddressModalComponent } from './enterprise/client/component/address-modal/address-modal.component';
import { StorefrontFooterComponent } from './enterprise/main/footer/storefront-footer.component';
import { StorefrontHeaderComponent } from './enterprise/main/header/storefront-header.component';
import { CheckoutComponent } from './enterprise/sale/pages/checkout/checkout.component';
import { OrdersComponent } from './enterprise/sale/pages/orders/orders.component';
import { LoadingComponent } from './enterprise/shared/component/loading/loading.component';
import { LocationSelectorComponent } from './enterprise/store/component/location-selector/location-selector.component';
import { LoadingInterceptor } from './interceptors/LoadingInterceptor';

@NgModule({
  declarations: [
    AppComponent,
    StorefrontHeaderComponent,
    StorefrontFooterComponent,
    LocationSelectorComponent,
    LoadingComponent,
    CatalogComponent,
    CartProductModalComponent,
    ProductDetailComponent,
    CartComponent,
    LoginComponent,
    RegisterComponent,
    ProfileComponent,
    AddressModalComponent,
    CheckoutComponent,
    OrdersComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    FormsModule,
    HttpClientModule,
    AppRoutingModule,
    ToastrModule.forRoot({
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      timeOut: 4500
    })
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: LoadingInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
