-- Ejecutar despues de:
--   tables/table_tax.sql
--   tables/table_tax_affectation.sql
--   tables/table_product_tax_config.sql
--   tables/table_sale_det.sql
--   tables/table_sale_det_tax.sql
-- Opcionalmente ejecutar despues de regularize_sale_det_tax_amounts_20260705.sql
-- para que el desglose historico use subtotales ya regularizados.

START TRANSACTION;

INSERT INTO `tax` (
    `TaxCod`, `SunatTaxCod`, `TaxRateValue`, `FixedUnitAmount`, `TaxCalculationType`,
    `IsInformative`, `CalculationOrder`, `Name`, `Description`,
    `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`
)
VALUES
    ('1000', '1000', 18.0000, 0.0000, 'P', 'N', 20, 'IGV', 'Impuesto general a las ventas', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('2000', '2000', 0.0000, 0.0000, 'P', 'N', 10, 'ISC', 'Impuesto selectivo al consumo', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('7152', '7152', 0.0000, 0.0000, 'F', 'N', 90, 'ICBPER', 'Impuesto bolsa plastica por unidad', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('9995', '9995', 0.0000, 0.0000, 'N', 'S', 20, 'EXPORTACION', 'Exportacion informativa', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('9997', '9997', 0.0000, 0.0000, 'N', 'S', 20, 'EXONERADO', 'Operacion exonerada informativa', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('9998', '9998', 0.0000, 0.0000, 'N', 'S', 20, 'INAFECTO', 'Operacion inafecta informativa', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('9999', '9999', 0.0000, 0.0000, 'N', 'S', 100, 'OTROS', 'Otros conceptos tributarios futuros', 'SISTEMA', NOW(), NULL, NOW(), 'A')
ON DUPLICATE KEY UPDATE
    `SunatTaxCod` = VALUES(`SunatTaxCod`),
    `TaxRateValue` = VALUES(`TaxRateValue`),
    `FixedUnitAmount` = VALUES(`FixedUnitAmount`),
    `TaxCalculationType` = VALUES(`TaxCalculationType`),
    `IsInformative` = VALUES(`IsInformative`),
    `CalculationOrder` = VALUES(`CalculationOrder`),
    `Name` = VALUES(`Name`),
    `Description` = VALUES(`Description`),
    `ModifyUser` = 'SISTEMA',
    `ModifyDate` = NOW(),
    `Status` = 'A';

INSERT INTO `tax_affectation` (
    `TaxAffectationCod`, `TaxCod`, `Name`, `Description`, `IsTaxed`,
    `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`
)
VALUES
    ('10', '1000', 'Gravado - operacion onerosa', 'Afectacion IGV gravada onerosa', 'S', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('20', '9997', 'Exonerado - operacion onerosa', 'Afectacion exonerada onerosa', 'N', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('30', '9998', 'Inafecto - operacion onerosa', 'Afectacion inafecta onerosa', 'N', 'SISTEMA', NOW(), NULL, NOW(), 'A'),
    ('40', '9995', 'Exportacion', 'Afectacion de exportacion', 'N', 'SISTEMA', NOW(), NULL, NOW(), 'A')
ON DUPLICATE KEY UPDATE
    `TaxCod` = VALUES(`TaxCod`),
    `Name` = VALUES(`Name`),
    `Description` = VALUES(`Description`),
    `IsTaxed` = VALUES(`IsTaxed`),
    `ModifyUser` = 'SISTEMA',
    `ModifyDate` = NOW(),
    `Status` = 'A';

INSERT INTO `product_tax_config` (
    `ProductCod`, `StoreCod`, `TaxCod`, `TaxAffectationCod`, `IsMainTax`,
    `TaxRateValue`, `FixedUnitAmount`, `TaxCalculationType`, `IsInformative`,
    `CalculationOrder`, `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`
)
SELECT
    pc.`ProductCod`,
    pc.`StoreCod`,
    '1000',
    '10',
    'S',
    18.0000,
    0.0000,
    'P',
    'N',
    20,
    'SISTEMA',
    NOW(),
    NULL,
    NOW(),
    'A'
FROM `product_config` pc
WHERE pc.`Status` = 'A'
  AND NOT EXISTS (
      SELECT 1
      FROM `product_tax_config` ptc
      WHERE ptc.`ProductCod` = pc.`ProductCod`
        AND ptc.`StoreCod` = pc.`StoreCod`
        AND ptc.`Status` = 'A'
  );

INSERT INTO `sale_det_tax` (
    `SaleCod`, `ItemNumber`, `TaxLineNumber`, `TaxCod`, `SunatTaxCod`, `TaxName`,
    `TaxAffectationCod`, `TaxAffectationName`, `TaxCalculationType`, `IsInformative`,
    `TaxRateValue`, `FixedUnitAmount`, `TaxBaseAmount`, `TaxQuantity`, `TaxAmount`,
    `CalculationOrder`, `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`
)
SELECT
    sd.`SaleCod`,
    sd.`ItemNumber`,
    1,
    t.`TaxCod`,
    t.`SunatTaxCod`,
    t.`Name`,
    ta.`TaxAffectationCod`,
    ta.`Name`,
    t.`TaxCalculationType`,
    t.`IsInformative`,
    t.`TaxRateValue`,
    t.`FixedUnitAmount`,
    CASE
        WHEN COALESCE(sd.`NumPriceSubTotal`, 0) > 0 THEN ROUND(sd.`NumPriceSubTotal`, 2)
        ELSE ROUND(COALESCE(sd.`NumTotalPrice`, 0), 2)
    END,
    COALESCE(sd.`NumUnit`, 0),
    CASE WHEN COALESCE(sd.`IsAppliedTax`, 'S') = 'S' THEN ROUND(COALESCE(sd.`NumTotalTax`, 0), 2) ELSE 0.00 END,
    t.`CalculationOrder`,
    'SISTEMA',
    NOW(),
    NULL,
    NOW(),
    'A'
FROM `sale_det` sd
JOIN `tax_affectation` ta
    ON ta.`TaxAffectationCod` = CASE WHEN COALESCE(sd.`IsAppliedTax`, 'S') = 'S' THEN '10' ELSE '30' END
JOIN `tax` t
    ON t.`TaxCod` = ta.`TaxCod`
WHERE sd.`Status` = 'A'
  AND NOT EXISTS (
      SELECT 1
      FROM `sale_det_tax` sdt
      WHERE sdt.`SaleCod` = sd.`SaleCod`
        AND sdt.`ItemNumber` = sd.`ItemNumber`
        AND sdt.`TaxLineNumber` = 1
  );

SELECT
    COUNT(*) AS `ProductStoreWithoutMainTaxConfig`
FROM `product_config` pc
LEFT JOIN `product_tax_config` ptc
    ON ptc.`ProductCod` = pc.`ProductCod`
   AND ptc.`StoreCod` = pc.`StoreCod`
   AND ptc.`IsMainTax` = 'S'
   AND ptc.`Status` = 'A'
WHERE pc.`Status` = 'A'
  AND ptc.`ProductTaxConfigId` IS NULL;

SELECT
    COUNT(*) AS `ActiveSaleDetailsWithoutTaxBreakdown`
FROM `sale_det` sd
LEFT JOIN `sale_det_tax` sdt
    ON sdt.`SaleCod` = sd.`SaleCod`
   AND sdt.`ItemNumber` = sd.`ItemNumber`
   AND sdt.`Status` = 'A'
WHERE sd.`Status` = 'A'
  AND sdt.`SaleCod` IS NULL;

COMMIT;
