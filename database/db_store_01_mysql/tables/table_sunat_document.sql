DROP PROCEDURE IF EXISTS `p_manage_sunat_document`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sunat_document`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sunat_document';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sunat_document` (
          `SunatDocumentCod` varchar(24) NOT NULL,
          `SunatConfigCod` varchar(24) NOT NULL,
          `SourceModule` varchar(32) NOT NULL COMMENT 'Modulo origen: SALE, CREDIT_NOTE u otro contrato futuro',
          `SourceDocumentCod` varchar(32) NOT NULL COMMENT 'Codigo del documento comercial origen',
          `SourceDocumentType` varchar(16) DEFAULT NULL COMMENT 'Tipo de documento comercial origen',
          `SunatDocumentType` char(2) NOT NULL COMMENT '01, 03, 07, 08, RC o RA',
          `Series` varchar(8) NOT NULL,
          `Correlative` int NOT NULL,
          `FullDocumentNumber` varchar(32) NOT NULL,
          `IssuerRuc` varchar(11) NOT NULL,
          `IssueDate` datetime NOT NULL,
          `CurrencyCod` varchar(5) NOT NULL,
          `NumTotalPrice` decimal(16,2) NOT NULL DEFAULT 0,
          `NumTotalTax` decimal(16,2) NOT NULL DEFAULT 0,
          `Environment` varchar(16) NOT NULL,
          `ElectronicStatus` varchar(4) NOT NULL DEFAULT 'PEN' COMMENT 'PEN, GEN, FIR, ZIP, ENV, TCK, ACE, OBS, REJ, ERR, RET, ANU',
          `TicketSunat` varchar(128) DEFAULT NULL,
          `SunatResponseCode` varchar(16) DEFAULT NULL,
          `SunatResponseDescription` varchar(500) DEFAULT NULL,
          `SunatObservations` text DEFAULT NULL,
          `TechnicalResponse` longtext DEFAULT NULL,
          `LastTechnicalError` longtext DEFAULT NULL,
          `LastFunctionalError` varchar(500) DEFAULT NULL,
          `LastErrorType` varchar(16) DEFAULT NULL COMMENT 'INTERNAL, FUNCTIONAL o SUNAT',
          `SendAttemptCount` int NOT NULL DEFAULT 0,
          `TicketAttemptCount` int NOT NULL DEFAULT 0,
          `AcceptedDate` datetime DEFAULT NULL,
          `RejectedDate` datetime DEFAULT NULL,
          `OriginalSunatDocumentCod` varchar(24) DEFAULT NULL,
          `RelatedDocumentNumber` varchar(32) DEFAULT NULL,
          `RelatedDocumentType` char(2) DEFAULT NULL,
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`SunatDocumentCod`),
          UNIQUE KEY `uk_sunat_document_number` (`IssuerRuc`, `Environment`, `SunatDocumentType`, `Series`, `Correlative`),
          UNIQUE KEY `uk_sunat_document_source` (`SourceModule`, `SourceDocumentCod`, `SunatDocumentType`),
          KEY `fk_sunat_document_config` (`SunatConfigCod`),
          KEY `fk_sunat_document_original` (`OriginalSunatDocumentCod`),
          KEY `idx_sunat_document_status` (`ElectronicStatus`, `Status`),
          KEY `idx_sunat_document_ticket` (`TicketSunat`),
          CONSTRAINT `fk_sunat_document_config` FOREIGN KEY (`SunatConfigCod`) REFERENCES `sunat_config` (`SunatConfigCod`),
          CONSTRAINT `fk_sunat_document_original` FOREIGN KEY (`OriginalSunatDocumentCod`) REFERENCES `sunat_document` (`SunatDocumentCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sunat_document creada desde cero.' AS Mensaje;
    ELSE
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sunat_document'
              AND column_name = 'LastFunctionalError'
        ) THEN
            ALTER TABLE `sunat_document`
                ADD COLUMN `LastFunctionalError` varchar(500) DEFAULT NULL AFTER `LastTechnicalError`;
            SELECT 'Columna LastFunctionalError agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sunat_document'
              AND column_name = 'LastErrorType'
        ) THEN
            ALTER TABLE `sunat_document`
                ADD COLUMN `LastErrorType` varchar(16) DEFAULT NULL COMMENT 'INTERNAL, FUNCTIONAL o SUNAT' AFTER `LastFunctionalError`;
            SELECT 'Columna LastErrorType agregada exitosamente.' AS Mensaje;
        END IF;

        SELECT 'Tabla sunat_document ya existe. Validacion de estructura completada.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sunat_document`();
DROP PROCEDURE `p_manage_sunat_document`;
