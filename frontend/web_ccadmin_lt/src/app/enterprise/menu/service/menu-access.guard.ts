import { Injectable } from "@angular/core";
import { ActivatedRouteSnapshot, CanActivate, CanActivateChild, Router, RouterStateSnapshot, UrlTree } from "@angular/router";
import { DataSesionService } from "../../compartido/service/datasesion.service";
import { SidebarMenuConfigService } from "./sidebar-menu-config.service";

@Injectable({
    providedIn: 'root'
})
export class MenuAccessGuard implements CanActivate, CanActivateChild {

    constructor(
        private dataSesionService: DataSesionService,
        private router: Router,
        private sidebarMenuConfigService: SidebarMenuConfigService
    ) {
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
        return this.validateRoute(state.url);
    }

    canActivateChild(childRoute: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
        return this.validateRoute(state.url);
    }

    private validateRoute(url: string): boolean | UrlTree {
        const permissionGroups = this.sidebarMenuConfigService.getRoutePermissionGroups(url);

        if (permissionGroups.length === 0) {
            return this.router.parseUrl('/pages/permissiondenied');
        }

        if (permissionGroups.some(group => group.every(permission => this.dataSesionService.PermissionExists(permission)))) {
            return true;
        }

        return this.router.parseUrl('/pages/permissiondenied');
    }
}
