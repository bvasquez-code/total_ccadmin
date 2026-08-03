import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, Router, UrlTree } from '@angular/router';
import { DataSesionService } from '../../compartido/service/datasesion.service';

@Injectable({
  providedIn: 'root'
})
export class SessionAccessGuard implements CanActivate, CanActivateChild {

  constructor(
    private dataSesionService: DataSesionService,
    private router: Router
  ) {
  }

  canActivate(): boolean | UrlTree {
    return this.validateSession();
  }

  canActivateChild(): boolean | UrlTree {
    return this.validateSession();
  }

  private validateSession(): boolean | UrlTree {
    if (this.dataSesionService.SessionExists()) {
      return true;
    }

    return this.router.parseUrl('/login');
  }
}
