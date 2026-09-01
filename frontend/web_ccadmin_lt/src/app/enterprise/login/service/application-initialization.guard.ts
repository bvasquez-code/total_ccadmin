import { Injectable } from '@angular/core';
import {
  CanActivate,
  CanActivateChild,
  Router,
  RouterStateSnapshot,
  UrlTree
} from '@angular/router';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import {
  ApplicationInitializationService,
  ApplicationInitializationStatusDto
} from './application-initialization.service';

@Injectable({
  providedIn: 'root'
})
export class ApplicationInitializationGuard implements CanActivate, CanActivateChild {

  private readonly initializationUrl =
    '/enterprise/system/pages/applicationinitialization';

  constructor(
    private dataSesionService: DataSesionService,
    private applicationInitializationService: ApplicationInitializationService,
    private router: Router
  ) {
  }

  canActivate(
    _route: unknown,
    state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> {
    return this.validateRoute(state.url);
  }

  canActivateChild(
    _route: unknown,
    state: RouterStateSnapshot
  ): Promise<boolean | UrlTree> {
    return this.validateRoute(state.url);
  }

  private async validateRoute(url: string): Promise<boolean | UrlTree> {
    let initializationRequired =
      this.dataSesionService.RequiresApplicationInitialization();
    const isInitializationRoute = url.split('?')[0] === this.initializationUrl;

    if (initializationRequired) {
      const response = await this.applicationInitializationService.findStatus();
      if (!response.ErrorStatus && response.Data) {
        const status = response.Data as ApplicationInitializationStatusDto;
        if (!status.Required) {
          this.dataSesionService.CompleteApplicationInitialization();
          initializationRequired = false;
        }
      }
    }

    if (initializationRequired) {
      return isInitializationRoute
        ? true
        : this.router.parseUrl(this.initializationUrl);
    }

    const hasSavedProgress =
      this.dataSesionService.getSessionStorageDto().UserCod.toUpperCase() === 'ROOT'
      && sessionStorage.getItem('ApplicationInitializationStep') !== null;
    return isInitializationRoute && !hasSavedProgress
      ? this.router.parseUrl('/')
      : true;
  }
}
