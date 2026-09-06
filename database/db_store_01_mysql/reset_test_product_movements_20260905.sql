-- ============================================================================
-- REINICIO DE MOVIMIENTOS DE PRODUCTOS - SOLO BASE DE DATOS DE PRUEBAS
-- ============================================================================
-- Este script es destructivo e irreversible:
--   1. elimina compras, entradas/salidas, transferencias, preventas, ventas,
--      notas de credito y sus registros operativos relacionados;
--   2. elimina kardex, kardex por zona y trazabilidad por lote tecnico;
--   3. elimina pagos, sesiones de caja y documentos SUNAT de esas pruebas;
--   4. reinicia AUTO_INCREMENT mediante TRUNCATE y los correlativos funcionales;
--   5. conserva maestros (productos, variantes, locales, almacenes, clientes,
--      proveedores, configuraciones y talonarios) y deja todo stock en cero.
--
-- Antes de ejecutar, reemplace el nombre de la base y escriba el token exacto.
-- La doble confirmacion evita ejecutarlo accidentalmente en otra base.
-- Detenga temporalmente el backend y sus workers para que no creen movimientos
-- durante el reinicio. Tome un respaldo si necesita recuperar estos datos.
-- ============================================================================

SET @ResetExpectedTestDatabase = 'REEMPLAZAR_POR_NOMBRE_DE_BASE_TEST';
SET @ResetProductMovementsConfirmation = '';
-- SET @ResetProductMovementsConfirmation = 'BORRAR_MOVIMIENTOS_PRODUCTOS_TEST';

DROP PROCEDURE IF EXISTS `p_reset_test_product_movements_truncate`;
DROP PROCEDURE IF EXISTS `p_reset_test_product_movements`;

DELIMITER $$

