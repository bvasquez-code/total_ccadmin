-- Validacion de escenarios funcionales tributarios.
-- Este script no inserta ni modifica datos.
-- Reemplazar los codigos segun las ventas/notas creadas manualmente para QA.

SET @SaleCodIGV = '';
SET @SaleCodExonerated = '';
SET @SaleCodUnaffected = '';
SET @SaleCodExport = '';
SET @SaleCodISCIGV = '';
SET @SaleCodICBPER = '';
SET @CreditNoteCodPartial = '';

SELECT
    'Escenario 1 - producto gravado IGV 18' AS `Scenario`,
    CASE
        WHEN @SaleCodIGV = '' THEN 'SKIP'
        WHEN EXISTS (
            SELECT 1
            FROM `sale_det_tax` sdt
            WHERE sdt.`SaleCod` = @SaleCodIGV
              AND sdt.`TaxCod` = '1000'
              AND sdt.`TaxAffectationCod` = '10'
              AND sdt.`TaxCalculationType` = 'P'
              AND ROUND(sdt.`TaxRateValue`, 2) = 18.00
              AND sdt.`TaxAmount` > 0
              AND sdt.`Status` = 'A'
        ) THEN 'OK'
        ELSE 'FAIL'
    END AS `Result`
UNION ALL
SELECT
    'Escenario 2 - producto exonerado',
    CASE
        WHEN @SaleCodExonerated = '' THEN 'SKIP'
        WHEN EXISTS (
            SELECT 1
            FROM `sale_det_tax` sdt
            JOIN `sale_head` sh ON sh.`SaleCod` = sdt.`SaleCod`
            WHERE sdt.`SaleCod` = @SaleCodExonerated
              AND sdt.`TaxCod` = '9997'
              AND sdt.`TaxAffectationCod` = '20'
              AND sdt.`IsInformative` = 'S'
              AND sdt.`TaxAmount` = 0.00
              AND sh.`NumTotalTax` = 0.00
              AND sdt.`Status` = 'A'
        ) THEN 'OK'
        ELSE 'FAIL'
    END
UNION ALL
SELECT
    'Escenario 3 - producto inafecto',
    CASE
        WHEN @SaleCodUnaffected = '' THEN 'SKIP'
        WHEN EXISTS (
            SELECT 1
            FROM `sale_det_tax` sdt
            JOIN `sale_head` sh ON sh.`SaleCod` = sdt.`SaleCod`
            WHERE sdt.`SaleCod` = @SaleCodUnaffected
              AND sdt.`TaxCod` = '9998'
              AND sdt.`TaxAffectationCod` = '30'
              AND sdt.`IsInformative` = 'S'
              AND sdt.`TaxAmount` = 0.00
              AND sh.`NumTotalTax` = 0.00
              AND sdt.`Status` = 'A'
        ) THEN 'OK'
        ELSE 'FAIL'
    END
UNION ALL
SELECT
    'Escenario 4 - producto exportacion',
    CASE
        WHEN @SaleCodExport = '' THEN 'SKIP'
        WHEN EXISTS (
            SELECT 1
            FROM `sale_det_tax` sdt
            JOIN `sale_head` sh ON sh.`SaleCod` = sdt.`SaleCod`
            WHERE sdt.`SaleCod` = @SaleCodExport
              AND sdt.`TaxCod` = '9995'
              AND sdt.`TaxAffectationCod` = '40'
              AND sdt.`IsInformative` = 'S'
              AND sdt.`TaxAmount` = 0.00
              AND sh.`NumTotalTax` = 0.00
              AND sdt.`Status` = 'A'
        ) THEN 'OK'
        ELSE 'FAIL'
    END
UNION ALL
SELECT
    'Escenario 5 - producto con ISC + IGV',
    CASE
        WHEN @SaleCodISCIGV = '' THEN 'SKIP'
        WHEN EXISTS (
            SELECT 1
            FROM `sale_det_tax` isc
            JOIN `sale_det_tax` igv
              ON igv.`SaleCod` = isc.`SaleCod`
             AND igv.`ItemNumber` = isc.`ItemNumber`
            WHERE isc.`SaleCod` = @SaleCodISCIGV
              AND isc.`TaxCod` = '2000'
              AND igv.`TaxCod` = '1000'
              AND igv.`TaxAffectationCod` = '10'
              AND isc.`TaxAmount` > 0
              AND igv.`TaxAmount` > 0
              AND isc.`CalculationOrder` <= igv.`CalculationOrder`
              AND igv.`TaxBaseAmount` >= isc.`TaxBaseAmount`
              AND isc.`Status` = 'A'
              AND igv.`Status` = 'A'
        ) THEN 'OK'
        ELSE 'FAIL'
    END
