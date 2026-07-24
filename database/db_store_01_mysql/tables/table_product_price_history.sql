DROP PROCEDURE IF EXISTS `p_manage_product_price_history`;

DELIMITER $$

CREATE PROCEDURE `p_manage_product_price_history`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'product_price_history'
    ) THEN
        CREATE TABLE `product_price_history` (
          `ProductPriceHistoryId` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK autoincremental del cambio de precio',
          `ProductCod` varchar(20) NOT NULL COMMENT 'FK. Producto cuyo precio fue modificado',
          `StoreCod` varchar(4) NOT NULL COMMENT 'FK. Local donde se modifico el precio',
          `OldPrice` decimal(16,2) DEFAULT NULL COMMENT 'Precio vigente antes de la modificacion',
          `NewPrice` decimal(16,2) DEFAULT NULL COMMENT 'Precio vigente despues de la modificacion',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que origino el cambio en product_config',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora efectiva del cambio de precio',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro historico: A=Activo, I=Inactivo',
          PRIMARY KEY (`ProductPriceHistoryId`),
          KEY `idx_product_price_history_lookup` (`ProductCod`,`StoreCod`,`CreationDate`),
          CONSTRAINT `fk_product_price_history_product`
              FOREIGN KEY (`ProductCod`) REFERENCES `product` (`ProductCod`),
          CONSTRAINT `fk_product_price_history_store`
              FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `chk_product_price_history_status`
              CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Historico de cambios efectivos de precio por producto y local';
    ELSE
        ALTER TABLE `product_price_history`
            MODIFY COLUMN `ProductPriceHistoryId` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK autoincremental del cambio de precio',
            MODIFY COLUMN `ProductCod` varchar(20) NOT NULL COMMENT 'FK. Producto cuyo precio fue modificado',
            MODIFY COLUMN `StoreCod` varchar(4) NOT NULL COMMENT 'FK. Local donde se modifico el precio',
            MODIFY COLUMN `OldPrice` decimal(16,2) DEFAULT NULL COMMENT 'Precio vigente antes de la modificacion',
            MODIFY COLUMN `NewPrice` decimal(16,2) DEFAULT NULL COMMENT 'Precio vigente despues de la modificacion',
            MODIFY COLUMN `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que origino el cambio en product_config',
            MODIFY COLUMN `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora efectiva del cambio de precio',
            MODIFY COLUMN `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro historico: A=Activo, I=Inactivo';
    END IF;
END $$

DELIMITER ;

CALL `p_manage_product_price_history`();
DROP PROCEDURE `p_manage_product_price_history`;
