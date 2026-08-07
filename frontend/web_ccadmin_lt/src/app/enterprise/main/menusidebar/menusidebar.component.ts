import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { SidebarMenuConfigDto } from 'src/app/enterprise/menu/model/dto/SidebarMenuConfigDto';
import { SidebarSubMenuConfigDto } from 'src/app/enterprise/menu/model/dto/SidebarSubMenuConfigDto';
import { SidebarMenuConfigService } from 'src/app/enterprise/menu/service/sidebar-menu-config.service';
import { MenuPagina } from 'src/app/enterprise/menu/model/entity/MenuPagina';
import { SubMenuPagina } from 'src/app/enterprise/menu/model/entity/SubMenuPagina';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { CashsessionService } from '../../cash/service/CashsessionService';
import { CurrentCashSessionDto } from '../../cash/model/dto/CurrentCashSessionDto';
import { ResponseWsDto } from '../../shared/model/dto/ResponseWsDto';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-menusidebar',
  templateUrl: './menusidebar.component.html',
  styleUrls: ['./menusidebar.component.css']
})
export class MenusidebarComponent implements OnInit, OnDestroy {

  public g_flg_menu_defecto: boolean = false;
  public g_list_menu: MenuPagina[] = [];
  public isOpenMenu: boolean = false;

  private readonly defaultIcon = "nav-icon fa fa-cube";
  private cashSessionChangedSubscription?: Subscription;

  constructor(
    @Inject(DOCUMENT) private document: Document,
    private dataSesionService: DataSesionService,
    private sidebarMenuConfigService: SidebarMenuConfigService,
    private cashSessionService: CashsessionService
  ) {
  }

  ngOnInit(): void {
    this.cashSessionChangedSubscription = this.cashSessionService.cashSessionChanged$.subscribe(() => {
      this.ObtenerMenu();
    });
    this.ObtenerMenu();
  }

  ngOnDestroy(): void {
    this.cashSessionChangedSubscription?.unsubscribe();
  }

  async ObtenerMenu(): Promise<void> {
    this.g_list_menu = [this.getOptionDashboard()];

    const menuConfig = this.sidebarMenuConfigService.getMenuConfig();
    await this.configureCurrentCashSessionOption(menuConfig);

    menuConfig.forEach(config => this.addMenuIfAllowed(config));
    this.markActiveMenu();
  }

  private async configureCurrentCashSessionOption(menuConfig: SidebarMenuConfigDto[]): Promise<void> {
    if (!this.permissionExists("CJ000000") || !this.permissionExists("CJ000003")) {
      return;
    }

    const cashMenu = menuConfig.find(config => config.permission === "CJ000000");
    const cashSessionOption = cashMenu?.children.find(child =>
      this.normalizeUrl(child.url) === "enterprise/cash/pages/opencashsession"
    );

    if (!cashSessionOption) return;

    const rpt: ResponseWsDto = await this.cashSessionService.findCurrent();
    if (rpt.ErrorStatus) return;

    const current: CurrentCashSessionDto = rpt.Data;
    if (current.IsOpen && current.CashSession) {
      cashSessionOption.label = "Cerrar caja";
      cashSessionOption.url = "enterprise/cash/pages/closecashsession";
      cashSessionOption.urlPosition = "enterprise/cash/pages/closecashsession";
    }
  }

  private addMenuIfAllowed(config: SidebarMenuConfigDto): void {
    if (!this.permissionExists(config.permission)) {
      return;
    }

    const menu = this.createMainMenu(config.label, config.icon);

    config.children.forEach(childConfig => {
      if (this.permissionExists(childConfig.permission)) {
        menu.list_sub_menu.push(this.createSubMenu(childConfig));
      }
    });

    if (menu.list_sub_menu.length > 0) {
      this.g_list_menu.push(menu);
    }
  }

  private getOptionDashboard(): MenuPagina {
    const mainMenu: MenuPagina = new MenuPagina();
    mainMenu.url = "";
    mainMenu.des_menu = "Dashboard";
    mainMenu.icono = "nav-icon fa fa-tachometer-alt";
    return mainMenu;
  }

  private createMainMenu(label: string, icon: string): MenuPagina {
    const mainMenu: MenuPagina = new MenuPagina();
    mainMenu.url = "#";
    mainMenu.des_menu = label;
    mainMenu.icono = icon || this.defaultIcon;
    return mainMenu;
  }

  private createSubMenu(config: SidebarSubMenuConfigDto): SubMenuPagina {
    const subMenu: SubMenuPagina = new SubMenuPagina();
    subMenu.url = config.url;
    subMenu.url_position = config.urlPosition || config.url;
    subMenu.url_shade = config.urlShade || config.url;
    subMenu.des_menu = config.label;
    subMenu.icono = config.icon || this.defaultIcon;
    subMenu.IsVisible = config.isVisible !== false;
    return subMenu;
  }

  private markActiveMenu(): void {
    const url = this.normalizeUrl(this.document.location.pathname);
    const requestedShadeUrl = new URLSearchParams(this.document.location.search).get('ReturnUrl') || '';
    this.isOpenMenu = false;

    for (const menu of this.g_list_menu) {
      for (const submenu of menu.list_sub_menu) {
        const urlPosition = this.normalizeUrl(submenu.url_position);
        if (submenu.url !== "" && submenu.url !== null && url.includes(urlPosition)) {
          submenu.flg_menu_activo = true;
          menu.flg_menu_activo = true;
          this.isOpenMenu = true;
          this.markShadedSubMenu(submenu, requestedShadeUrl);
        }
      }
    }
  }

  private markShadedSubMenu(submenu: SubMenuPagina, requestedShadeUrl: string): void {
    const normalizedRequestedShadeUrl = this.normalizeUrl(requestedShadeUrl);
    const canUseRequestedShade = this.g_list_menu.some(menu =>
      menu.list_sub_menu.some(item =>
        item.IsVisible && this.normalizeUrl(item.url) === normalizedRequestedShadeUrl
      )
    );
    const shadeUrl = canUseRequestedShade
      ? normalizedRequestedShadeUrl
      : this.normalizeUrl(submenu.url_shade || submenu.url);

    this.g_list_menu.forEach(menu => {
      const itemView = menu.list_sub_menu.find(item => this.normalizeUrl(item.url) === shadeUrl);
      if (itemView) {
        itemView.shadedMenu = true;
      }
    });
  }

  private normalizeUrl(url: string): string {
    return (url || '')
      .split('?')[0]
      .split('#')[0]
      .replace(/^\/+/, '')
      .replace(/\/+$/, '');
  }

  private permissionExists(menuCod: string): boolean {
    return this.dataSesionService.PermissionExists(menuCod);
  }

}
