INSERT INTO `app_menu`
(`MenuCod`, `Name`, `Description`, `IsMenuDad`, `MenuDadCod`, `CreationUser`, `Status`)
VALUES
('VT000010', 'Facturación Web', 'Bandeja de pedidos originados en la tienda virtual', 'N', 'VT000000', 'CENTRAL', 'A')
ON DUPLICATE KEY UPDATE
`Name` = VALUES(`Name`),
`Description` = VALUES(`Description`),
`IsMenuDad` = VALUES(`IsMenuDad`),
`MenuDadCod` = VALUES(`MenuDadCod`),
`ModifyUser` = 'CENTRAL',
`Status` = 'A';

-- El permiso VT000010 debe asignarse desde la administración de perfiles.
-- Este script no concede el acceso automáticamente a ningún perfil.
