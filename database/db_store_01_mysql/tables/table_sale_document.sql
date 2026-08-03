DROP PROCEDURE IF EXISTS `p_manage_sale_document`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sale_document`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE v_counterfoil_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'sale_document';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_document` (
  `DocumentCod` varchar(32) NOT NULL COMMENT 'codigo de documento',
  `CounterfoilCod` char(6) NOT NULL COMMENT 'codigo talonario',
  `DocumentType` char(2) NOT NULL COMMENT 'Tipo de documento de venta: 99 proforma, 01 factura o 03 boleta',
  `DocumentRole` char(1) NOT NULL COMMENT 'Rol del documento: I interno, F fiscal u O otro',
  `SaleCod` varchar(16) NOT NULL COMMENT 'codigo de venta',
  `ClientCod` varchar(16) DEFAULT NULL COMMENT 'Cliente asociado al documento al momento de su emision',
  `IssueDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de emision del documento',
  `CreationUser` varchar(16) NOT NULL,
  `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ModifyUser` varchar(16) DEFAULT NULL,
  `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Status` char(1) NOT NULL DEFAULT 'A',
  PRIMARY KEY (`DocumentCod`,`SaleCod`),
  KEY `fk_sale_document_sale` (`SaleCod`),
  KEY `fk_sale_document_client` (`ClientCod`),
  KEY `idx_sale_document_sale_role` (`SaleCod`,`DocumentRole`,`Status`),
  CONSTRAINT `fk_sale_document_sale` FOREIGN KEY (`SaleCod`) REFERENCES `sale_head` (`SaleCod`),
  CONSTRAINT `fk_sale_document_client` FOREIGN KEY (`ClientCod`) REFERENCES `client` (`ClientCod`),
  CONSTRAINT `chk_sale_document_role` CHECK (`DocumentRole` IN ('I', 'F', 'O'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sale_head`
--

        SELECT 'Tabla sale_document creada desde cero.' AS Mensaje;

    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        
        SELECT COUNT(*) INTO v_counterfoil_exists
        FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'counterfoil';

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND column_name = 'DocumentType'
        ) THEN
            ALTER TABLE `sale_document`
                ADD COLUMN `DocumentType` char(2) DEFAULT NULL
                COMMENT 'Tipo de documento de venta: 99 proforma, 01 factura o 03 boleta' AFTER `CounterfoilCod`;
        END IF;

        IF v_counterfoil_exists > 0 THEN
            UPDATE `sale_document` sd
            INNER JOIN `counterfoil` c ON c.`CounterfoilCod` = sd.`CounterfoilCod`
            SET sd.`DocumentType` = c.`DocumentType`
            WHERE sd.`DocumentType` IS NULL OR sd.`DocumentType` = '';
        END IF;

        UPDATE `sale_document`
        SET `DocumentType` = CASE
            WHEN `DocumentCod` LIKE 'F%' THEN '01'
            WHEN `DocumentCod` LIKE 'B%' THEN '03'
            WHEN `DocumentCod` LIKE 'P%' THEN '99'
            ELSE 'OT'
        END
        WHERE `DocumentType` IS NULL OR `DocumentType` = '';

        ALTER TABLE `sale_document`
            MODIFY COLUMN `DocumentType` char(2) NOT NULL
            COMMENT 'Tipo de documento de venta: 99 proforma, 01 factura o 03 boleta';

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND column_name = 'DocumentRole'
        ) THEN
            ALTER TABLE `sale_document`
                ADD COLUMN `DocumentRole` char(1) DEFAULT NULL
                COMMENT 'Rol del documento: I interno, F fiscal u O otro' AFTER `DocumentType`;
        END IF;

        UPDATE `sale_document`
        SET `DocumentRole` = CASE
            WHEN `DocumentType` = '99' THEN 'I'
            WHEN `DocumentType` IN ('01', '03') THEN 'F'
            ELSE 'O'
        END
        WHERE `DocumentRole` IS NULL OR `DocumentRole` = '';

        ALTER TABLE `sale_document`
            MODIFY COLUMN `DocumentRole` char(1) NOT NULL
            COMMENT 'Rol del documento: I interno, F fiscal u O otro';

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND column_name = 'ClientCod'
        ) THEN
            ALTER TABLE `sale_document`
                ADD COLUMN `ClientCod` varchar(16) DEFAULT NULL
                COMMENT 'Cliente asociado al documento al momento de su emision' AFTER `SaleCod`;
        END IF;

        UPDATE `sale_document` sd
        INNER JOIN `sale_head` sh ON sh.`SaleCod` = sd.`SaleCod`
        SET sd.`ClientCod` = sh.`ClientCod`
        WHERE sd.`ClientCod` IS NULL AND sh.`ClientCod` IS NOT NULL;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND column_name = 'IssueDate'
        ) THEN
            ALTER TABLE `sale_document`
                ADD COLUMN `IssueDate` datetime DEFAULT NULL
                COMMENT 'Fecha de emision del documento' AFTER `ClientCod`;
        END IF;

        UPDATE `sale_document`
        SET `IssueDate` = COALESCE(`IssueDate`, `CreationDate`, CURRENT_TIMESTAMP)
        WHERE `IssueDate` IS NULL;

        ALTER TABLE `sale_document`
            MODIFY COLUMN `IssueDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
            COMMENT 'Fecha de emision del documento';

        IF NOT EXISTS (
            SELECT * FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND index_name = 'idx_sale_document_sale_role'
        ) THEN
            CREATE INDEX `idx_sale_document_sale_role`
                ON `sale_document` (`SaleCod`, `DocumentRole`, `Status`);
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND constraint_name = 'chk_sale_document_role'
              AND constraint_type = 'CHECK'
        ) THEN
            ALTER TABLE `sale_document`
                ADD CONSTRAINT `chk_sale_document_role`
                CHECK (`DocumentRole` IN ('I', 'F', 'O'));
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND index_name = 'fk_sale_document_client'
        ) THEN
            CREATE INDEX `fk_sale_document_client` ON `sale_document` (`ClientCod`);
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'sale_document'
              AND constraint_name = 'fk_sale_document_client'
              AND constraint_type = 'FOREIGN KEY'
        ) THEN
            ALTER TABLE `sale_document`
                ADD CONSTRAINT `fk_sale_document_client`
                FOREIGN KEY (`ClientCod`) REFERENCES `client` (`ClientCod`);
        END IF;

        SELECT 'Tabla sale_document regularizada para multiples documentos.' AS Mensaje;

    END IF;

END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_sale_document`();
DROP PROCEDURE `p_manage_sale_document`;
