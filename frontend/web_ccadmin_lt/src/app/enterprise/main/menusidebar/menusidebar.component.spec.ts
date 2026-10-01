import { CommonModule } from '@angular/common';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DataSesionService } from '../../compartido/service/datasesion.service';
import { SidebarMenuConfigDto } from '../../menu/model/dto/SidebarMenuConfigDto';
import { SidebarMenuConfigService } from '../../menu/service/sidebar-menu-config.service';
import { MenusidebarComponent } from './menusidebar.component';

describe('MenusidebarComponent search', () => {
  let fixture: ComponentFixture<MenusidebarComponent>;
  let component: MenusidebarComponent;
  let element: HTMLElement;

  beforeEach(async () => {
    const permissions = new Set(['SALES', 'INVOICE', 'PRESALE', 'HIDDEN', 'PRODUCTS']);
    const menuConfig = [
      new SidebarMenuConfigDto({
        permission: 'SALES', label: 'Ventas', children: [
          { permission: 'INVOICE', label: 'Facturación', url: 'enterprise/sale/pages/listsale' },
          { permission: 'PRESALE', label: 'Preventa', url: 'enterprise/sale/pages/listpresale' },
          { permission: 'HIDDEN', label: 'Ajuste interno', url: 'internal', isVisible: false },
          { permission: 'DENIED', label: 'Nota de crédito', url: 'credit' }
        ]
      }),
      new SidebarMenuConfigDto({
        permission: 'ADMIN', label: 'Administración', children: [
          { permission: 'INVOICE', label: 'Usuarios', url: 'users' }
        ]
      }),
      new SidebarMenuConfigDto({
        permission: 'PRODUCTS', label: 'Productos', children: [
          { permission: 'DENIED', label: 'Bandeja de productos', url: 'products' }
        ]
      })
    ];
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [MenusidebarComponent],
      providers: [
        { provide: DataSesionService, useValue: { PermissionExists: (code: string) => permissions.has(code) } },
        { provide: SidebarMenuConfigService, useValue: { getMenuConfig: () => menuConfig } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(MenusidebarComponent);
    component = fixture.componentInstance;
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  function search(value: string): void {
    const input = element.querySelector<HTMLInputElement>('.sidebar-search-input')!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('filtra al escribir sin distinguir mayúsculas, tildes ni espacios externos', () => {
    search('  FACTURACION  ');
    expect(component.filteredMenus.map(menu => menu.des_menu)).toEqual(['Ventas']);
    expect(component.filteredMenus[0].list_sub_menu.map(menu => menu.des_menu)).toEqual(['Facturación']);
    expect(element.querySelector('#user-sidebar-menus .submenu.show')).not.toBeNull();
    expect(element.querySelector('#user-sidebar-menus a[href="enterprise/sale/pages/listsale"]')).not.toBeNull();
    expect(element.querySelector('#user-sidebar-menus')!.textContent).not.toContain('Preventa');
  });

  it('muestra los hijos visibles y autorizados cuando coincide una sección', () => {
    search('ventas');
    expect(component.filteredMenus[0].list_sub_menu.map(menu => menu.des_menu))
      .toEqual(['Facturación', 'Preventa']);
  });

  it('permite combinar el nombre de la sección con el de una opción', () => {
    search('ventas   facturación');
    expect(component.filteredMenus[0].list_sub_menu.map(menu => menu.des_menu)).toEqual(['Facturación']);
  });

  for (const query of ['Administración', 'Nota de crédito', 'Ajuste interno', 'Productos']) {
    it(`no revela opciones ocultas o sin permiso al buscar ${query}`, () => {
      search(query);
      expect(component.filteredMenus).toEqual([]);
      expect(element.querySelector('[role="status"]')!.textContent).toContain('No se encontraron menús.');
    });
  }

  it('busca también opciones sin submenús', () => {
    search('dashboard');
    expect(component.filteredMenus.map(menu => menu.des_menu)).toEqual(['Dashboard']);
  });

  it('restaura la lista y la selección original al limpiar con el botón', () => {
    const originalMenus = component.g_list_menu;
    const salesMenu = originalMenus.find(menu => menu.des_menu === 'Ventas')!;
    salesMenu.flg_menu_activo = true;
    salesMenu.list_sub_menu[1].shadedMenu = true;
    search('facturacion');
    element.querySelector<HTMLButtonElement>('button[aria-label="Limpiar búsqueda"]')!.click();
    fixture.detectChanges();
    expect(component.filteredMenus).toBe(originalMenus);
    expect(salesMenu.list_sub_menu.length).toBe(3);
    expect(salesMenu.list_sub_menu[1].shadedMenu).toBeTrue();
    expect(salesMenu.flg_menu_activo).toBeTrue();
    expect(component.isFiltering).toBeFalse();
    expect(element.querySelector<HTMLInputElement>('.sidebar-search-input')!.value).toBe('');
  });

  it('limpia la búsqueda con Escape', () => {
    search('no existe');
    element.querySelector('input')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(component.menuSearch).toBe('');
    expect(component.filteredMenus).toBe(component.g_list_menu);
    expect(element.querySelector('[role="status"]')).toBeNull();
  });

  it('trata una búsqueda de solo espacios como una búsqueda vacía', () => {
    search('   ');
    expect(component.isFiltering).toBeFalse();
    expect(component.filteredMenus).toBe(component.g_list_menu);
  });
});
