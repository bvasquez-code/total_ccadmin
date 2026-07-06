-- Ejecutar despues de database/db_store_01_mysql/tables/table_sale_det.sql.
-- Regulariza subtotal e impuesto por detalle usando el porcentaje guardado por venta.

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_sale_det_tax_rate;
CREATE TEMPORARY TABLE tmp_sale_det_tax_rate AS
SELECT
    `SaleCod`,
    SUM(`TaxRateValue`) AS `TaxRateValue`
FROM `sale_applied_tax`
WHERE `Status` = 'A'
GROUP BY `SaleCod`;

SELECT
    COUNT(*) AS `ActiveSaleDetailsWithoutAppliedTax`
FROM `sale_det` sd
LEFT JOIN tmp_sale_det_tax_rate tr ON tr.`SaleCod` = sd.`SaleCod`
WHERE sd.`Status` = 'A'
  AND COALESCE(sd.`IsAppliedTax`, 'S') = 'S'
  AND tr.`SaleCod` IS NULL;

UPDATE `sale_det` sd
LEFT JOIN tmp_sale_det_tax_rate tr ON tr.`SaleCod` = sd.`SaleCod`
SET
    sd.`NumPriceSubTotal` = CASE
        WHEN COALESCE(sd.`IsAppliedTax`, 'S') = 'S'
             AND COALESCE(tr.`TaxRateValue`, 0) > 0
            THEN ROUND(COALESCE(sd.`NumTotalPrice`, 0) / (1 + (COALESCE(tr.`TaxRateValue`, 0) / 100)), 2)
        ELSE ROUND(COALESCE(sd.`NumTotalPrice`, 0), 2)
    END,
    sd.`NumTotalTax` = CASE
        WHEN COALESCE(sd.`IsAppliedTax`, 'S') = 'S'
             AND COALESCE(tr.`TaxRateValue`, 0) > 0
            THEN ROUND(
                COALESCE(sd.`NumTotalPrice`, 0)
                - ROUND(COALESCE(sd.`NumTotalPrice`, 0) / (1 + (COALESCE(tr.`TaxRateValue`, 0) / 100)), 2),
                2
            )
        ELSE 0.00
    END,
    sd.`ModifyUser` = 'SISTEMA',
    sd.`ModifyDate` = NOW()
WHERE sd.`Status` = 'A';

DROP TEMPORARY TABLE IF EXISTS tmp_sale_det_tax_sum;
CREATE TEMPORARY TABLE tmp_sale_det_tax_sum AS
SELECT
    sd.`SaleCod`,
    COUNT(*) AS `DetailCount`,
    ROUND(SUM(sd.`NumPriceSubTotal`), 2) AS `NumPriceSubTotal`,
    ROUND(SUM(sd.`NumTotalTax`), 2) AS `NumTotalTax`
FROM `sale_det` sd
WHERE sd.`Status` = 'A'
GROUP BY sd.`SaleCod`;

DROP TEMPORARY TABLE IF EXISTS tmp_sale_det_tax_last;
CREATE TEMPORARY TABLE tmp_sale_det_tax_last AS
SELECT
    x.`SaleCod`,
    x.`ItemNumber`
FROM (
    SELECT
        sd.`SaleCod`,
        sd.`ItemNumber`,
        ROW_NUMBER() OVER (PARTITION BY sd.`SaleCod` ORDER BY sd.`ItemNumber` DESC) AS rn
    FROM `sale_det` sd
    WHERE sd.`Status` = 'A'
) x
WHERE x.rn = 1;

UPDATE `sale_det` sd
JOIN tmp_sale_det_tax_last last_det
    ON last_det.`SaleCod` = sd.`SaleCod`
   AND last_det.`ItemNumber` = sd.`ItemNumber`
JOIN tmp_sale_det_tax_sum tax_sum
    ON tax_sum.`SaleCod` = sd.`SaleCod`
JOIN `sale_head` sh
    ON sh.`SaleCod` = sd.`SaleCod`
SET
    sd.`NumPriceSubTotal` = ROUND(sd.`NumPriceSubTotal` + ROUND(sh.`NumTotalPriceNoTax` - tax_sum.`NumPriceSubTotal`, 2), 2),
    sd.`NumTotalTax` = ROUND(sd.`NumTotalTax` + ROUND(sh.`NumTotalTax` - tax_sum.`NumTotalTax`, 2), 2),
    sd.`ModifyUser` = 'SISTEMA',
    sd.`ModifyDate` = NOW()
WHERE sh.`Status` = 'A'
  AND ABS(ROUND(sh.`NumTotalPriceNoTax` - tax_sum.`NumPriceSubTotal`, 2)) <= (tax_sum.`DetailCount` * 0.01)
  AND ABS(ROUND(sh.`NumTotalTax` - tax_sum.`NumTotalTax`, 2)) <= (tax_sum.`DetailCount` * 0.01);

SELECT
    sh.`SaleCod`,
    sh.`NumTotalPriceNoTax` AS `HeadSubTotal`,
    ROUND(SUM(sd.`NumPriceSubTotal`), 2) AS `DetailSubTotal`,
    ROUND(sh.`NumTotalPriceNoTax` - SUM(sd.`NumPriceSubTotal`), 2) AS `SubTotalDifference`,
    sh.`NumTotalTax` AS `HeadTax`,
    ROUND(SUM(sd.`NumTotalTax`), 2) AS `DetailTax`,
    ROUND(sh.`NumTotalTax` - SUM(sd.`NumTotalTax`), 2) AS `TaxDifference`
FROM `sale_head` sh
JOIN `sale_det` sd ON sd.`SaleCod` = sh.`SaleCod`
WHERE sh.`Status` = 'A'
  AND sd.`Status` = 'A'
GROUP BY sh.`SaleCod`, sh.`NumTotalPriceNoTax`, sh.`NumTotalTax`
HAVING `SubTotalDifference` <> 0.00
    OR `TaxDifference` <> 0.00
ORDER BY sh.`SaleCod`
LIMIT 50;

COMMIT;
