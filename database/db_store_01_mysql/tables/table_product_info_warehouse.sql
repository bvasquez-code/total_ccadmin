DROP PROCEDURE IF EXISTS `p_manage_product_info_warehouse`;

DELIMITER $$

CREATE PROCEDURE `p_manage_product_info_warehouse`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'product_info_warehouse';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_info_warehouse` (
  `ProductCod` varchar(20) NOT NULL,
  `Variant` varchar(4) NOT NULL DEFAULT (_utf8mb4'0000'),
  `WarehouseCod` varchar(8) NOT NULL COMMENT 'codigo de alamacen',
  `NumDigitalStock` int DEFAULT '0',
  `NumPhysicalStock` int DEFAULT '0',
  `NumUnavailableStock` int NOT NULL DEFAULT '0',
  `NumReservedStock` int NOT NULL DEFAULT '0',
  `NumTotalStock` int NOT NULL DEFAULT '0',
  `CreationUser` varchar(16) NOT NULL,
  `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ModifyUser` varchar(16) DEFAULT NULL,
  `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Status` char(1) NOT NULL DEFAULT 'A',
  PRIMARY KEY (`ProductCod`,`Variant`,`WarehouseCod`),
  KEY `fk_product_info_warehouse` (`WarehouseCod`),
  CONSTRAINT `fk_product_info_warehouse` FOREIGN KEY (`WarehouseCod`) REFERENCES `warehouse` (`WarehouseCod`),
  CONSTRAINT `fk_product_info_warehouse_product` FOREIGN KEY (`ProductCod`) REFERENCES `product` (`ProductCod`),
  CONSTRAINT `fk_product_info_warehouse_variant` FOREIGN KEY (`ProductCod`, `Variant`) REFERENCES `product_variant` (`ProductCod`, `Variant`),
  CONSTRAINT `chk_product_info_warehouse_stock_non_negative` CHECK (((coalesce(`NumPhysicalStock`,0) >= 0) and (`NumUnavailableStock` >= 0) and (`NumReservedStock` >= 0) and (`NumTotalStock` >= 0))),
  CONSTRAINT `chk_product_info_warehouse_stock_total` CHECK (((coalesce(`NumPhysicalStock`,0) + `NumUnavailableStock` + `NumReservedStock`) = `NumTotalStock`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_picture`
--

        SELECT 'Tabla product_info_warehouse creada desde cero.' AS Mensaje;

    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product_info_warehouse'
            AND column_name = 'NumUnavailableStock'
        ) THEN
            ALTER TABLE `product_info_warehouse` ADD COLUMN `NumUnavailableStock` int NOT NULL DEFAULT '0' AFTER `NumPhysicalStock`;
            SELECT 'Columna NumUnavailableStock agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product_info_warehouse'
            AND column_name = 'NumReservedStock'
        ) THEN
            ALTER TABLE `product_info_warehouse` ADD COLUMN `NumReservedStock` int NOT NULL DEFAULT '0' AFTER `NumUnavailableStock`;
            SELECT 'Columna NumReservedStock agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product_info_warehouse'
            AND column_name = 'NumTotalStock'
        ) THEN
            ALTER TABLE `product_info_warehouse` ADD COLUMN `NumTotalStock` int NOT NULL DEFAULT '0' AFTER `NumReservedStock`;
            SELECT 'Columna NumTotalStock agregada exitosamente.' AS Mensaje;
        END IF;

        UPDATE `product_info_warehouse`
        SET `NumTotalStock` = coalesce(`NumPhysicalStock`,0) + `NumUnavailableStock` + `NumReservedStock`;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'product_info_warehouse'
            AND constraint_name = 'chk_product_info_warehouse_stock_non_negative'
        ) THEN
            ALTER TABLE `product_info_warehouse` ADD CONSTRAINT `chk_product_info_warehouse_stock_non_negative`
            CHECK (((coalesce(`NumPhysicalStock`,0) >= 0) and (`NumUnavailableStock` >= 0) and (`NumReservedStock` >= 0) and (`NumTotalStock` >= 0)));
            SELECT 'Check chk_product_info_warehouse_stock_non_negative agregado exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'product_info_warehouse'
            AND constraint_name = 'chk_product_info_warehouse_stock_total'
        ) THEN
            ALTER TABLE `product_info_warehouse` ADD CONSTRAINT `chk_product_info_warehouse_stock_total`
            CHECK (((coalesce(`NumPhysicalStock`,0) + `NumUnavailableStock` + `NumReservedStock`) = `NumTotalStock`));
            SELECT 'Check chk_product_info_warehouse_stock_total agregado exitosamente.' AS Mensaje;
        END IF;
        
        SELECT 'Tabla product_info_warehouse ya existe. No se realizaron cambios estructurales.' AS Mensaje;

    END IF;

END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_product_info_warehouse`();
DROP PROCEDURE `p_manage_product_info_warehouse`;
