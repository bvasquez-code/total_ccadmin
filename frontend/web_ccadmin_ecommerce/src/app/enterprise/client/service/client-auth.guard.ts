import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { ClientSessionService } from './client-session.service';

@Injectable({ providedIn: 'root' })
export class ClientAuthGuard implements CanActivate {

  public constructor(
    private clientSessionService: ClientSessionService,
    private router: Router
  ) {
  }

  public canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    if (this.clientSessionService.isAuthenticated()) return true;
    return this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
}
