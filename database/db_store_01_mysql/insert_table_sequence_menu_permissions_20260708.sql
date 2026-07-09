SET @ProfileCod = 'root';

INSERT INTO `app_menu`
(`MenuCod`, `Name`, `Description`, `IsMenuDad`, `MenuDadCod`, `CreationUser`, `Status`)
VALUES
('SI000018', 'Secuencias globales', 'Secuencias globales', 'N', 'SI000000', 'CENTRAL', 'A'),
('SI000019', 'Crear secuencia global', 'Crear secuencia global', 'N', 'SI000000', 'CENTRAL', 'A')
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
    SELECT 'SI000018' AS MenuCod UNION ALL
    SELECT 'SI000019'
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
    'SI000018',
    'SI000019'
  );