CREATE PROCEDURE `p_reset_test_product_movements_truncate`(
    IN p_TableName varchar(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_TableName
          AND table_type = 'BASE TABLE'
    ) THEN
        SET @ResetProductMovementsSql = CONCAT(
            'TRUNCATE TABLE `',
            REPLACE(p_TableName, '`', '``'),
            '`'
        );

        PREPARE ResetProductMovementsStatement
            FROM @ResetProductMovementsSql;
        EXECUTE ResetProductMovementsStatement;
        DEALLOCATE PREPARE ResetProductMovementsStatement;
    END IF;
END $$

CREATE PROCEDURE `p_reset_test_product_movements`()
BEGIN
    DECLARE v_PreviousForeignKeyChecks int DEFAULT 1;
    DECLARE v_ProductInfoRows bigint DEFAULT 0;
    DECLARE v_ProductInfoWarehouseRows bigint DEFAULT 0;
    DECLARE v_ProductSearchRows bigint DEFAULT 0;
    DECLARE v_ProductRankingRows bigint DEFAULT 0;
    DECLARE v_AppSessionRows bigint DEFAULT 0;
    DECLARE v_AppSessionHistoryRows bigint DEFAULT 0;
    DECLARE v_StoreSequenceRows bigint DEFAULT 0;
    DECLARE v_TableSequenceRows bigint DEFAULT 0;
    DECLARE v_CounterfoilRows bigint DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET FOREIGN_KEY_CHECKS = v_PreviousForeignKeyChecks;
        RESIGNAL;
    END;

    SET v_PreviousForeignKeyChecks = @@FOREIGN_KEY_CHECKS;

    IF DATABASE() IS NULL OR TRIM(DATABASE()) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seleccione explicitamente la base de datos TEST antes de ejecutar el reinicio';
    END IF;

    IF COALESCE(@ResetExpectedTestDatabase, '') <> DATABASE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La base actual no coincide con @ResetExpectedTestDatabase; reinicio cancelado';
    END IF;

    IF COALESCE(@ResetProductMovementsConfirmation, '')
            <> 'BORRAR_MOVIMIENTOS_PRODUCTOS_TEST' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Token de confirmacion incorrecto; reinicio cancelado';
    END IF;

    -- TRUNCATE reinicia tambien los AUTO_INCREMENT. Se desactivan las FK solo
    -- en esta sesion y se restauran incluso si una sentencia genera un error.
    SET FOREIGN_KEY_CHECKS = 0;

    -- Trazabilidad e inventario historico.
    CALL `p_reset_test_product_movements_truncate`('product_traceability');
    CALL `p_reset_test_product_movements_truncate`('kardex_zone');
    CALL `p_reset_test_product_movements_truncate`('kardex');

    -- Documentos electronicos generados por ventas, notas y transferencias.
    CALL `p_reset_test_product_movements_truncate`('sunat_document_attempt');
    CALL `p_reset_test_product_movements_truncate`('sunat_document_file');
    CALL `p_reset_test_product_movements_truncate`('sunat_document_payload');
    CALL `p_reset_test_product_movements_truncate`('sunat_document');
    CALL `p_reset_test_product_movements_truncate`('sunat_submission');

    -- Notas de credito.
    CALL `p_reset_test_product_movements_truncate`('credit_note_det_tax');
    CALL `p_reset_test_product_movements_truncate`('credit_note_det_warehouse');
    CALL `p_reset_test_product_movements_truncate`('credit_note_application');
    CALL `p_reset_test_product_movements_truncate`('credit_note_document');
    CALL `p_reset_test_product_movements_truncate`('credit_note_det');
    CALL `p_reset_test_product_movements_truncate`('credit_note_head');

    -- Ventas, preventas y carrito virtual.
    CALL `p_reset_test_product_movements_truncate`('virtual_cart');
    CALL `p_reset_test_product_movements_truncate`('sale_det_tax');
    CALL `p_reset_test_product_movements_truncate`('sale_det_warehouse');
    CALL `p_reset_test_product_movements_truncate`('sale_applied_tax');
    CALL `p_reset_test_product_movements_truncate`('sale_billing');
    CALL `p_reset_test_product_movements_truncate`('sale_channel');
    CALL `p_reset_test_product_movements_truncate`('sale_delivery');
    CALL `p_reset_test_product_movements_truncate`('sale_document');
    CALL `p_reset_test_product_movements_truncate`('sale_payments');
    CALL `p_reset_test_product_movements_truncate`('sale_det');
    CALL `p_reset_test_product_movements_truncate`('sale_head');
    CALL `p_reset_test_product_movements_truncate`('presale_channel');
    CALL `p_reset_test_product_movements_truncate`('presale_det_warehouse');
    CALL `p_reset_test_product_movements_truncate`('presale_det');
    CALL `p_reset_test_product_movements_truncate`('presale_head');

    -- Pagos y sesiones de caja vinculados a las operaciones eliminadas.
    CALL `p_reset_test_product_movements_truncate`('trx_payments_document');
    CALL `p_reset_test_product_movements_truncate`('trx_payments');
    CALL `p_reset_test_product_movements_truncate`('cash_session_item');
    CALL `p_reset_test_product_movements_truncate`('cash_session');

    -- Las sesiones de usuario se conservan, pero ya no deben apuntar a una
    -- sesion de caja que acaba de ser eliminada.
    UPDATE `app_session`
    SET `CashSessionID` = NULL,
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = NOW()
    WHERE `CashSessionID` IS NOT NULL;
    SET v_AppSessionRows = ROW_COUNT();

    UPDATE `app_session_history`
    SET `CashSessionID` = NULL,
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = NOW()
    WHERE `CashSessionID` IS NOT NULL;
    SET v_AppSessionHistoryRows = ROW_COUNT();

    -- Transferencias y solicitudes.
    CALL `p_reset_test_product_movements_truncate`('transfer_document');
    CALL `p_reset_test_product_movements_truncate`('transfer_det');
    CALL `p_reset_test_product_movements_truncate`('transfer_head');
    CALL `p_reset_test_product_movements_truncate`('transfer_request_det');
    CALL `p_reset_test_product_movements_truncate`('transfer_request_head');

    -- Entradas y salidas excepcionales de stock.
    CALL `p_reset_test_product_movements_truncate`('stock_entry_det');
    CALL `p_reset_test_product_movements_truncate`('stock_entry_head');
    CALL `p_reset_test_product_movements_truncate`('stock_exit_det');
    CALL `p_reset_test_product_movements_truncate`('stock_exit_head');

    -- Compras y solicitudes de compra.
    CALL `p_reset_test_product_movements_truncate`('pucharse_det_delivery');
    CALL `p_reset_test_product_movements_truncate`('pucharse_det');
    CALL `p_reset_test_product_movements_truncate`('pucharse_head');
    CALL `p_reset_test_product_movements_truncate`('pucharse_request_det');
    CALL `p_reset_test_product_movements_truncate`('pucharse_request_head');

    -- Historial de cargas masivas. La secuencia es compartida por los tipos de
    -- carga, por eso se limpia el historial completo antes de reiniciarla.
    CALL `p_reset_test_product_movements_truncate`('bulk_load_det');
    CALL `p_reset_test_product_movements_truncate`('bulk_load_destination');
    CALL `p_reset_test_product_movements_truncate`('bulk_load_head');

    -- Stock actual por local, almacen y vista desnormalizada de busqueda.
    UPDATE `product_info`
    SET `NumDigitalStock` = 0,
        `NumPhysicalStock` = 0,
        `NumUnavailableStock` = 0,
        `NumReservedStock` = 0,
        `NumTotalStock` = 0,
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = NOW();
    SET v_ProductInfoRows = ROW_COUNT();

    UPDATE `product_info_warehouse`
    SET `NumDigitalStock` = 0,
        `NumPhysicalStock` = 0,
        `NumUnavailableStock` = 0,
        `NumReservedStock` = 0,
        `NumTotalStock` = 0,
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = NOW();
    SET v_ProductInfoWarehouseRows = ROW_COUNT();

    UPDATE `product_search`
    SET `NumDigitalStock` = 0,
        `NumPhysicalStock` = 0,
        `NumUnavailableStock` = 0,
        `NumReservedStock` = 0,
        `NumTotalStock` = 0,
        `NumTrend` = 0,
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = NOW();
    SET v_ProductSearchRows = ROW_COUNT();

    UPDATE `product_ranking`
    SET `RankingPoints` = 0,
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = NOW();
    SET v_ProductRankingRows = ROW_COUNT();

    -- Correlativos por local de los documentos operativos eliminados.
    UPDATE `store_sequence`
    SET `SequenceTrx` = 0
    WHERE `SequenceTableType` IN (
        'pucharse_request_head',
        'pucharse_head',
        'stock_entry_head',
        'stock_exit_head',
        'transfer_request_head',
        'transfer_head',
        'presale_head',
        'sale_head',
        'credit_note_head'
    );
    SET v_StoreSequenceRows = ROW_COUNT();

    -- Correlativos globales usados por cargas, envios SUNAT y lote tecnico.
    UPDATE `table_sequence`
    SET `SequenceTrx` = 0
    WHERE `SequenceTableType` IN (
        'bulk_load_head',
        'sunat_submission',
        'product_traceability'
    );
    SET v_TableSequenceRows = ROW_COUNT();

    -- Se conservan series, asignaciones por local y configuracion del talonario.
    UPDATE `counterfoil`
    SET `Correlative` = 0,
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = NOW();
    SET v_CounterfoilRows = ROW_COUNT();

    SET FOREIGN_KEY_CHECKS = v_PreviousForeignKeyChecks;

    SELECT
        DATABASE() AS `DatabaseReset`,
        v_ProductInfoRows AS `ProductInfoRowsReset`,
        v_ProductInfoWarehouseRows AS `ProductInfoWarehouseRowsReset`,
        v_ProductSearchRows AS `ProductSearchRowsReset`,
        v_ProductRankingRows AS `ProductRankingRowsReset`,
        v_AppSessionRows AS `AppSessionRowsUnlinked`,
        v_AppSessionHistoryRows AS `AppSessionHistoryRowsUnlinked`,
        v_StoreSequenceRows AS `StoreSequenceRowsReset`,
        v_TableSequenceRows AS `TableSequenceRowsReset`,
        v_CounterfoilRows AS `CounterfoilRowsReset`,
        'OK' AS `Result`;
END $$

DELIMITER ;

CALL `p_reset_test_product_movements`();

DROP PROCEDURE IF EXISTS `p_reset_test_product_movements`;
DROP PROCEDURE IF EXISTS `p_reset_test_product_movements_truncate`;

SET @ResetProductMovementsSql = NULL;
SET @ResetProductMovementsConfirmation = NULL;
SET @ResetExpectedTestDatabase = NULL;
