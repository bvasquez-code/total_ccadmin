import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { Subscription } from 'rxjs';
import { CartService } from '../../cart/service/cart.service';
import { ClientSessionDto } from '../../client/model/dto/ClientSessionDto';
import { ClientSessionService } from '../../client/service/client-session.service';
import { StoreContextDto } from '../../store/model/dto/StoreContextDto';
import { StoreContextService } from '../../store/service/store-context.service';
import { VirtualStoreService } from '../../store/service/virtual-store.service';

@Component({
  selector: 'app-storefront-header',
  templateUrl: './storefront-header.component.html',
  styleUrls: ['./storefront-header.component.css']
})
export class StorefrontHeaderComponent implements OnInit, OnDestroy {
  public StoreContext: StoreContextDto | null = null;
  public ClientSession: ClientSessionDto | null = null;
  public CartCount: number = 0;
  public ShowLocationModal: boolean = false;
  public MobileMenuOpen: boolean = false;

  private subscriptions = new Subscription();

  public constructor(
    private storeContextService: StoreContextService,
    private virtualStoreService: VirtualStoreService,
    private cartService: CartService,
    private clientSessionService: ClientSessionService,
    private router: Router,
    private toastrService: ToastrService
  ) {
  }

  public ngOnInit(): void {
    this.subscriptions.add(this.storeContextService.Context$.subscribe(context => this.StoreContext = context));
    this.subscriptions.add(this.storeContextService.LocationSelectionRequested$.subscribe(
      () => this.ShowLocationModal = true
    ));
    this.subscriptions.add(this.cartService.Cart$.subscribe(() => this.CartCount = this.cartService.count()));
    this.subscriptions.add(this.clientSessionService.Session$.subscribe(session => this.ClientSession = session));

    if (!this.StoreContext) void this.resolveInitialStore();
  }

  public ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  public openLocation(): void {
    this.ShowLocationModal = true;
    this.MobileMenuOpen = false;
  }

  public applyStore(context: StoreContextDto): void {
    const cart = this.cartService.getCurrent();
    const changesCartStore = !!cart.StoreCod && cart.StoreCod !== context.Store.StoreCod;
    if (changesCartStore) {
      const confirmed = window.confirm(
        'Tu carrito pertenece a otra tienda. Para cambiar de ubicación debemos vaciarlo. ¿Deseas continuar?'
      );
      if (!confirmed) return;
      this.cartService.clear();
    }

    this.storeContextService.setContext(context);
    this.toastrService.success(`Ahora estás comprando en ${context.Store.Name}.`);
  }

  public logout(): void {
    this.clientSessionService.logout();
    this.MobileMenuOpen = false;
    void this.router.navigate(['/']);
    this.toastrService.info('Tu sesión se cerró correctamente.');
  }

  private async resolveInitialStore(): Promise<void> {
    const response = await this.virtualStoreService.resolveByIp();
    if (!response.ErrorStatus && response.Data?.Store?.StoreCod) {
      this.storeContextService.setContext(Object.assign(new StoreContextDto(), response.Data));
      return;
    }
    this.ShowLocationModal = true;
  }
}
