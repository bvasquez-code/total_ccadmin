DROP PROCEDURE IF EXISTS `p_manage_sale_head`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sale_head`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;
    
    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'sale_head';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
        CREATE TABLE `sale_head` (
          `SaleCod` varchar(16) NOT NULL COMMENT 'codigo de venta',
          `PresaleCod` varchar(16) NOT NULL COMMENT 'codigo de preventa',
          `StoreCod` varchar(4) NOT NULL COMMENT 'codigo de tienda',
          `ClientCod` varchar(16) DEFAULT NULL COMMENT 'codigo de cliente',
          `NumPriceSubTotal` decimal(16,2) NOT NULL COMMENT 'Precio subtotal',
          `NumDiscount` decimal(16,2) NOT NULL COMMENT 'Monto de descuento',
          `NumTotalPrice` decimal(16,2) NOT NULL COMMENT 'Precio total',
          `NumTotalPriceNoTax` decimal(16,2) NOT NULL COMMENT 'Precio total sin impuestos',
          `NumTotalTax` decimal(16,2) NOT NULL COMMENT 'Total de impuestos',
          `Commenter` varchar(128) DEFAULT NULL COMMENT 'comentario sobre la venta',
          `PeriodId` int NOT NULL COMMENT 'Periodo Is',
          `SaleStatus` char(1) NOT NULL DEFAULT 'P' COMMENT 'estado de la preventa',
          `CurrencyCod` varchar(5) NOT NULL COMMENT 'codigo de moneda',
          `CurrencyCodSys` varchar(5) NOT NULL COMMENT 'codigo de moneda del sistema',
          `NumExchangevalue` decimal(16,4) DEFAULT NULL COMMENT 'valor de cambio',
          `IsPaid` char(1) NOT NULL DEFAULT 'N',
          `HasCreditNote` char(1) NOT NULL DEFAULT 'N' COMMENT 'Tiene nota de credito (S/N)',
          `HasFiscalDocument` char(1) NOT NULL DEFAULT 'N' COMMENT 'Indica si la venta ya tiene boleta o factura emitida (S/N)',
          `IsPickingConfirmed` char(1) NOT NULL DEFAULT 'N' COMMENT 'Indica si el pickeo por lote fue confirmado (S/N)',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`SaleCod`),
          KEY `fk_sale_head_presale` (`PresaleCod`),
          KEY `fk_sale_head_client` (`ClientCod`),
          KEY `fk_sale_head_period` (`PeriodId`),
          KEY `fk_sale_head_store` (`StoreCod`),
          KEY `fk_sale_head_currency` (`CurrencyCod`),
          KEY `fk_sale_head_currencySys` (`CurrencyCodSys`),
          CONSTRAINT `fk_sale_head_client` FOREIGN KEY (`ClientCod`) REFERENCES `client` (`ClientCod`),
          CONSTRAINT `fk_sale_head_currency` FOREIGN KEY (`CurrencyCod`) REFERENCES `currency` (`CurrencyCod`),
          CONSTRAINT `fk_sale_head_currencySys` FOREIGN KEY (`CurrencyCodSys`) REFERENCES `currency` (`CurrencyCod`),
          CONSTRAINT `fk_sale_head_period` FOREIGN KEY (`PeriodId`) REFERENCES `period` (`PeriodId`),
          CONSTRAINT `fk_sale_head_presale` FOREIGN KEY (`PresaleCod`) REFERENCES `presale_head` (`PresaleCod`),
          CONSTRAINT `fk_sale_head_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `chk_sale_head_fiscal_document` CHECK (`HasFiscalDocument` IN ('S', 'N'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
        
        SELECT 'Tabla sale_head creada con indicadores de nota de credito y facturacion.' AS Mensaje;

    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        
        -- Verificamos si existe la columna HasCreditNote
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_head' 
            AND column_name = 'HasCreditNote'
        ) THEN
            ALTER TABLE `sale_head` ADD COLUMN `HasCreditNote` CHAR(1) NOT NULL DEFAULT 'N' COMMENT 'Tiene nota de credito (S/N)';
            SELECT 'Columna HasCreditNote agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_head'
            AND column_name = 'HasFiscalDocument'
        ) THEN
            ALTER TABLE `sale_head`
                ADD COLUMN `HasFiscalDocument` CHAR(1) NOT NULL DEFAULT 'N'
                COMMENT 'Indica si la venta ya tiene boleta o factura emitida (S/N)' AFTER `HasCreditNote`;
            SELECT 'Columna HasFiscalDocument agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'sale_head'
              AND constraint_name = 'chk_sale_head_fiscal_document'
              AND constraint_type = 'CHECK'
        ) THEN
            ALTER TABLE `sale_head`
                ADD CONSTRAINT `chk_sale_head_fiscal_document`
                CHECK (`HasFiscalDocument` IN ('S', 'N'));
            SELECT 'Restriccion chk_sale_head_fiscal_document agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sale_head'
            AND column_name = 'IsPickingConfirmed'
        ) THEN
            ALTER TABLE `sale_head`
                ADD COLUMN `IsPickingConfirmed` CHAR(1) NOT NULL DEFAULT 'N'
                COMMENT 'Indica si el pickeo por lote fue confirmado (S/N)' AFTER `HasCreditNote`;
            SELECT 'Columna IsPickingConfirmed agregada exitosamente.' AS Mensaje;
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
        ) AND EXISTS (
            SELECT * FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = 'counterfoil'
        ) THEN
            UPDATE `sale_head` sh
            SET sh.`HasFiscalDocument` = 'S'
            WHERE EXISTS (
                SELECT 1
                FROM `sale_document` sd
                INNER JOIN `counterfoil` c ON c.`CounterfoilCod` = sd.`CounterfoilCod`
                WHERE sd.`SaleCod` = sh.`SaleCod`
                  AND sd.`Status` = 'A'
                  AND c.`DocumentType` IN ('01', '03')
            );
        END IF;

        -- Aqui puedes agregar mas bloques IF NOT EXISTS para otros ALTER futuros...

    END IF;

END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_sale_head`();
DROP PROCEDURE `p_manage_sale_head`;
