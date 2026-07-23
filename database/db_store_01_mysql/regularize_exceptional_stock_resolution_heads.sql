DROP PROCEDURE IF EXISTS `p_regularize_exceptional_stock_resolution_heads`;

DELIMITER $$

CREATE PROCEDURE `p_regularize_exceptional_stock_resolution_heads`()
BEGIN
    DECLARE v_entry_legacy_count INT DEFAULT 0;
    DECLARE v_exit_legacy_count INT DEFAULT 0;

    -- Las versiones anteriores creaban una cabecera ProcessType=R por cada resolucion.
    -- Este proceso conserva los acumulados ya aplicados, copia los ultimos motivos al
    -- detalle original, reasocia el kardex y elimina solamente esas cabeceras redundantes.

    SELECT COUNT(*) INTO v_entry_legacy_count
    FROM `stock_entry_head`
    WHERE `ProcessType` = 'R';

    IF v_entry_legacy_count > 0 THEN
        DROP TEMPORARY TABLE IF EXISTS `tmp_stock_entry_resolution`;
        CREATE TEMPORARY TABLE `tmp_stock_entry_resolution` AS
        SELECT
            rh.`OriginStockEntryCod` AS `OriginCode`,
            rd.`OriginItemNumber`,
            rd.`ResolutionType`,
            rd.`ResolutionReasonCode`,
            rd.`Observation`,
            rd.`NextReviewDate`,
            rh.`ResolutionUser`,
            COALESCE(rh.`ResolutionDate`, rh.`ConfirmDate`, rh.`CreationDate`) AS `ResolutionDate`,
            ROW_NUMBER() OVER (
                PARTITION BY rh.`OriginStockEntryCod`, rd.`OriginItemNumber`,
                    CASE
                        WHEN rd.`ResolutionType` = 'L' THEN 'I'
                        WHEN rd.`ResolutionType` IN ('B', 'D') THEN 'O'
                        ELSE 'M'
                    END
                ORDER BY COALESCE(rh.`ResolutionDate`, rh.`ConfirmDate`, rh.`CreationDate`) DESC,
                    rh.`StockEntryCod` DESC
            ) AS `DirectionOrder`,
            ROW_NUMBER() OVER (
                PARTITION BY rh.`OriginStockEntryCod`, rd.`OriginItemNumber`
                ORDER BY COALESCE(rh.`ResolutionDate`, rh.`ConfirmDate`, rh.`CreationDate`) DESC,
                    rh.`StockEntryCod` DESC
            ) AS `ItemOrder`
        FROM `stock_entry_head` rh
        INNER JOIN `stock_entry_det` rd ON rd.`StockEntryCod` = rh.`StockEntryCod`
        WHERE rh.`ProcessType` = 'R'
          AND rh.`ProcessStatus` = 'C'
          AND rh.`OriginStockEntryCod` IS NOT NULL
          AND rd.`OriginItemNumber` IS NOT NULL;

        UPDATE `stock_entry_det` original
        INNER JOIN `tmp_stock_entry_resolution` legacy
            ON legacy.`OriginCode` = original.`StockEntryCod`
           AND legacy.`OriginItemNumber` = original.`ItemNumber`
           AND legacy.`ResolutionType` = 'L'
           AND legacy.`DirectionOrder` = 1
        SET original.`ResolvedInReasonCode` = legacy.`ResolutionReasonCode`;

        UPDATE `stock_entry_det` original
        INNER JOIN `tmp_stock_entry_resolution` legacy
            ON legacy.`OriginCode` = original.`StockEntryCod`
           AND legacy.`OriginItemNumber` = original.`ItemNumber`
           AND legacy.`ResolutionType` IN ('B', 'D')
           AND legacy.`DirectionOrder` = 1
        SET original.`ResolvedOutReasonCode` = legacy.`ResolutionReasonCode`,
            original.`ResolvedOutType` = legacy.`ResolutionType`;

        UPDATE `stock_entry_det` original
        INNER JOIN `tmp_stock_entry_resolution` legacy
            ON legacy.`OriginCode` = original.`StockEntryCod`
           AND legacy.`OriginItemNumber` = original.`ItemNumber`
           AND legacy.`ItemOrder` = 1
        SET original.`ResolutionVersion` = (
                SELECT COUNT(*)
                FROM `tmp_stock_entry_resolution` counted
                WHERE counted.`OriginCode` = original.`StockEntryCod`
                  AND counted.`OriginItemNumber` = original.`ItemNumber`
            ),
            original.`ResolutionType` = legacy.`ResolutionType`,
            original.`ResolutionReasonCode` = legacy.`ResolutionReasonCode`,
            original.`Observation` = legacy.`Observation`,
            original.`NextReviewDate` = legacy.`NextReviewDate`;

        UPDATE `stock_entry_head` original
        INNER JOIN (
            SELECT `OriginCode`, `ResolutionUser`, `ResolutionDate`
            FROM (
                SELECT
                    `OriginCode`,
                    `ResolutionUser`,
                    `ResolutionDate`,
                    ROW_NUMBER() OVER (
                        PARTITION BY `OriginCode`
                        ORDER BY `ResolutionDate` DESC
                    ) AS `HeadOrder`
                FROM `tmp_stock_entry_resolution`
            ) ordered
            WHERE ordered.`HeadOrder` = 1
        ) legacy ON legacy.`OriginCode` = original.`StockEntryCod`
        SET original.`ResolutionUser` = legacy.`ResolutionUser`,
            original.`ResolutionDate` = legacy.`ResolutionDate`;

        UPDATE `kardex` movement
        INNER JOIN `stock_entry_head` legacy ON legacy.`StockEntryCod` = movement.`OperationCod`
        SET movement.`OperationCod` = legacy.`OriginStockEntryCod`
        WHERE legacy.`ProcessType` = 'R'
          AND movement.`SourceTable` = 'stock_entry_head';

        UPDATE `kardex_zone` movement
        INNER JOIN `stock_entry_head` legacy ON legacy.`StockEntryCod` = movement.`OperationCod`
        SET movement.`OperationCod` = legacy.`OriginStockEntryCod`
        WHERE legacy.`ProcessType` = 'R'
          AND movement.`SourceTable` = 'stock_entry_head';

        DELETE detail
        FROM `stock_entry_det` detail
        INNER JOIN `stock_entry_head` legacy ON legacy.`StockEntryCod` = detail.`StockEntryCod`
        WHERE legacy.`ProcessType` = 'R';

        DELETE FROM `stock_entry_head`
        WHERE `ProcessType` = 'R';

        DROP TEMPORARY TABLE IF EXISTS `tmp_stock_entry_resolution`;
    END IF;

    SELECT COUNT(*) INTO v_exit_legacy_count
    FROM `stock_exit_head`
    WHERE `ProcessType` = 'R';

    IF v_exit_legacy_count > 0 THEN
        DROP TEMPORARY TABLE IF EXISTS `tmp_stock_exit_resolution`;
        CREATE TEMPORARY TABLE `tmp_stock_exit_resolution` AS
        SELECT
            rh.`OriginStockExitCod` AS `OriginCode`,
            rd.`OriginItemNumber`,
            rd.`ResolutionType`,
            rd.`ResolutionReasonCode`,
            rd.`Observation`,
            rd.`NextReviewDate`,
            rh.`ResolutionUser`,
            COALESCE(rh.`ResolutionDate`, rh.`ConfirmDate`, rh.`CreationDate`) AS `ResolutionDate`,
            ROW_NUMBER() OVER (
                PARTITION BY rh.`OriginStockExitCod`, rd.`OriginItemNumber`,
                    CASE
                        WHEN rd.`ResolutionType` = 'L' THEN 'I'
                        WHEN rd.`ResolutionType` IN ('B', 'D') THEN 'O'
                        ELSE 'M'
                    END
                ORDER BY COALESCE(rh.`ResolutionDate`, rh.`ConfirmDate`, rh.`CreationDate`) DESC,
                    rh.`StockExitCod` DESC
            ) AS `DirectionOrder`,
            ROW_NUMBER() OVER (
                PARTITION BY rh.`OriginStockExitCod`, rd.`OriginItemNumber`
                ORDER BY COALESCE(rh.`ResolutionDate`, rh.`ConfirmDate`, rh.`CreationDate`) DESC,
                    rh.`StockExitCod` DESC
            ) AS `ItemOrder`
        FROM `stock_exit_head` rh
        INNER JOIN `stock_exit_det` rd ON rd.`StockExitCod` = rh.`StockExitCod`
        WHERE rh.`ProcessType` = 'R'
          AND rh.`ProcessStatus` = 'C'
          AND rh.`OriginStockExitCod` IS NOT NULL
          AND rd.`OriginItemNumber` IS NOT NULL;

        UPDATE `stock_exit_det` original
        INNER JOIN `tmp_stock_exit_resolution` legacy
            ON legacy.`OriginCode` = original.`StockExitCod`
           AND legacy.`OriginItemNumber` = original.`ItemNumber`
           AND legacy.`ResolutionType` = 'L'
           AND legacy.`DirectionOrder` = 1
        SET original.`ResolvedInReasonCode` = legacy.`ResolutionReasonCode`;

        UPDATE `stock_exit_det` original
        INNER JOIN `tmp_stock_exit_resolution` legacy
            ON legacy.`OriginCode` = original.`StockExitCod`
           AND legacy.`OriginItemNumber` = original.`ItemNumber`
           AND legacy.`ResolutionType` IN ('B', 'D')
           AND legacy.`DirectionOrder` = 1
        SET original.`ResolvedOutReasonCode` = legacy.`ResolutionReasonCode`,
            original.`ResolvedOutType` = legacy.`ResolutionType`;

        UPDATE `stock_exit_det` original
        INNER JOIN `tmp_stock_exit_resolution` legacy
            ON legacy.`OriginCode` = original.`StockExitCod`
           AND legacy.`OriginItemNumber` = original.`ItemNumber`
           AND legacy.`ItemOrder` = 1
        SET original.`ResolutionVersion` = (
                SELECT COUNT(*)
                FROM `tmp_stock_exit_resolution` counted
                WHERE counted.`OriginCode` = original.`StockExitCod`
                  AND counted.`OriginItemNumber` = original.`ItemNumber`
            ),
            original.`ResolutionType` = legacy.`ResolutionType`,
            original.`ResolutionReasonCode` = legacy.`ResolutionReasonCode`,
            original.`Observation` = legacy.`Observation`,
            original.`NextReviewDate` = legacy.`NextReviewDate`;

        UPDATE `stock_exit_head` original
        INNER JOIN (
            SELECT `OriginCode`, `ResolutionUser`, `ResolutionDate`
            FROM (
                SELECT
                    `OriginCode`,
                    `ResolutionUser`,
                    `ResolutionDate`,
                    ROW_NUMBER() OVER (
                        PARTITION BY `OriginCode`
                        ORDER BY `ResolutionDate` DESC
                    ) AS `HeadOrder`
                FROM `tmp_stock_exit_resolution`
            ) ordered
            WHERE ordered.`HeadOrder` = 1
        ) legacy ON legacy.`OriginCode` = original.`StockExitCod`
        SET original.`ResolutionUser` = legacy.`ResolutionUser`,
            original.`ResolutionDate` = legacy.`ResolutionDate`;

        UPDATE `kardex` movement
        INNER JOIN `stock_exit_head` legacy ON legacy.`StockExitCod` = movement.`OperationCod`
        SET movement.`OperationCod` = legacy.`OriginStockExitCod`
        WHERE legacy.`ProcessType` = 'R'
          AND movement.`SourceTable` = 'stock_exit_head';

        UPDATE `kardex_zone` movement
        INNER JOIN `stock_exit_head` legacy ON legacy.`StockExitCod` = movement.`OperationCod`
        SET movement.`OperationCod` = legacy.`OriginStockExitCod`
        WHERE legacy.`ProcessType` = 'R'
          AND movement.`SourceTable` = 'stock_exit_head';

        DELETE detail
        FROM `stock_exit_det` detail
        INNER JOIN `stock_exit_head` legacy ON legacy.`StockExitCod` = detail.`StockExitCod`
        WHERE legacy.`ProcessType` = 'R';

        DELETE FROM `stock_exit_head`
        WHERE `ProcessType` = 'R';

        DROP TEMPORARY TABLE IF EXISTS `tmp_stock_exit_resolution`;
    END IF;

    SELECT
        v_entry_legacy_count AS `EntryResolutionHeadsRegularized`,
        v_exit_legacy_count AS `ExitResolutionHeadsRegularized`;
END $$

DELIMITER ;

CALL `p_regularize_exceptional_stock_resolution_heads`();
DROP PROCEDURE `p_regularize_exceptional_stock_resolution_heads`;
