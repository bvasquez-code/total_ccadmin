-- Ejecutar despues de tables/table_table_sequence.sql,
-- tables/table_kardex.sql, tables/table_product_traceability.sql
-- y procedures/get_cod_seq.sql.

INSERT INTO `table_sequence`
    (`SequenceTrx`, `Prefix`, `SequenceTableType`, `length`, `UsePrefix`)
SELECT
    COALESCE(MAX(CAST(SUBSTRING(`TechnicalLot`, 3) AS UNSIGNED)), 0),
    'LT',
    'product_traceability',
    20,
    'S'
FROM `product_traceability`
WHERE `TechnicalLot` REGEXP '^LT[0-9]{18}$'
ON DUPLICATE KEY UPDATE
    `Prefix` = VALUES(`Prefix`),
    `length` = VALUES(`length`),
    `UsePrefix` = VALUES(`UsePrefix`);
