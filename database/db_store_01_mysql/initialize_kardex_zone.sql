-- Ejecutar despues de:
--   tables/table_kardex_zone.sql
--   tables/table_product_info.sql
--   tables/table_product_info_warehouse.sql
--   tables/table_warehouse.sql
--
-- Crea un movimiento inicial 0 -> saldo actual por cada zona con stock.
-- Es idempotente: no duplica una inicializacion ya registrada.

DROP PROCEDURE IF EXISTS `p_initialize_kardex_zone`;

DELIMITER $$

CREATE PROCEDURE `p_initialize_kardex_zone`()
BEGIN
    DECLARE v_inconsistent_products INT DEFAULT 0;
    DECLARE v_orphan_warehouses INT DEFAULT 0;
    DECLARE v_inserted_rows INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF NOT EXISTS (
        SELECT * FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'kardex_zone'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Debe crear la tabla kardex_zone antes de inicializarla';
    END IF;

    SELECT COUNT(*) INTO v_inconsistent_products
    FROM (
        SELECT pi.ProductCod, pi.Variant, pi.StoreCod
        FROM product_info pi
        LEFT JOIN (
            SELECT
                piw.ProductCod,
                piw.Variant,
                w.StoreCod,
                SUM(COALESCE(piw.NumPhysicalStock, 0)) AS NumPhysicalStock,
                SUM(piw.NumUnavailableStock) AS NumUnavailableStock,
                SUM(piw.NumReservedStock) AS NumReservedStock,
                SUM(piw.NumTotalStock) AS NumTotalStock
            FROM product_info_warehouse piw
            INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod
            GROUP BY piw.ProductCod, piw.Variant, w.StoreCod
        ) warehouse_stock
          ON warehouse_stock.ProductCod = pi.ProductCod
         AND warehouse_stock.Variant = pi.Variant
         AND warehouse_stock.StoreCod = pi.StoreCod
        WHERE COALESCE(pi.NumPhysicalStock, 0) <> COALESCE(warehouse_stock.NumPhysicalStock, 0)
           OR pi.NumUnavailableStock <> COALESCE(warehouse_stock.NumUnavailableStock, 0)
           OR pi.NumReservedStock <> COALESCE(warehouse_stock.NumReservedStock, 0)
           OR pi.NumTotalStock <> COALESCE(warehouse_stock.NumTotalStock, 0)
    ) inconsistent;

    SELECT COUNT(*) INTO v_orphan_warehouses
    FROM product_info_warehouse piw
    INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod
    LEFT JOIN product_info pi
      ON pi.ProductCod = piw.ProductCod
     AND pi.Variant = piw.Variant
     AND pi.StoreCod = w.StoreCod
    WHERE pi.ProductCod IS NULL;

    IF v_inconsistent_products > 0 OR v_orphan_warehouses > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Stock inconsistente entre product_info y product_info_warehouse';
    END IF;

    START TRANSACTION;

    INSERT INTO kardex_zone (
        OperationCod,
        ItemNumber,
        SourceTable,
        MovementEvent,
        ProductCod,
        Variant,
        StoreCod,
        WarehouseCod,
        ZoneStockMoved,
        TypeOperation,
        NumStockMoved,
        NumZoneStockBefore,
        NumZoneStockAfter,
        LotNumber,
        ExpirationDate,
        CreationUser,
        CreationDate,
        ModifyUser,
        ModifyDate,
        Status
    )
    SELECT
        '00000000',
        NULL,
        'no_table',
        'INITIALIZATION',
        stock.ProductCod,
        stock.Variant,
        stock.StoreCod,
        stock.WarehouseCod,
        stock.ZoneStockMoved,
        'S',
        stock.NumStock,
        0,
        stock.NumStock,
        NULL,
        NULL,
        'SYSTEM',
        NOW(),
        NULL,
        NOW(),
        'A'
    FROM (
        SELECT
            piw.ProductCod,
            piw.Variant,
            w.StoreCod,
            piw.WarehouseCod,
            'PHYSICAL' AS ZoneStockMoved,
            COALESCE(piw.NumPhysicalStock, 0) AS NumStock
        FROM product_info_warehouse piw
        INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod
        INNER JOIN product_info pi
          ON pi.ProductCod = piw.ProductCod
         AND pi.Variant = piw.Variant
         AND pi.StoreCod = w.StoreCod
        WHERE COALESCE(piw.NumPhysicalStock, 0) > 0

        UNION ALL

        SELECT
            piw.ProductCod,
            piw.Variant,
            w.StoreCod,
            piw.WarehouseCod,
            'RESERVED' AS ZoneStockMoved,
            piw.NumReservedStock AS NumStock
        FROM product_info_warehouse piw
        INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod
        INNER JOIN product_info pi
          ON pi.ProductCod = piw.ProductCod
         AND pi.Variant = piw.Variant
         AND pi.StoreCod = w.StoreCod
        WHERE piw.NumReservedStock > 0

        UNION ALL

        SELECT
            piw.ProductCod,
            piw.Variant,
            w.StoreCod,
            piw.WarehouseCod,
            'UNAVAILABLE' AS ZoneStockMoved,
            piw.NumUnavailableStock AS NumStock
        FROM product_info_warehouse piw
        INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod
        INNER JOIN product_info pi
          ON pi.ProductCod = piw.ProductCod
         AND pi.Variant = piw.Variant
         AND pi.StoreCod = w.StoreCod
        WHERE piw.NumUnavailableStock > 0
    ) stock
    WHERE NOT EXISTS (
        SELECT 1
        FROM kardex_zone kz
        WHERE kz.SourceTable = 'no_table'
          AND kz.OperationCod = '00000000'
          AND kz.MovementEvent = 'INITIALIZATION'
          AND kz.ProductCod = stock.ProductCod
          AND kz.Variant = stock.Variant
          AND kz.StoreCod = stock.StoreCod
          AND kz.WarehouseCod = stock.WarehouseCod
          AND kz.ZoneStockMoved = stock.ZoneStockMoved
    );

    SET v_inserted_rows = ROW_COUNT();
    COMMIT;

    SELECT v_inserted_rows AS InsertedRows;
END $$

DELIMITER ;

CALL `p_initialize_kardex_zone`();
DROP PROCEDURE `p_initialize_kardex_zone`;
