-- Ejecutar despues de:
--   tables/table_kardex_zone.sql
--   initialize_kardex_zone.sql
--
-- Reconcilia el ultimo saldo registrado por zona contra product_info_warehouse.
-- Solo inserta movimientos contables en kardex_zone; no modifica el stock real.
-- Es idempotente: una segunda ejecucion sin cambios inserta cero filas.

DROP PROCEDURE IF EXISTS `p_regularize_kardex_zone`;

DELIMITER $$

CREATE PROCEDURE `p_regularize_kardex_zone`()
BEGIN
    DECLARE v_inconsistent_products INT DEFAULT 0;
    DECLARE v_orphan_warehouses INT DEFAULT 0;
    DECLARE v_differences_before INT DEFAULT 0;
    DECLARE v_differences_after INT DEFAULT 0;
    DECLARE v_inserted_rows INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS tmp_kardex_zone_balance;
        RESIGNAL;
    END;

    IF NOT EXISTS (
        SELECT * FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'kardex_zone'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Debe crear la tabla kardex_zone antes de regularizarla';
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
                SUM(COALESCE(piw.NumUnavailableStock, 0)) AS NumUnavailableStock,
                SUM(COALESCE(piw.NumReservedStock, 0)) AS NumReservedStock,
                SUM(COALESCE(piw.NumTotalStock, 0)) AS NumTotalStock
            FROM product_info_warehouse piw
            INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod
            GROUP BY piw.ProductCod, piw.Variant, w.StoreCod
        ) warehouse_stock
          ON warehouse_stock.ProductCod = pi.ProductCod
         AND warehouse_stock.Variant = pi.Variant
         AND warehouse_stock.StoreCod = pi.StoreCod
        WHERE COALESCE(pi.NumPhysicalStock, 0) <> COALESCE(warehouse_stock.NumPhysicalStock, 0)
           OR COALESCE(pi.NumUnavailableStock, 0) <> COALESCE(warehouse_stock.NumUnavailableStock, 0)
           OR COALESCE(pi.NumReservedStock, 0) <> COALESCE(warehouse_stock.NumReservedStock, 0)
           OR COALESCE(pi.NumTotalStock, 0) <> COALESCE(warehouse_stock.NumTotalStock, 0)
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

    DROP TEMPORARY TABLE IF EXISTS tmp_kardex_zone_balance;
    CREATE TEMPORARY TABLE tmp_kardex_zone_balance (
        ProductCod varchar(20) NOT NULL,
        Variant varchar(4) NOT NULL,
        StoreCod varchar(4) NOT NULL,
        WarehouseCod varchar(8) NOT NULL,
        ZoneStockMoved varchar(16) NOT NULL,
        CurrentStock int NOT NULL,
        LastStock int NOT NULL,
        PRIMARY KEY (ProductCod, Variant, StoreCod, WarehouseCod, ZoneStockMoved)
    ) ENGINE=InnoDB;

    INSERT INTO tmp_kardex_zone_balance (
        ProductCod,
        Variant,
        StoreCod,
        WarehouseCod,
        ZoneStockMoved,
        CurrentStock,
        LastStock
    )
    SELECT
        current_stock.ProductCod,
        current_stock.Variant,
        current_stock.StoreCod,
        current_stock.WarehouseCod,
        current_stock.ZoneStockMoved,
        current_stock.CurrentStock,
        COALESCE(last_movement.NumZoneStockAfter, 0)
    FROM (
        SELECT piw.ProductCod, piw.Variant, w.StoreCod, piw.WarehouseCod,
               'PHYSICAL' AS ZoneStockMoved,
               COALESCE(piw.NumPhysicalStock, 0) AS CurrentStock
        FROM product_info_warehouse piw
        INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod

        UNION ALL

        SELECT piw.ProductCod, piw.Variant, w.StoreCod, piw.WarehouseCod,
               'RESERVED' AS ZoneStockMoved,
               COALESCE(piw.NumReservedStock, 0) AS CurrentStock
        FROM product_info_warehouse piw
        INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod

        UNION ALL

        SELECT piw.ProductCod, piw.Variant, w.StoreCod, piw.WarehouseCod,
               'UNAVAILABLE' AS ZoneStockMoved,
               COALESCE(piw.NumUnavailableStock, 0) AS CurrentStock
        FROM product_info_warehouse piw
        INNER JOIN warehouse w ON w.WarehouseCod = piw.WarehouseCod
    ) current_stock
    LEFT JOIN (
        SELECT kz.*
        FROM kardex_zone kz
        INNER JOIN (
            SELECT
                ProductCod,
                Variant,
                StoreCod,
                WarehouseCod,
                ZoneStockMoved,
                MAX(KardexZoneID) AS KardexZoneID
            FROM kardex_zone
            WHERE Status = 'A'
            GROUP BY ProductCod, Variant, StoreCod, WarehouseCod, ZoneStockMoved
        ) last_id ON last_id.KardexZoneID = kz.KardexZoneID
    ) last_movement
      ON last_movement.ProductCod = current_stock.ProductCod
     AND last_movement.Variant = current_stock.Variant
     AND last_movement.StoreCod = current_stock.StoreCod
     AND last_movement.WarehouseCod = current_stock.WarehouseCod
     AND last_movement.ZoneStockMoved = current_stock.ZoneStockMoved;

    SELECT COUNT(*) INTO v_differences_before
    FROM tmp_kardex_zone_balance
    WHERE CurrentStock <> LastStock;

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
        'REGULARIZATION',
        NULL,
        'no_table',
        'BALANCE_REGULARIZATION',
        balance.ProductCod,
        balance.Variant,
        balance.StoreCod,
        balance.WarehouseCod,
        balance.ZoneStockMoved,
        CASE WHEN balance.CurrentStock > balance.LastStock THEN 'S' ELSE 'R' END,
        ABS(balance.CurrentStock - balance.LastStock),
        balance.LastStock,
        balance.CurrentStock,
        NULL,
        NULL,
        'SYSTEM',
        NOW(),
        NULL,
        NOW(),
        'A'
    FROM tmp_kardex_zone_balance balance
    WHERE balance.CurrentStock <> balance.LastStock;

    SET v_inserted_rows = ROW_COUNT();
    COMMIT;

    SELECT COUNT(*) INTO v_differences_after
    FROM tmp_kardex_zone_balance balance
    INNER JOIN kardex_zone kz
      ON kz.KardexZoneID = (
          SELECT MAX(kz_last.KardexZoneID)
          FROM kardex_zone kz_last
          WHERE kz_last.ProductCod = balance.ProductCod
            AND kz_last.Variant = balance.Variant
            AND kz_last.StoreCod = balance.StoreCod
            AND kz_last.WarehouseCod = balance.WarehouseCod
            AND kz_last.ZoneStockMoved = balance.ZoneStockMoved
            AND kz_last.Status = 'A'
      )
    WHERE balance.CurrentStock <> kz.NumZoneStockAfter;

    DROP TEMPORARY TABLE tmp_kardex_zone_balance;

    SELECT
        v_inconsistent_products AS InconsistentProducts,
        v_orphan_warehouses AS OrphanWarehouses,
        v_differences_before AS DifferencesBefore,
        v_inserted_rows AS InsertedRows,
        v_differences_after AS DifferencesAfter;
END $$

DELIMITER ;

CALL `p_regularize_kardex_zone`();
DROP PROCEDURE `p_regularize_kardex_zone`;
