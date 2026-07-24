-- Ejecutar despues de:
-- table_app_file.sql, table_store.sql, table_product.sql, table_product_config.sql,
-- table_bulk_load_head.sql, table_bulk_load_destination.sql, table_bulk_load_det.sql,
-- table_product_price_history.sql y trg_product_config_price_history.sql.

INSERT INTO `table_sequence`
    (`SequenceTrx`, `Prefix`, `SequenceTableType`, `length`, `UsePrefix`)
VALUES
    (0, 'CM', 'bulk_load_head', 16, 'S')
ON DUPLICATE KEY UPDATE
    `Prefix` = VALUES(`Prefix`),
    `length` = VALUES(`length`),
    `UsePrefix` = VALUES(`UsePrefix`);

INSERT INTO `app_menu`
    (`MenuCod`, `Name`, `Description`, `IsMenuDad`, `MenuDadCod`, `CreationUser`, `CreationDate`, `Status`)
VALUES
    ('AT000013', 'Cargas masivas', 'Bandeja de procesos de carga masiva', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
    ('AT000014', 'Nueva carga masiva', 'Validacion y confirmacion de una carga masiva', 'N', 'AT000000', 'SYSTEM', NOW(), 'A')
ON DUPLICATE KEY UPDATE
    `Name` = VALUES(`Name`),
    `Description` = VALUES(`Description`),
    `IsMenuDad` = VALUES(`IsMenuDad`),
    `MenuDadCod` = VALUES(`MenuDadCod`),
    `ModifyUser` = 'SYSTEM',
    `ModifyDate` = NOW(),
    `Status` = 'A';

INSERT INTO `profile_menu`
    (`ProfileCod`, `MenuCod`, `CreationUser`, `CreationDate`, `Status`)
SELECT parent.`ProfileCod`, child.`MenuCod`, 'SYSTEM', NOW(), 'A'
FROM `profile_menu` parent
JOIN `app_menu` child ON child.`MenuCod` IN ('AT000013', 'AT000014')
WHERE parent.`MenuCod` = 'AT000000'
  AND parent.`Status` = 'A'
ON DUPLICATE KEY UPDATE
    `ModifyUser` = 'SYSTEM',
    `ModifyDate` = NOW(),
    `Status` = 'A';

-- Motivo especifico de entrada directa para los documentos creados por la carga.
-- Se hereda el GroupCod real configurado para GroupId=8.
INSERT INTO `business_config` (
    `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`,
    `ConfigName`, `ConfigDesc`, `CreationUser`, `CreationDate`, `Status`
)
SELECT
    8, source.`GroupCod`, source.`NextCorr`, 'CARGA_MASIVA_STOCK', 'CARGA_MASIVA_STOCK',
    'Carga masiva de stock', 'Entrada directa generada por el modulo de carga masiva',
    'SYSTEM', NOW(), 'A'
FROM (
    SELECT `GroupCod`, MAX(`ConfigCorr`) + 1 AS `NextCorr`
    FROM `business_config`
    WHERE `GroupId` = 8
    GROUP BY `GroupCod`
    ORDER BY `GroupCod`
    LIMIT 1
) source
WHERE NOT EXISTS (
    SELECT 1
    FROM `business_config`
    WHERE `ConfigCod` = 'CARGA_MASIVA_STOCK'
);
