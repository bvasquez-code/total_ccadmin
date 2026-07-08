-- Validaciones manuales para configuracion tributaria extensible.
-- Ejecutar despues de crear ventas/notas de prueba.

-- 1. Producto/local sin configuracion tributaria activa.
SELECT
    pc.`ProductCod`,
    pc.`StoreCod`
FROM `product_config` pc
LEFT JOIN `product_tax_config` ptc
    ON ptc.`ProductCod` = pc.`ProductCod`
   AND ptc.`StoreCod` = pc.`StoreCod`
   AND ptc.`Status` = 'A'
WHERE ptc.`ProductTaxConfigId` IS NULL
LIMIT 50;

-- 2. Producto/local sin una unica afectacion principal activa.
SELECT
    ptc.`ProductCod`,
    ptc.`StoreCod`,
    SUM(CASE WHEN ptc.`IsMainTax` = 'S' THEN 1 ELSE 0 END) AS `MainTaxCount`,
    COUNT(*) AS `ActiveTaxCount`
FROM `product_tax_config` ptc
WHERE ptc.`Status` = 'A'
GROUP BY ptc.`ProductCod`, ptc.`StoreCod`
HAVING `MainTaxCount` <> 1
LIMIT 50;

-- 3. Duplicados de tributo activo para el mismo producto/local.
SELECT
    ptc.`ProductCod`,
    ptc.`StoreCod`,
    ptc.`TaxCod`,
    COUNT(*) AS `ActiveDuplicatedCount`
FROM `product_tax_config` ptc
WHERE ptc.`Status` = 'A'
GROUP BY ptc.`ProductCod`, ptc.`StoreCod`, ptc.`TaxCod`
HAVING COUNT(*) > 1
LIMIT 50;

-- 4. Combinaciones invalidas de tributo principal y afectacion.
SELECT
    ptc.`ProductTaxConfigId`,
    ptc.`ProductCod`,
    ptc.`StoreCod`,
    ptc.`TaxCod`,
    ptc.`TaxAffectationCod`
FROM `product_tax_config` ptc
LEFT JOIN `tax_affectation` ta
    ON ta.`TaxAffectationCod` = ptc.`TaxAffectationCod`
   AND ta.`TaxCod` = ptc.`TaxCod`
   AND ta.`Status` = 'A'
WHERE ptc.`Status` = 'A'
  AND ptc.`IsMainTax` = 'S'
  AND ta.`TaxAffectationCod` IS NULL
LIMIT 50;

-- 5. Tributos de afectacion IGV configurados como adicionales.
SELECT
    ptc.`ProductTaxConfigId`,
    ptc.`ProductCod`,
    ptc.`StoreCod`,
    ptc.`TaxCod`
FROM `product_tax_config` ptc
JOIN `tax_affectation` ta
    ON ta.`TaxCod` = ptc.`TaxCod`
   AND ta.`Status` = 'A'
WHERE ptc.`Status` = 'A'
  AND ptc.`IsMainTax` <> 'S'
LIMIT 50;

-- 6. Ventas con detalle sin desglose tributario.
SELECT
    sd.`SaleCod`,
    sd.`ItemNumber`,
    sd.`ProductCod`,
    sd.`NumTotalPrice`,
    sd.`NumPriceSubTotal`,
    sd.`NumTotalTax`
FROM `sale_det` sd
LEFT JOIN `sale_det_tax` sdt
    ON sdt.`SaleCod` = sd.`SaleCod`
   AND sdt.`ItemNumber` = sd.`ItemNumber`
   AND sdt.`Status` = 'A'
WHERE sd.`Status` = 'A'
  AND sdt.`SaleCod` IS NULL
ORDER BY sd.`SaleCod`, sd.`ItemNumber`
LIMIT 50;

-- 7. Ventas cuyo resumen del detalle no cuadra con su desglose.
SELECT
    sd.`SaleCod`,
    sd.`ItemNumber`,
    sd.`NumTotalTax` AS `DetailTax`,
    ROUND(SUM(sdt.`TaxAmount`), 2) AS `TaxBreakdown`,
    ROUND(sd.`NumTotalTax` - SUM(sdt.`TaxAmount`), 2) AS `TaxDifference`
FROM `sale_det` sd
JOIN `sale_det_tax` sdt
    ON sdt.`SaleCod` = sd.`SaleCod`
   AND sdt.`ItemNumber` = sd.`ItemNumber`
   AND sdt.`Status` = 'A'
WHERE sd.`Status` = 'A'
GROUP BY sd.`SaleCod`, sd.`ItemNumber`, sd.`NumTotalTax`
HAVING `TaxDifference` <> 0.00
ORDER BY sd.`SaleCod`, sd.`ItemNumber`
LIMIT 50;

-- 8. Notas de credito cuyo resumen del detalle no cuadra con su desglose.
SELECT
    cnd.`CreditNoteCod`,
    cnd.`ItemNumber`,
    cnd.`NumTotalTax` AS `DetailTax`,
    ROUND(SUM(cndt.`TaxAmount`), 2) AS `TaxBreakdown`,
    ROUND(cnd.`NumTotalTax` - SUM(cndt.`TaxAmount`), 2) AS `TaxDifference`
FROM `credit_note_det` cnd
JOIN `credit_note_det_tax` cndt
    ON cndt.`CreditNoteCod` = cnd.`CreditNoteCod`
   AND cndt.`ItemNumber` = cnd.`ItemNumber`
   AND cndt.`Status` = 'A'
WHERE cnd.`Status` = 'A'
GROUP BY cnd.`CreditNoteCod`, cnd.`ItemNumber`, cnd.`NumTotalTax`
HAVING `TaxDifference` <> 0.00
ORDER BY cnd.`CreditNoteCod`, cnd.`ItemNumber`
LIMIT 50;
