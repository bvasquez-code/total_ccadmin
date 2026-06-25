import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnInit } from '@angular/core';
import { SidebarMenuConfigDto } from 'src/app/enterprise/menu/model/dto/SidebarMenuConfigDto';
import { SidebarSubMenuConfigDto } from 'src/app/enterprise/menu/model/dto/SidebarSubMenuConfigDto';
import { SidebarMenuConfigService } from 'src/app/enterprise/menu/service/sidebar-menu-config.service';
import { MenuPagina } from 'src/app/enterprise/menu/model/entity/MenuPagina';
import { SubMenuPagina } from 'src/app/enterprise/menu/model/entity/SubMenuPagina';
import { DataSesionService } from '../../compartido/service/datasesion.service';

@Component({
  selector: 'app-menusidebar',
  templateUrl: './menusidebar.component.html'
})
export class MenusidebarComponent implements OnInit {

  public g_flg_menu_defecto: boolean = false;
  public g_list_menu: MenuPagina[] = [];
  public isOpenMenu: boolean = false;

  private readonly defaultIcon = "nav-icon fa fa-cube";

  constructor(
    @Inject(DOCUMENT) private document: Document,
    private dataSesionService: DataSesionService,
    private sidebarMenuConfigService: SidebarMenuConfigService
  ) {
  }

  ngOnInit(): void {
    this.ObtenerMenu();
  }

  ObtenerMenu(): void {
    this.g_list_menu = [this.getOptionDashboard()];

    this.sidebarMenuConfigService.getMenuConfig().forEach(config => this.addMenuIfAllowed(config));
    this.markActiveMenu();
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
    const url = this.document.location.href;
    this.isOpenMenu = false;

    for (const menu of this.g_list_menu) {
      for (const submenu of menu.list_sub_menu) {
        if (submenu.url !== "" && submenu.url !== null && url.includes(submenu.url_position)) {
          submenu.flg_menu_activo = true;
          menu.flg_menu_activo = true;
          this.isOpenMenu = true;
          this.markShadedSubMenu(submenu);
        }
      }
    }
  }

  private markShadedSubMenu(submenu: SubMenuPagina): void {
    const shadeUrl = submenu.url_shade || submenu.url;

    this.g_list_menu.forEach(menu => {
      const itemView = menu.list_sub_menu.find(item => item.url === shadeUrl);
      if (itemView) {
        itemView.shadedMenu = true;
      }
    });
  }

  private permissionExists(menuCod: string): boolean {
    return this.dataSesionService.PermissionExists(menuCod);
  }

}
