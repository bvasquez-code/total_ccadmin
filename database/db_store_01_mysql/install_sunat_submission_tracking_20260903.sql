-- Ejecutar despues de table_store.sql, table_sequence.sql,
-- table_app_menu.sql, table_profile_menu.sql y table_sunat_submission.sql.

INSERT INTO `table_sequence`
    (`SequenceTrx`, `Prefix`, `SequenceTableType`, `length`, `UsePrefix`)
VALUES
    (0, 'ES', 'sunat_submission', 20, 'S')
ON DUPLICATE KEY UPDATE
    `Prefix` = VALUES(`Prefix`),
    `length` = VALUES(`length`),
    `UsePrefix` = VALUES(`UsePrefix`);

INSERT INTO `app_menu`
    (`MenuCod`, `Name`, `Description`, `IsMenuDad`, `MenuDadCod`, `CreationUser`, `CreationDate`, `Status`)
VALUES
    ('SE000003', 'Envios SUNAT', 'Bandeja de seguimiento y reenvio manual de documentos SUNAT', 'N', 'SE000000', 'SYSTEM', NOW(), 'A')
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
SELECT parent.`ProfileCod`, 'SE000003', 'SYSTEM', NOW(), 'A'
FROM `profile_menu` parent
WHERE parent.`MenuCod` = 'SE000000'
  AND parent.`Status` = 'A'
ON DUPLICATE KEY UPDATE
    `ModifyUser` = 'SYSTEM',
    `ModifyDate` = NOW(),
    `Status` = 'A';
