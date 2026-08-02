DROP PROCEDURE IF EXISTS `p_manage_sale_det_warehouse`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sale_det_warehouse`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE v_allocation_column_exists INT DEFAULT 0;
    DECLARE v_invalid_allocation_count INT DEFAULT 0;
    DECLARE v_tax_table_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'sale_det_warehouse';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_det_warehouse` (
  `SaleCod` varchar(16) NOT NULL COMMENT 'codigo de venta',
  `ItemNumber` int NOT NULL COMMENT 'Número de ítem/secuencia dentro de la venta',
  `ProductCod` varchar(20) NOT NULL COMMENT 'codigo de producto',
  `Variant` varchar(4) NOT NULL DEFAULT (_utf8mb4'0000') COMMENT 'codigo de variante',
  `WarehouseCod` varchar(8) NOT NULL COMMENT 'codigo de almacen',
  `NumUnit` int DEFAULT NULL COMMENT 'Numero de unidades',
  `ProductUnitName` varchar(32) NOT NULL DEFAULT 'NIU' COMMENT 'Unidad visible usada al registrar el detalle',
  `ProductUnitFactor` int NOT NULL DEFAULT '1' COMMENT 'Factor usado al registrar el detalle',
  `LotNumber` varchar(32) DEFAULT NULL COMMENT 'Número de lote del producto (si aplica)',
  `ExpirationDate` date DEFAULT NULL COMMENT 'Fecha de vencimiento (si aplica)',
  `CreationUser` varchar(16) NOT NULL,
  `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ModifyUser` varchar(16) DEFAULT NULL,
  `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Status` char(1) NOT NULL DEFAULT 'A',
  PRIMARY KEY (`SaleCod`,`ItemNumber`),
  KEY `idx_sale_det_warehouse_old_pk` (`SaleCod`,`ProductCod`,`Variant`,`WarehouseCod`),
  KEY `fk_sale_det_warehouse_warehouse` (`WarehouseCod`),
  KEY `fk_sale_det_warehouse_variant` (`ProductCod`,`Variant`),
  CONSTRAINT `fk_sale_det_warehouse_product` FOREIGN KEY (`ProductCod`) REFERENCES `product` (`ProductCod`),
  CONSTRAINT `fk_sale_det_warehouse_detail` FOREIGN KEY (`SaleCod`, `ItemNumber`) REFERENCES `sale_det` (`SaleCod`, `ItemNumber`),
  CONSTRAINT `fk_sale_det_warehouse_sale` FOREIGN KEY (`SaleCod`) REFERENCES `sale_head` (`SaleCod`),
  CONSTRAINT `fk_sale_det_warehouse_variant` FOREIGN KEY (`ProductCod`, `Variant`) REFERENCES `product_variant` (`ProductCod`, `Variant`),
  CONSTRAINT `fk_sale_det_warehouse_warehouse` FOREIGN KEY (`WarehouseCod`) REFERENCES `warehouse` (`WarehouseCod`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sale_document`
--

        SELECT 'Tabla sale_det_warehouse creada desde cero.' AS Mensaje;

    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        
        -- AGREGANDO COLUMNA ItemNumber
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND column_name = 'ItemNumber'
        ) THEN
            ALTER TABLE `sale_det_warehouse` ADD COLUMN `ItemNumber` int NOT NULL DEFAULT 0 COMMENT 'Número de ítem/secuencia dentro de la venta' AFTER `SaleCod`;
            UPDATE `sale_det_warehouse` t
            JOIN (
                SELECT x.`SaleCod`, x.`ProductCod`, x.`Variant`, x.`WarehouseCod`,
                       @item_number := IF(@parent_cod = x.`SaleCod`, @item_number + 1, 1) AS NewItemNumber,
                       @parent_cod := x.`SaleCod`
                FROM (
                    SELECT `SaleCod`, `ProductCod`, `Variant`, `WarehouseCod`
                    FROM `sale_det_warehouse`
                    ORDER BY `SaleCod`, `ProductCod`, `Variant`, `WarehouseCod`
                ) x
                CROSS JOIN (SELECT @parent_cod := '', @item_number := 0) vars
            ) n ON n.`SaleCod` = t.`SaleCod`
                AND n.`ProductCod` = t.`ProductCod`
                AND n.`Variant` = t.`Variant`
                AND n.`WarehouseCod` = t.`WarehouseCod`
            SET t.`ItemNumber` = n.NewItemNumber;
            SELECT 'Columna ItemNumber agregada exitosamente.' AS Mensaje;
        END IF;

        -- ASEGURAR LOS METADATOS INFORMATIVOS ANTES DE MIGRAR POSIBLES ASIGNACIONES.
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND column_name = 'LotNumber'
        ) THEN
            ALTER TABLE `sale_det_warehouse` ADD COLUMN `LotNumber` varchar(32) DEFAULT NULL COMMENT 'Número de lote del producto (si aplica)' AFTER `NumUnit`;
            SELECT 'Columna LotNumber agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND column_name = 'ProductUnitName'
        ) THEN
            ALTER TABLE `sale_det_warehouse` ADD COLUMN `ProductUnitName` varchar(32) NOT NULL DEFAULT 'NIU' COMMENT 'Unidad visible usada al registrar el detalle' AFTER `NumUnit`;
            SELECT 'Columna ProductUnitName agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND column_name = 'ProductUnitFactor'
        ) THEN
            ALTER TABLE `sale_det_warehouse` ADD COLUMN `ProductUnitFactor` int NOT NULL DEFAULT '1' COMMENT 'Factor usado al registrar el detalle' AFTER `ProductUnitName`;
            SELECT 'Columna ProductUnitFactor agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND column_name = 'ExpirationDate'
        ) THEN
            ALTER TABLE `sale_det_warehouse` ADD COLUMN `ExpirationDate` date DEFAULT NULL COMMENT 'Fecha de vencimiento (si aplica)' AFTER `LotNumber`;
            SELECT 'Columna ExpirationDate agregada exitosamente.' AS Mensaje;
        END IF;

        -- CORREGIR EL MODELO TRANSITORIO QUE GUARDABA VARIOS LOTES EN UN MISMO ITEM.
        -- CADA ASIGNACION SE CONVIERTE EN UN sale_det REAL Y sale_det_warehouse VUELVE A SER 1:1.
        SELECT COUNT(*) INTO v_allocation_column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'sale_det_warehouse'
          AND column_name = 'AllocationNumber';

        IF v_allocation_column_exists > 0 THEN
            SELECT COUNT(*) INTO v_invalid_allocation_count
            FROM (
                SELECT w.`SaleCod`, w.`ItemNumber`, SUM(w.`NumUnit`) AS PickedQuantity
                FROM `sale_det_warehouse` w
                GROUP BY w.`SaleCod`, w.`ItemNumber`
            ) picked
            LEFT JOIN `sale_det` sd
              ON sd.`SaleCod` = picked.`SaleCod`
             AND sd.`ItemNumber` = picked.`ItemNumber`
            WHERE sd.`SaleCod` IS NULL
               OR picked.PickedQuantity <= 0
               OR picked.PickedQuantity <> sd.`NumUnit`;

            IF v_invalid_allocation_count > 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'No se puede migrar AllocationNumber: las cantidades no coinciden con sale_det';
            END IF;

            DROP TEMPORARY TABLE IF EXISTS `tmp_sale_picking_allocation`;
            CREATE TEMPORARY TABLE `tmp_sale_picking_allocation` AS
            SELECT ranked.*,
                   CASE
                       WHEN ranked.AllocationOrder = 1 THEN ranked.OriginalItemNumber
                       ELSE ranked.MaxItemNumber
                            + SUM(CASE WHEN ranked.AllocationOrder > 1 THEN 1 ELSE 0 END) OVER (
                                PARTITION BY ranked.SaleCod
                                ORDER BY ranked.OriginalItemNumber, ranked.AllocationNumber
                                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                              )
                   END AS NewItemNumber
            FROM (
                SELECT w.`SaleCod`,
                       w.`ItemNumber` AS OriginalItemNumber,
                       w.`AllocationNumber`,
                       w.`NumUnit`,
                       w.`LotNumber`,
                       w.`ExpirationDate`,
                       ROW_NUMBER() OVER (
                           PARTITION BY w.`SaleCod`, w.`ItemNumber`
                           ORDER BY w.`AllocationNumber`
                       ) AS AllocationOrder,
                       COUNT(*) OVER (
                           PARTITION BY w.`SaleCod`, w.`ItemNumber`
                       ) AS AllocationCount,
                       SUM(w.`NumUnit`) OVER (
                           PARTITION BY w.`SaleCod`, w.`ItemNumber`
                       ) AS TotalNumUnit,
                       COALESCE(max_item.MaxItemNumber, 0) AS MaxItemNumber
                FROM `sale_det_warehouse` w
                LEFT JOIN (
                    SELECT `SaleCod`, MAX(`ItemNumber`) AS MaxItemNumber
                    FROM `sale_det`
                    GROUP BY `SaleCod`
                ) max_item ON max_item.`SaleCod` = w.`SaleCod`
            ) ranked;

            ALTER TABLE `tmp_sale_picking_allocation`
                ADD PRIMARY KEY (`SaleCod`, `OriginalItemNumber`, `AllocationNumber`);

            DROP TEMPORARY TABLE IF EXISTS `tmp_sale_picking_detail`;
            CREATE TEMPORARY TABLE `tmp_sale_picking_detail` AS
            SELECT shares.*,
                   shares.OriginalDiscount AS NewDiscount,
                   CASE WHEN shares.AllocationOrder = shares.AllocationCount
                        THEN shares.OriginalTotalPrice - SUM(
                            CASE WHEN shares.AllocationOrder < shares.AllocationCount
                                 THEN shares.TotalPriceShare ELSE 0 END
                        ) OVER (PARTITION BY shares.SaleCod, shares.OriginalItemNumber)
                        ELSE shares.TotalPriceShare END AS NewTotalPrice,
                   CASE WHEN shares.AllocationOrder = shares.AllocationCount
                        THEN shares.OriginalSubtotal - SUM(
                            CASE WHEN shares.AllocationOrder < shares.AllocationCount
                                 THEN shares.SubtotalShare ELSE 0 END
                        ) OVER (PARTITION BY shares.SaleCod, shares.OriginalItemNumber)
                        ELSE shares.SubtotalShare END AS NewSubtotal,
                   CASE WHEN shares.AllocationOrder = shares.AllocationCount
                        THEN shares.OriginalTax - SUM(
                            CASE WHEN shares.AllocationOrder < shares.AllocationCount
                                 THEN shares.TaxShare ELSE 0 END
                        ) OVER (PARTITION BY shares.SaleCod, shares.OriginalItemNumber)
                        ELSE shares.TaxShare END AS NewTax
            FROM (
                SELECT allocation.*,
                       COALESCE(sd.`NumDiscount`, 0.00) AS OriginalDiscount,
                       COALESCE(sd.`NumTotalPrice`, 0.00) AS OriginalTotalPrice,
                       COALESCE(sd.`NumPriceSubTotal`, 0.00) AS OriginalSubtotal,
                       COALESCE(sd.`NumTotalTax`, 0.00) AS OriginalTax,
                       ROUND(COALESCE(sd.`NumTotalPrice`, 0.00) * allocation.NumUnit / allocation.TotalNumUnit, 2)
                           AS TotalPriceShare,
                       ROUND(COALESCE(sd.`NumPriceSubTotal`, 0.00) * allocation.NumUnit / allocation.TotalNumUnit, 2)
                           AS SubtotalShare,
                       ROUND(COALESCE(sd.`NumTotalTax`, 0.00) * allocation.NumUnit / allocation.TotalNumUnit, 2)
                           AS TaxShare
                FROM `tmp_sale_picking_allocation` allocation
                JOIN `sale_det` sd
                  ON sd.`SaleCod` = allocation.SaleCod
                 AND sd.`ItemNumber` = allocation.OriginalItemNumber
            ) shares;

            SELECT COUNT(*) INTO v_tax_table_exists
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'sale_det_tax';

            IF v_tax_table_exists > 0 THEN
                DROP TEMPORARY TABLE IF EXISTS `tmp_sale_picking_tax`;
                CREATE TEMPORARY TABLE `tmp_sale_picking_tax` AS
                SELECT shares.*,
                       CASE WHEN shares.AllocationOrder = shares.AllocationCount
                            THEN shares.OriginalBaseAmount - SUM(
                                CASE WHEN shares.AllocationOrder < shares.AllocationCount
                                     THEN shares.BaseAmountShare ELSE 0 END
                            ) OVER (PARTITION BY shares.SaleCod, shares.OriginalItemNumber, shares.TaxLineNumber)
                            ELSE shares.BaseAmountShare END AS NewBaseAmount,
                       CASE WHEN shares.AllocationOrder = shares.AllocationCount
                            THEN shares.OriginalTaxQuantity - SUM(
                                CASE WHEN shares.AllocationOrder < shares.AllocationCount
                                     THEN shares.TaxQuantityShare ELSE 0 END
                            ) OVER (PARTITION BY shares.SaleCod, shares.OriginalItemNumber, shares.TaxLineNumber)
                            ELSE shares.TaxQuantityShare END AS NewTaxQuantity,
                       CASE WHEN shares.AllocationOrder = shares.AllocationCount
                            THEN shares.OriginalTaxAmount - SUM(
                                CASE WHEN shares.AllocationOrder < shares.AllocationCount
                                     THEN shares.TaxAmountShare ELSE 0 END
                            ) OVER (PARTITION BY shares.SaleCod, shares.OriginalItemNumber, shares.TaxLineNumber)
                            ELSE shares.TaxAmountShare END AS NewTaxAmount
                FROM (
                    SELECT allocation.`SaleCod`, allocation.OriginalItemNumber,
                           allocation.NewItemNumber, allocation.AllocationOrder,
                           allocation.AllocationCount, tax.`TaxLineNumber`,
                           tax.`TaxBaseAmount` AS OriginalBaseAmount,
                           tax.`TaxQuantity` AS OriginalTaxQuantity,
                           tax.`TaxAmount` AS OriginalTaxAmount,
                           ROUND(tax.`TaxBaseAmount` * allocation.NumUnit / allocation.TotalNumUnit, 2)
                               AS BaseAmountShare,
                           ROUND(tax.`TaxQuantity` * allocation.NumUnit / allocation.TotalNumUnit, 4)
                               AS TaxQuantityShare,
                           ROUND(tax.`TaxAmount` * allocation.NumUnit / allocation.TotalNumUnit, 2)
                               AS TaxAmountShare
                    FROM `tmp_sale_picking_allocation` allocation
                    JOIN `sale_det_tax` tax
                      ON tax.`SaleCod` = allocation.SaleCod
                     AND tax.`ItemNumber` = allocation.OriginalItemNumber
                ) shares;
            END IF;

            INSERT INTO `sale_det` (
                `SaleCod`, `ItemNumber`, `ProductCod`, `Variant`, `NumUnit`,
                `NumUnitPrice`, `NumDiscount`, `NumUnitPriceSale`, `NumTotalPrice`,
                `NumPriceSubTotal`, `NumTotalTax`, `ProductUnitName`, `ProductUnitFactor`,
                `IsAppliedTax`, `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`,
                `Status`, `LotNumber`, `ExpirationDate`
            )
            SELECT detail.SaleCod, detail.NewItemNumber, source.`ProductCod`, source.`Variant`,
                   detail.NumUnit, source.`NumUnitPrice`, detail.NewDiscount,
                   source.`NumUnitPriceSale`, detail.NewTotalPrice, detail.NewSubtotal,
                   detail.NewTax, source.`ProductUnitName`, source.`ProductUnitFactor`,
                   source.`IsAppliedTax`, source.`CreationUser`, source.`CreationDate`,
                   source.`ModifyUser`, source.`ModifyDate`, source.`Status`,
                   detail.LotNumber, detail.ExpirationDate
            FROM `tmp_sale_picking_detail` detail
            JOIN `sale_det` source
              ON source.`SaleCod` = detail.SaleCod
             AND source.`ItemNumber` = detail.OriginalItemNumber
            WHERE detail.AllocationOrder > 1;

            IF v_tax_table_exists > 0 THEN
                INSERT INTO `sale_det_tax` (
                    `SaleCod`, `ItemNumber`, `TaxLineNumber`, `TaxCod`, `SunatTaxCod`,
                    `TaxName`, `TaxAffectationCod`, `TaxAffectationName`, `TaxCalculationType`,
                    `IsInformative`, `TaxRateValue`, `FixedUnitAmount`, `TaxBaseAmount`,
                    `TaxQuantity`, `TaxAmount`, `CalculationOrder`, `CreationUser`,
                    `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`
                )
                SELECT tax_split.SaleCod, tax_split.NewItemNumber, source.`TaxLineNumber`,
                       source.`TaxCod`, source.`SunatTaxCod`, source.`TaxName`,
                       source.`TaxAffectationCod`, source.`TaxAffectationName`,
                       source.`TaxCalculationType`, source.`IsInformative`, source.`TaxRateValue`,
                       source.`FixedUnitAmount`, tax_split.NewBaseAmount,
                       tax_split.NewTaxQuantity, tax_split.NewTaxAmount,
                       source.`CalculationOrder`, source.`CreationUser`, source.`CreationDate`,
                       source.`ModifyUser`, source.`ModifyDate`, source.`Status`
                FROM `tmp_sale_picking_tax` tax_split
                JOIN `sale_det_tax` source
                  ON source.`SaleCod` = tax_split.SaleCod
                 AND source.`ItemNumber` = tax_split.OriginalItemNumber
                 AND source.`TaxLineNumber` = tax_split.TaxLineNumber
                WHERE tax_split.AllocationOrder > 1;

                UPDATE `sale_det_tax` tax
                JOIN `tmp_sale_picking_tax` tax_split
                  ON tax_split.SaleCod = tax.`SaleCod`
                 AND tax_split.OriginalItemNumber = tax.`ItemNumber`
                 AND tax_split.TaxLineNumber = tax.`TaxLineNumber`
                 AND tax_split.AllocationOrder = 1
                SET tax.`TaxBaseAmount` = tax_split.NewBaseAmount,
                    tax.`TaxQuantity` = tax_split.NewTaxQuantity,
                    tax.`TaxAmount` = tax_split.NewTaxAmount;
            END IF;

            UPDATE `sale_det` detail
            JOIN `tmp_sale_picking_detail` detail_split
              ON detail_split.SaleCod = detail.`SaleCod`
             AND detail_split.OriginalItemNumber = detail.`ItemNumber`
             AND detail_split.AllocationOrder = 1
            SET detail.`NumUnit` = detail_split.NumUnit,
                detail.`NumDiscount` = detail_split.NewDiscount,
                detail.`NumTotalPrice` = detail_split.NewTotalPrice,
                detail.`NumPriceSubTotal` = detail_split.NewSubtotal,
                detail.`NumTotalTax` = detail_split.NewTax,
                detail.`LotNumber` = detail_split.LotNumber,
                detail.`ExpirationDate` = detail_split.ExpirationDate;

            UPDATE `sale_det_warehouse` warehouse
            JOIN `tmp_sale_picking_allocation` allocation
              ON allocation.SaleCod = warehouse.`SaleCod`
             AND allocation.OriginalItemNumber = warehouse.`ItemNumber`
             AND allocation.AllocationNumber = warehouse.`AllocationNumber`
            SET warehouse.`ItemNumber` = allocation.NewItemNumber;

            IF EXISTS (
                SELECT * FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = 'sale_det_warehouse'
                  AND constraint_type = 'PRIMARY KEY'
            ) THEN
                ALTER TABLE `sale_det_warehouse` DROP PRIMARY KEY;
            END IF;

            ALTER TABLE `sale_det_warehouse` DROP COLUMN `AllocationNumber`;
            ALTER TABLE `sale_det_warehouse` ADD PRIMARY KEY (`SaleCod`, `ItemNumber`);

            DROP TEMPORARY TABLE IF EXISTS `tmp_sale_picking_tax`;
            DROP TEMPORARY TABLE IF EXISTS `tmp_sale_picking_detail`;
            DROP TEMPORARY TABLE IF EXISTS `tmp_sale_picking_allocation`;

            SELECT 'AllocationNumber eliminado y detalles de venta separados por lote.' AS Mensaje;
        END IF;

        -- ACTUALIZANDO PRIMARY KEY SI ES NECESARIO
        IF NOT EXISTS (
            SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND index_name = 'PRIMARY' AND column_name = 'SaleCod' AND seq_in_index = 1
        ) OR NOT EXISTS (
            SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND index_name = 'PRIMARY' AND column_name = 'ItemNumber' AND seq_in_index = 2
        ) OR EXISTS (
            SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
            AND index_name = 'PRIMARY' AND seq_in_index > 2
        ) THEN
            IF NOT EXISTS (
                SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
                AND index_name = 'idx_sale_det_warehouse_old_pk'
            ) THEN
                ALTER TABLE `sale_det_warehouse` ADD KEY `idx_sale_det_warehouse_old_pk` (`SaleCod`,`ProductCod`,`Variant`,`WarehouseCod`);
                SELECT 'Índice idx_sale_det_warehouse_old_pk agregado exitosamente.' AS Mensaje;
            END IF;

            IF EXISTS (
                SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'sale_det_warehouse'
                AND constraint_type = 'PRIMARY KEY'
            ) THEN
                ALTER TABLE `sale_det_warehouse` DROP PRIMARY KEY;
            END IF;

            ALTER TABLE `sale_det_warehouse` ADD PRIMARY KEY (`SaleCod`,`ItemNumber`);
            SELECT 'Primary key de sale_det_warehouse actualizada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'sale_det_warehouse'
              AND constraint_name = 'fk_sale_det_warehouse_detail'
              AND constraint_type = 'FOREIGN KEY'
        ) THEN
            SELECT COUNT(*) INTO v_invalid_allocation_count
            FROM `sale_det_warehouse` warehouse
            LEFT JOIN `sale_det` detail
              ON detail.`SaleCod` = warehouse.`SaleCod`
             AND detail.`ItemNumber` = warehouse.`ItemNumber`
            WHERE detail.`SaleCod` IS NULL;

            IF v_invalid_allocation_count > 0 THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'Existen registros de sale_det_warehouse sin un sale_det correspondiente';
            END IF;

            ALTER TABLE `sale_det_warehouse`
                ADD CONSTRAINT `fk_sale_det_warehouse_detail`
                FOREIGN KEY (`SaleCod`, `ItemNumber`)
                REFERENCES `sale_det` (`SaleCod`, `ItemNumber`);
            SELECT 'Relacion 1:1 entre sale_det_warehouse y sale_det agregada exitosamente.' AS Mensaje;
        END IF;

    END IF;

END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_sale_det_warehouse`();
DROP PROCEDURE `p_manage_sale_det_warehouse`;
