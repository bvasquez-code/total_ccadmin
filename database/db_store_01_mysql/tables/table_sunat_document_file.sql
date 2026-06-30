DROP PROCEDURE IF EXISTS `p_manage_sunat_document_file`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sunat_document_file`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sunat_document_file';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sunat_document_file` (
          `SunatFileCod` varchar(24) NOT NULL,
          `SunatDocumentCod` varchar(24) NOT NULL,
          `FileType` varchar(16) NOT NULL COMMENT 'XML, XML_SIGNED, ZIP_SEND, CDR_ZIP, CDR_XML, RESPONSE, ERROR',
          `FileName` varchar(180) NOT NULL,
          `FilePath` varchar(700) NOT NULL,
          `ContentType` varchar(80) DEFAULT NULL,
          `SizeBytes` bigint DEFAULT NULL,
          `Sha256Hash` varchar(64) DEFAULT NULL,
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`SunatFileCod`),
          KEY `fk_sunat_document_file_document` (`SunatDocumentCod`),
          KEY `idx_sunat_document_file_type` (`FileType`),
          CONSTRAINT `fk_sunat_document_file_document` FOREIGN KEY (`SunatDocumentCod`) REFERENCES `sunat_document` (`SunatDocumentCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sunat_document_file creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla sunat_document_file ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sunat_document_file`();
DROP PROCEDURE `p_manage_sunat_document_file`;
