-- MySQL 8. Reportes de mercadería. Reejecutable; mantiene los permisos existentes.
-- Asignar RP000010 desde Usuarios > Bandeja de perfiles para habilitar Compras vs. ventas.
SET NAMES utf8mb4;

INSERT INTO app_menu
    (MenuCod, Name, Description, IsMenuDad, MenuDadCod, CreationUser, Status)
VALUES
    ('RP000000', 'Reportes', 'Módulo de reportes', 'S', NULL, 'CENTRAL', 'A'),
    ('RP000009', 'Utilidad por venta', 'Venta menos costo trazado de las unidades vendidas', 'N', 'RP000000', 'CENTRAL', 'A'),
    ('RP000010', 'Compras vs. ventas', 'Compras de mercadería y ventas del período', 'N', 'RP000000', 'CENTRAL', 'A')
ON DUPLICATE KEY UPDATE
    Name = VALUES(Name), Description = VALUES(Description),
    IsMenuDad = VALUES(IsMenuDad), MenuDadCod = VALUES(MenuDadCod),
    ModifyUser = 'CENTRAL', ModifyDate = CURRENT_TIMESTAMP, Status = 'A';
