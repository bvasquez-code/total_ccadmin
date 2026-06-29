DROP PROCEDURE IF EXISTS `p_manage_product_config`;

DELIMITER $$

CREATE PROCEDURE `p_manage_product_config`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'product_config';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product_config` (
  `ProductCod` varchar(20) NOT NULL,
  `StoreCod` varchar(4) NOT NULL COMMENT 'codigo de tienda',
  `NumPrice` decimal(16,2) DEFAULT '0.00',
  `NumMaxStock` int DEFAULT '0',
  `NumMinStock` int DEFAULT '0',
  `IsDiscontable` char(1) DEFAULT 'N',
  `DiscountType` char(2) DEFAULT NULL,
  `NumDiscountMax` decimal(16,2) DEFAULT '0.00',
  `ProductUnitName` varchar(32) NOT NULL DEFAULT 'NIU' COMMENT 'Unidad visible usada para operar el producto',
  `ProductUnitFactor` int NOT NULL DEFAULT '1' COMMENT 'Multiplicador desde cantidad visible hacia unidad minima',
  `Version` varchar(8) DEFAULT NULL,
  `CreationUser` varchar(16) NOT NULL,
  `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ModifyUser` varchar(16) DEFAULT NULL,
  `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Status` char(1) NOT NULL DEFAULT 'A',
  PRIMARY KEY (`ProductCod`,`StoreCod`),
  KEY `fk_product_config_store` (`StoreCod`),
  CONSTRAINT `chk_product_config_product_unit_factor` CHECK (`ProductUnitFactor` >= 1),
  CONSTRAINT `fk_product_config_product` FOREIGN KEY (`ProductCod`) REFERENCES `product` (`ProductCod`),
  CONSTRAINT `fk_product_config_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_info`
--

        SELECT 'Tabla product_config creada desde cero.' AS Mensaje;

    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product_config'
            AND column_name = 'StoreCod'
        ) THEN
            ALTER TABLE `product_config` ADD COLUMN `StoreCod` varchar(4) NULL COMMENT 'codigo de tienda' AFTER `ProductCod`;
            UPDATE `product_config`
            SET `StoreCod` = (SELECT s.`StoreCod` FROM `store` s ORDER BY s.`StoreCod` LIMIT 1)
            WHERE `StoreCod` IS NULL;
            SELECT 'Columna StoreCod agregada exitosamente.' AS Mensaje;
        END IF;
        
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product_config'
            AND column_name = 'ProductUnitName'
        ) THEN
            ALTER TABLE `product_config` ADD COLUMN `ProductUnitName` varchar(32) NOT NULL DEFAULT 'NIU' COMMENT 'Unidad visible usada para operar el producto' AFTER `NumDiscountMax`;
            SELECT 'Columna ProductUnitName agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product_config'
            AND column_name = 'ProductUnitFactor'
        ) THEN
            ALTER TABLE `product_config` ADD COLUMN `ProductUnitFactor` int NOT NULL DEFAULT '1' COMMENT 'Multiplicador desde cantidad visible hacia unidad minima' AFTER `ProductUnitName`;
            SELECT 'Columna ProductUnitFactor agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'product_config'
            AND constraint_name = 'chk_product_config_product_unit_factor'
        ) THEN
            ALTER TABLE `product_config` ADD CONSTRAINT `chk_product_config_product_unit_factor`
            CHECK (`ProductUnitFactor` >= 1);
            SELECT 'Check chk_product_config_product_unit_factor agregado exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'product_config'
            AND index_name = 'PRIMARY' AND column_name = 'StoreCod' AND seq_in_index = 2
        ) THEN
            IF NOT EXISTS (
                SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'product_config'
                AND index_name = 'idx_product_config_productcod_tmp'
            ) THEN
                ALTER TABLE `product_config` ADD KEY `idx_product_config_productcod_tmp` (`ProductCod`);
            END IF;

            IF EXISTS (
                SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'product_config'
                AND constraint_type = 'PRIMARY KEY'
            ) THEN
                ALTER TABLE `product_config` DROP PRIMARY KEY;
            END IF;

            INSERT INTO `product_config` (
                `ProductCod`, `StoreCod`, `NumPrice`, `NumMaxStock`, `NumMinStock`,
                `IsDiscontable`, `DiscountType`, `NumDiscountMax`, `ProductUnitName`,
                `ProductUnitFactor`, `Version`, `CreationUser`, `CreationDate`,
                `ModifyUser`, `ModifyDate`, `Status`
            )
            SELECT
                base.`ProductCod`, s.`StoreCod`, base.`NumPrice`, base.`NumMaxStock`, base.`NumMinStock`,
                base.`IsDiscontable`, base.`DiscountType`, base.`NumDiscountMax`, base.`ProductUnitName`,
                base.`ProductUnitFactor`, base.`Version`, base.`CreationUser`, base.`CreationDate`,
                base.`ModifyUser`, base.`ModifyDate`, base.`Status`
            FROM (
                SELECT pc.*
                FROM `product_config` pc
                JOIN (
                    SELECT `ProductCod`, MIN(`StoreCod`) AS `StoreCod`
                    FROM `product_config`
                    GROUP BY `ProductCod`
                ) b ON b.`ProductCod` = pc.`ProductCod` AND b.`StoreCod` = pc.`StoreCod`
            ) base
            CROSS JOIN `store` s
            LEFT JOIN `product_config` existing
                ON existing.`ProductCod` = base.`ProductCod`
                AND existing.`StoreCod` = s.`StoreCod`
            WHERE existing.`ProductCod` IS NULL;

            ALTER TABLE `product_config` MODIFY COLUMN `StoreCod` varchar(4) NOT NULL COMMENT 'codigo de tienda';
            ALTER TABLE `product_config` ADD PRIMARY KEY (`ProductCod`,`StoreCod`);

            IF EXISTS (
                SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'product_config'
                AND index_name = 'idx_product_config_productcod_tmp'
            ) THEN
                ALTER TABLE `product_config` DROP KEY `idx_product_config_productcod_tmp`;
            END IF;

            SELECT 'Primary key de product_config actualizada por producto/local.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'product_config'
            AND index_name = 'fk_product_config_store'
        ) THEN
            ALTER TABLE `product_config` ADD KEY `fk_product_config_store` (`StoreCod`);
            SELECT 'Indice fk_product_config_store agregado exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'product_config'
            AND constraint_name = 'fk_product_config_store'
        ) THEN
            ALTER TABLE `product_config` ADD CONSTRAINT `fk_product_config_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`);
            SELECT 'FK fk_product_config_store agregada exitosamente.' AS Mensaje;
        END IF;
        
        SELECT 'Tabla product_config ya existe. No se realizaron cambios estructurales.' AS Mensaje;

    END IF;

END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_product_config`();
DROP PROCEDURE `p_manage_product_config`;
