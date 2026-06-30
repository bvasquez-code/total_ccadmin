DROP PROCEDURE IF EXISTS `p_manage_sunat_document_attempt`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sunat_document_attempt`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sunat_document_attempt';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sunat_document_attempt` (
          `AttemptId` bigint NOT NULL AUTO_INCREMENT,
          `SunatDocumentCod` varchar(24) NOT NULL,
          `OperationType` varchar(24) NOT NULL COMMENT 'GENERATE_XML, SIGN_XML, GENERATE_ZIP, SEND, CONSULT_TICKET, RETRY, SCHEDULER',
          `Environment` varchar(16) DEFAULT NULL,
          `Endpoint` varchar(500) DEFAULT NULL,
          `AttemptNumber` int NOT NULL DEFAULT 1,
          `Success` char(1) NOT NULL DEFAULT 'N',
          `TechnicalMessage` longtext DEFAULT NULL,
          `FunctionalMessage` varchar(500) DEFAULT NULL,
          `SunatTicket` varchar(128) DEFAULT NULL,
          `SunatResponseCode` varchar(16) DEFAULT NULL,
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`AttemptId`),
          KEY `fk_sunat_document_attempt_document` (`SunatDocumentCod`),
          KEY `idx_sunat_document_attempt_operation` (`OperationType`, `Success`),
          CONSTRAINT `fk_sunat_document_attempt_document` FOREIGN KEY (`SunatDocumentCod`) REFERENCES `sunat_document` (`SunatDocumentCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sunat_document_attempt creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla sunat_document_attempt ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sunat_document_attempt`();
DROP PROCEDURE `p_manage_sunat_document_attempt`;
