import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CartComponent } from './enterprise/cart/pages/cart/cart.component';
import { CatalogComponent } from './enterprise/catalog/pages/catalog/catalog.component';
import { ProductDetailComponent } from './enterprise/catalog/pages/product-detail/product-detail.component';
import { LoginComponent } from './enterprise/client/pages/login/login.component';
import { RegisterComponent } from './enterprise/client/pages/register/register.component';
import { ProfileComponent } from './enterprise/client/pages/profile/profile.component';
import { ClientAuthGuard } from './enterprise/client/service/client-auth.guard';
import { CheckoutComponent } from './enterprise/sale/pages/checkout/checkout.component';
import { OrdersComponent } from './enterprise/sale/pages/orders/orders.component';

const routes: Routes = [
  { path: '', redirectTo: 'catalog', pathMatch: 'full' },
  { path: 'catalog', component: CatalogComponent },
  { path: 'product/:productCode', component: ProductDetailComponent },
  { path: 'cart', component: CartComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [ClientAuthGuard] },
  { path: 'checkout', component: CheckoutComponent, canActivate: [ClientAuthGuard] },
  { path: 'orders', component: OrdersComponent, canActivate: [ClientAuthGuard] },
  { path: '**', redirectTo: 'catalog' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    scrollPositionRestoration: 'enabled',
    anchorScrolling: 'enabled'
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