UNION ALL
SELECT
    'Escenario 6 - producto con ICBPER',
    CASE
        WHEN @SaleCodICBPER = '' THEN 'SKIP'
        WHEN EXISTS (
            SELECT 1
            FROM `sale_det_tax` sdt
            WHERE sdt.`SaleCod` = @SaleCodICBPER
              AND sdt.`TaxCod` = '7152'
              AND sdt.`TaxCalculationType` = 'F'
              AND sdt.`FixedUnitAmount` > 0
              AND sdt.`TaxQuantity` > 0
              AND ROUND(sdt.`FixedUnitAmount` * sdt.`TaxQuantity`, 2) = sdt.`TaxAmount`
              AND sdt.`Status` = 'A'
        ) THEN 'OK'
        ELSE 'FAIL'
    END
UNION ALL
SELECT
    'Escenario 7 - no permitir IGV con afectacion no gravada',
    CASE
        WHEN NOT EXISTS (
            SELECT 1
            FROM `product_tax_config` ptc
            WHERE ptc.`Status` = 'A'
              AND ptc.`TaxCod` = '1000'
              AND COALESCE(ptc.`TaxAffectationCod`, '') <> '10'
        ) THEN 'OK'
        ELSE 'FAIL'
    END
UNION ALL
SELECT
    'Escenario 8 - no duplicar tributo activo por producto/local',
    CASE
        WHEN NOT EXISTS (
            SELECT 1
            FROM `product_tax_config` ptc
            WHERE ptc.`Status` = 'A'
            GROUP BY ptc.`ProductCod`, ptc.`StoreCod`, ptc.`TaxCod`
            HAVING COUNT(*) > 1
        ) THEN 'OK'
        ELSE 'FAIL'
    END
UNION ALL
SELECT
    'Nota de credito parcial - tributos proporcionales',
    CASE
        WHEN @CreditNoteCodPartial = '' THEN 'SKIP'
        WHEN NOT EXISTS (
            SELECT 1
            FROM `credit_note_head` cnh
            JOIN `credit_note_det` cnd
              ON cnd.`CreditNoteCod` = cnh.`CreditNoteCod`
             AND cnd.`Status` = 'A'
            JOIN `sale_det` sd
              ON sd.`SaleCod` = cnh.`SaleCod`
             AND sd.`ItemNumber` = cnd.`ItemNumber`
             AND sd.`Status` = 'A'
            JOIN `credit_note_det_tax` cndt
              ON cndt.`CreditNoteCod` = cnd.`CreditNoteCod`
             AND cndt.`ItemNumber` = cnd.`ItemNumber`
             AND cndt.`Status` = 'A'
            JOIN `sale_det_tax` sdt
              ON sdt.`SaleCod` = sd.`SaleCod`
             AND sdt.`ItemNumber` = sd.`ItemNumber`
             AND sdt.`TaxCod` = cndt.`TaxCod`
             AND sdt.`Status` = 'A'
            WHERE cnh.`CreditNoteCod` = @CreditNoteCodPartial
              AND ABS(ROUND(cndt.`TaxAmount` - ROUND(sdt.`TaxAmount` * (cnd.`NumUnit` / sd.`NumUnit`), 2), 2)) > 0.01
        ) THEN 'OK'
        ELSE 'FAIL'
    END;

-- Detalle de las ventas usadas en los escenarios configurados.
SELECT
    sh.`SaleCod`,
    sh.`NumTotalPriceNoTax`,
    sh.`NumTotalTax`,
    sh.`NumTotalPrice`,
    sd.`ItemNumber`,
    sd.`ProductCod`,
    sd.`NumUnit`,
    sd.`NumPriceSubTotal`,
    sd.`NumTotalTax` AS `DetailTax`,
    sdt.`TaxLineNumber`,
    sdt.`TaxCod`,
    sdt.`TaxAffectationCod`,
    sdt.`TaxCalculationType`,
    sdt.`TaxRateValue`,
    sdt.`FixedUnitAmount`,
    sdt.`TaxBaseAmount`,
    sdt.`TaxQuantity`,
    sdt.`TaxAmount`
FROM `sale_head` sh
JOIN `sale_det` sd ON sd.`SaleCod` = sh.`SaleCod` AND sd.`Status` = 'A'
LEFT JOIN `sale_det_tax` sdt
    ON sdt.`SaleCod` = sd.`SaleCod`
   AND sdt.`ItemNumber` = sd.`ItemNumber`
   AND sdt.`Status` = 'A'
WHERE sh.`SaleCod` IN (
    @SaleCodIGV,
    @SaleCodExonerated,
    @SaleCodUnaffected,
    @SaleCodExport,
    @SaleCodISCIGV,
    @SaleCodICBPER
)
ORDER BY sh.`SaleCod`, sd.`ItemNumber`, sdt.`TaxLineNumber`;
