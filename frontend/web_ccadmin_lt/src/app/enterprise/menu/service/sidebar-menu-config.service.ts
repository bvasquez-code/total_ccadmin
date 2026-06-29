import { Injectable } from "@angular/core";
import { SidebarMenuConfigDto } from "../model/dto/SidebarMenuConfigDto";

@Injectable({
    providedIn: 'root'
})
export class SidebarMenuConfigService {

    private readonly menuConfig: SidebarMenuConfigDto[] = [
        new SidebarMenuConfigDto({
            permission: "VT000000",
            label: "ventas",
            icon: "nav-icon fa fa-shopping-bag",
            children: [
                { permission: "VT000001", label: "Realizar venta", url: "enterprise/sale/pages/createpresale" },
                { permission: "VT000002", label: "Preventa", url: "enterprise/sale/pages/listpresale" },
                { permission: "VT000003", label: "Facturacion", url: "enterprise/sale/pages/listsale" },
                { permission: "VT000004", label: "Creacion de preventas", url: "enterprise/sale/pages/createpresale", urlShade: "enterprise/sale/pages/createpresale", isVisible: false },
                { permission: "VT000005", label: "Proceso de facturacion de venta", url: "enterprise/sale/pages/createsale", urlShade: "enterprise/sale/pages/listsale", isVisible: false },
                { permission: "VT000006", label: "Nota de credito", url: "enterprise/sale/pages/listcreditnote" },
                { permission: "VT000007", label: "Crear nota de credito", url: "enterprise/sale/pages/createcreditnote", urlShade: "enterprise/sale/pages/listcreditnote", isVisible: false },
                { permission: "VT000008", label: "Devolver stock de nota de credito", url: "enterprise/sale/pages/returnstockcreditnote", urlShade: "enterprise/sale/pages/listcreditnote", isVisible: false },
                { permission: "VT000009", label: "Ver nota de credito", url: "enterprise/sale/pages/viewcreditnote", urlShade: "enterprise/sale/pages/listcreditnote", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "AT000000",
            label: "Administrar tienda",
            icon: "nav-icon fa fa-store",
            children: [
                { permission: "AT000001", label: "Bandeja de Clientes", url: "enterprise/client/pages/listclient" },
                { permission: "AT000002", label: "Creacion de clientes", url: "enterprise/client/pages/createclient", urlShade: "enterprise/client/pages/listclient", isVisible: false },
                { permission: "AT000003", label: "Bandeja de Proveedores", url: "enterprise/supplier/pages/listsupplier" },
                { permission: "AT000004", label: "Creacion de proveedores", url: "enterprise/supplier/pages/createsupplier", urlShade: "enterprise/supplier/pages/listsupplier", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "CJ000000",
            label: "Modulo Caja",
            icon: "nav-icon fa fa-cash-register",
            children: [
                { permission: "CJ000001", label: "Bandeja de Cajas/POS", url: "enterprise/cash/pages/listcashregister" },
                { permission: "CJ000002", label: "Crear/Editar Caja", url: "enterprise/cash/pages/createcashregister", urlShade: "enterprise/cash/pages/listcashregister", isVisible: false },
                { permission: "CJ000003", label: "Apertura/Cierre", url: "enterprise/cash/pages/opencashsession", urlShade: "enterprise/cash/pages/listcashregister", isVisible: false },
                { permission: "CJ000003", label: "Cerrar Caja", url: "enterprise/cash/pages/closecashsession", urlShade: "enterprise/cash/pages/listcashregister", isVisible: false },
                { permission: "CJ000003", label: "Ver Sesion Caja", url: "enterprise/cash/pages/viewcashsession", urlShade: "enterprise/cash/pages/listcashregister", isVisible: false },
                { permission: "CJ000004", label: "Talonarios", url: "enterprise/cash/pages/listcounterfoil" },
                { permission: "CJ000005", label: "Crear/Editar Talonario", url: "enterprise/cash/pages/createcounterfoil", urlShade: "enterprise/cash/pages/listcounterfoil", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "CO000000",
            label: "compras",
            icon: "nav-icon fa fa-shopping-cart",
            children: [
                { permission: "CO000001", label: "Bandeja de compras", url: "enterprise/pucharse/pages/listpucharse" },
                { permission: "CO000001", label: "Ver compra", url: "enterprise/pucharse/pages/viewpucharse", urlShade: "enterprise/pucharse/pages/listpucharse", isVisible: false },
                { permission: "CO000002", label: "Recepcion de compras", url: "enterprise/pucharse/pages/listreception" },
                { permission: "CO000003", label: "Creacion de compras", url: "enterprise/pucharse/pages/createpucharse", urlShade: "enterprise/pucharse/pages/listpucharse", isVisible: false },
                { permission: "CO000004", label: "Proceso de recepcion de compras", url: "enterprise/pucharse/pages/confirmpucharse", urlShade: "enterprise/pucharse/pages/listpucharse", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "PR000000",
            label: "productos",
            icon: "nav-icon fa fa-boxes",
            children: [
                { permission: "PR000001", label: "Bandeja de Productos", url: "enterprise/product/pages/listProduct" },
                { permission: "PR000002", label: "Bandeja de Marcas", url: "enterprise/product/pages/listBrand" },
                { permission: "PR000003", label: "Bandeja de Categorias", url: "enterprise/product/pages/listCategory" },
                { permission: "PR000005", label: "Creacion de Productos", url: "enterprise/product/pages/createProduct", urlShade: "enterprise/product/pages/listProduct", isVisible: false },
                { permission: "PR000005", label: "Configuracion de producto por tienda", url: "enterprise/product/pages/createproductconfig", urlShade: "enterprise/product/pages/listProduct", isVisible: false },
                { permission: "PR000006", label: "Creacion de Marcas", url: "enterprise/product/pages/createBrand", urlShade: "enterprise/product/pages/listBrand", isVisible: false },
                { permission: "PR000007", label: "Creacion de categorias", url: "enterprise/product/pages/createCategory", urlShade: "enterprise/product/pages/listCategory", isVisible: false },
                { permission: "PR000009", label: "Creacion masiva de Productos", url: "enterprise/product/pages/createproductmassive", urlShade: "enterprise/product/pages/listProduct", isVisible: false },
                { permission: "PR000010", label: "Creacion masiva de Marcas", url: "enterprise/product/pages/createbrandmassive", urlShade: "enterprise/product/pages/listBrand", isVisible: false },
                { permission: "PR000011", label: "Creacion masiva de categorias", url: "enterprise/product/pages/createcategorymassive", urlShade: "enterprise/product/pages/listCategory", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "SE000000",
            label: "Seguimiento",
            icon: "nav-icon fa fa-chart-line",
            children: [
                { permission: "PR000004", label: "Kardex", url: "enterprise/product/pages/listkardex" },
                { permission: "PR000008", label: "Visualizacion de detalle de kardex", url: "enterprise/product/pages/listkardex", urlShade: "enterprise/product/pages/listkardex", isVisible: false },
                { permission: "SE000001", label: "Pagos", url: "enterprise/trxpayment/pages/listtrxpayment" },
                { permission: "SE000001", label: "Ver pago", url: "enterprise/trxpayment/pages/viewtrxpayment", urlShade: "enterprise/trxpayment/pages/listtrxpayment", isVisible: false },
                { permission: "SE000001", label: "Crear pago", url: "enterprise/trxpayment/pages/createtrxpayment", urlShade: "enterprise/trxpayment/pages/listtrxpayment", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "SI000000",
            label: "sistema",
            icon: "nav-icon fa fa-cogs",
            children: [
                { permission: "SI000001", label: "Bandeja de menus", url: "enterprise/menu/pages/listmenu" },
                { permission: "SI000002", label: "Creacion de menus", url: "enterprise/menu/pages/createmenu", urlShade: "enterprise/menu/pages/listmenu", isVisible: false },
                { permission: "SI000003", label: "Bandeja de Tiendas", url: "enterprise/store/pages/liststore" },
                { permission: "SI000004", label: "Creacion de Tiendas", url: "enterprise/store/pages/createstore", urlShade: "enterprise/store/pages/liststore", isVisible: false },
                { permission: "SI000005", label: "Monedas", url: "enterprise/system/pages/listcurrency" },
                { permission: "SI000006", label: "Crear moneda", url: "enterprise/system/pages/createcurrency", urlShade: "enterprise/system/pages/listcurrency", isVisible: false },
                { permission: "SI000007", label: "Metodos de pago", url: "enterprise/system/pages/listpaymentmethod" },
                { permission: "SI000008", label: "Crear metodo de pago", url: "enterprise/system/pages/createpaymentmethod", urlShade: "enterprise/system/pages/listpaymentmethod", isVisible: false },
                { permission: "SI000009", label: "Grupos de configuracion", url: "enterprise/businessconfiggroup/pages/listbusinessconfiggroup" },
                { permission: "SI000010", label: "Crear grupo de configuracion", url: "enterprise/businessconfiggroup/pages/createbusinessconfiggroup", urlShade: "enterprise/businessconfiggroup/pages/listbusinessconfiggroup", isVisible: false },
                { permission: "SI000011", label: "Administrar valores de configuracion", url: "enterprise/businessconfiggroup/pages/createbusinessconfig", urlShade: "enterprise/businessconfiggroup/pages/listbusinessconfiggroup", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "TR000000",
            label: "Transferencias",
            icon: "nav-icon fa fa-truck",
            children: [
                { permission: "TR000001", label: "Solicitudes de transferencia", url: "enterprise/transfer/pages/listtransferrequest" },
                { permission: "TR000001", label: "Detalle de transferencia", url: "enterprise/transfer/pages/transferdetail", urlShade: "enterprise/transfer/pages/listtransferrequest", isVisible: false },
                { permission: "TR000002", label: "Crear solicitud de transferencia", url: "enterprise/transfer/pages/createtransferrequest", urlShade: "enterprise/transfer/pages/listtransferrequest", isVisible: false },
                { permission: "TR000003", label: "Despacho de transferencias", url: "enterprise/transfer/pages/listtransferdispatch" },
                { permission: "TR000004", label: "Envio directo de transferencia", url: "enterprise/transfer/pages/directtransfer", urlShade: "enterprise/transfer/pages/listtransferdispatch", isVisible: false },
                { permission: "TR000005", label: "Despachar transferencia", url: "enterprise/transfer/pages/dispatchtransfer", urlShade: "enterprise/transfer/pages/listtransferdispatch", isVisible: false },
                { permission: "TR000006", label: "Recepcionar transferencia", url: "enterprise/transfer/pages/receivetransfer", urlShade: "enterprise/transfer/pages/listtransferdispatch", isVisible: false }
            ]
        }),
        new SidebarMenuConfigDto({
            permission: "US000000",
            label: "usuarios",
            icon: "nav-icon fa fa-users",
            children: [
                { permission: "US000001", label: "Bandeja de Usuarios", url: "enterprise/user/pages/listuser" },
                { permission: "US000002", label: "Bandeja de perfiles", url: "enterprise/user/pages/listprofile" },
                { permission: "US000003", label: "Creacion de usuarios", url: "enterprise/user/pages/createuser", urlShade: "enterprise/user/pages/listuser", isVisible: false },
                { permission: "US000004", label: "Creacion de perfiles", url: "enterprise/user/pages/createprofile", urlShade: "enterprise/user/pages/listprofile", isVisible: false }
            ]
        })
    ];

    getMenuConfig(): SidebarMenuConfigDto[] {
        return this.menuConfig.map(config => new SidebarMenuConfigDto(config));
    }

    getRoutePermissions(url: string): string[] {
        const permissions = new Set<string>();

        this.getRoutePermissionGroups(url).forEach(group => {
            group.forEach(permission => permissions.add(permission));
        });

        return Array.from(permissions);
    }

    getRoutePermissionGroups(url: string): string[][] {
        const normalizedUrl = this.normalizeUrl(url);
        const permissionGroups: string[][] = [];

        this.menuConfig.forEach(menu => {
            menu.children.forEach(child => {
                if (this.normalizeUrl(child.url) === normalizedUrl || this.normalizeUrl(child.urlPosition || "") === normalizedUrl) {
                    permissionGroups.push([menu.permission, child.permission]);
                }
            });
        });

        return permissionGroups;
    }

    isMappedRoute(url: string): boolean {
        return this.getRoutePermissions(url).length > 0;
    }

    private normalizeUrl(url: string): string {
        return (url || "")
            .split("?")[0]
            .split("#")[0]
            .replace(/^\/+/, "")
            .replace(/\/+$/, "");
    }
}
