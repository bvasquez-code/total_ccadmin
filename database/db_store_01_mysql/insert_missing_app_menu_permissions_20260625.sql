SET @ProfileCod = 'root';

INSERT INTO `app_menu`
(`MenuCod`, `Name`, `Description`, `IsMenuDad`, `MenuDadCod`, `CreationUser`, `Status`)
VALUES
('SI000005', 'Monedas', 'Monedas', 'N', 'SI000000', 'CENTRAL', 'A'),
('SI000006', 'Crear moneda', 'Crear moneda', 'N', 'SI000000', 'CENTRAL', 'A'),
('SI000007', 'Metodos de pago', 'Metodos de pago', 'N', 'SI000000', 'CENTRAL', 'A'),
('SI000008', 'Crear metodo de pago', 'Crear metodo de pago', 'N', 'SI000000', 'CENTRAL', 'A'),
('SI000009', 'Grupos de configuracion', 'Grupos de configuracion', 'N', 'SI000000', 'CENTRAL', 'A'),
('SI000010', 'Crear grupo de configuracion', 'Crear grupo de configuracion', 'N', 'SI000000', 'CENTRAL', 'A'),
('SI000011', 'Administrar valores de configuracion', 'Administrar valores de configuracion', 'N', 'SI000000', 'CENTRAL', 'A'),
('CJ000004', 'Talonarios', 'Talonarios', 'N', 'CJ000000', 'CENTRAL', 'A'),
('CJ000005', 'Crear/Editar Talonario', 'Crear/Editar Talonario', 'N', 'CJ000000', 'CENTRAL', 'A'),
('VT000006', 'Nota de credito', 'Nota de credito', 'N', 'VT000000', 'CENTRAL', 'A'),
('VT000007', 'Crear nota de credito', 'Crear nota de credito', 'N', 'VT000000', 'CENTRAL', 'A'),
('VT000008', 'Devolver stock de nota de credito', 'Devolver stock de nota de credito', 'N', 'VT000000', 'CENTRAL', 'A'),
('VT000009', 'Ver nota de credito', 'Ver nota de credito', 'N', 'VT000000', 'CENTRAL', 'A')
ON DUPLICATE KEY UPDATE
`Name` = VALUES(`Name`),
`Description` = VALUES(`Description`),
`IsMenuDad` = VALUES(`IsMenuDad`),
`MenuDadCod` = VALUES(`MenuDadCod`),
`ModifyUser` = 'CENTRAL',
`Status` = 'A';

INSERT INTO `profile_menu`
(`ProfileCod`, `MenuCod`, `CreationUser`, `Status`)
SELECT @ProfileCod, missing_menu.MenuCod, 'CENTRAL', 'A'
FROM (
    SELECT 'SI000005' AS MenuCod UNION ALL
    SELECT 'SI000006' UNION ALL
    SELECT 'SI000007' UNION ALL
    SELECT 'SI000008' UNION ALL
    SELECT 'SI000009' UNION ALL
    SELECT 'SI000010' UNION ALL
    SELECT 'SI000011' UNION ALL
    SELECT 'CJ000004' UNION ALL
    SELECT 'CJ000005' UNION ALL
    SELECT 'VT000006' UNION ALL
    SELECT 'VT000007' UNION ALL
    SELECT 'VT000008' UNION ALL
    SELECT 'VT000009'
) missing_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM `profile_menu` pm
    WHERE pm.ProfileCod = @ProfileCod
      AND pm.MenuCod = missing_menu.MenuCod
);

UPDATE `profile_menu`
SET `Status` = 'A',
    `ModifyUser` = 'CENTRAL'
WHERE `ProfileCod` = @ProfileCod
  AND `MenuCod` IN (
    'SI000005',
    'SI000006',
    'SI000007',
    'SI000008',
    'SI000009',
    'SI000010',
    'SI000011',
    'CJ000004',
    'CJ000005',
    'VT000006',
    'VT000007',
    'VT000008',
    'VT000009'
  );
