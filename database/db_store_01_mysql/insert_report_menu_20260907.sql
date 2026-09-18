-- MySQL 8. Ejecutar sobre la base de la aplicación, después de table_app_menu.sql.
-- Registra el padre y sus diez hijos; no modifica permisos de los perfiles existentes.
-- Asignar RP000000 y los hijos necesarios desde Usuarios > Bandeja de perfiles.
SET NAMES utf8mb4;

INSERT INTO app_menu
    (MenuCod, Name, Description, IsMenuDad, MenuDadCod, CreationUser, Status)
VALUES
    ('RP000000', 'Reportes', 'Módulo de reportes', 'S', NULL, 'CENTRAL', 'A'),
    ('RP000001', 'Reporte de ventas', 'Reporte de ventas', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000002', 'Reporte de productos vendidos', 'Reporte de productos vendidos', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000003', 'Reporte de inventario / stock', 'Reporte de inventario / stock', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000004', 'Reporte de medios de pago', 'Reporte de medios de pago', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000005', 'Reporte de comprobantes', 'Reporte de comprobantes', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000006', 'Reporte de clientes', 'Reporte de clientes', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000007', 'Reporte de pedidos', 'Reporte de pedidos', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000008', 'Reporte de devoluciones y notas de crédito', 'Reporte de devoluciones y notas de crédito', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000009', 'Utilidad por venta', 'Utilidad por venta', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000010', 'Compras vs. ventas', 'Comparación de compras de mercadería y ventas del período', 'N', 'RP000000', 'CENTRAL', 'A')
ON DUPLICATE KEY UPDATE
    Name = VALUES(Name),
    Description = VALUES(Description),
    IsMenuDad = VALUES(IsMenuDad),
    MenuDadCod = VALUES(MenuDadCod),
    ModifyUser = 'CENTRAL',
    ModifyDate = CURRENT_TIMESTAMP,
    Status = 'A';
