-- Motivo interno para la entrada excepcional directa que sigue a la
-- creacion rapida de un producto. Script idempotente para MySQL 8.

UPDATE `business_config`
SET
    `ConfigVal` = 'CARGA_INICIAL_TIENDA',
    `ConfigName` = 'Carga inicial de tienda',
    `ConfigDesc` = 'Entrada directa de stock posterior a la creacion rapida de un producto',
    `ModifyUser` = 'SYSTEM',
    `ModifyDate` = NOW(),
    `Status` = 'A'
WHERE `GroupId` = 8
  AND `ConfigCod` = 'CARGA_INICIAL_TIENDA';

INSERT INTO `business_config` (
    `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`,
    `ConfigName`, `ConfigDesc`, `CreationUser`, `CreationDate`, `Status`
)
SELECT
    8,
    source.`GroupCod`,
    source.`NextCorr`,
    'CARGA_INICIAL_TIENDA',
    'CARGA_INICIAL_TIENDA',
    'Carga inicial de tienda',
    'Entrada directa de stock posterior a la creacion rapida de un producto',
    'SYSTEM',
    NOW(),
    'A'
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
    WHERE `GroupId` = 8
      AND `ConfigCod` = 'CARGA_INICIAL_TIENDA'
);
