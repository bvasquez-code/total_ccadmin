DROP PROCEDURE IF EXISTS `p_manage_sunat_document_payload`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sunat_document_payload`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sunat_document_payload';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sunat_document_payload` (
          `SunatDocumentCod` varchar(24) NOT NULL,
          `PayloadJson` longtext NOT NULL COMMENT 'Payload recibido por API para generar XML',
          `UnsignedXml` longtext DEFAULT NULL COMMENT 'XML UBL 2.1 generado sin firma',
          `UnsignedXmlFileName` varchar(180) DEFAULT NULL,
          `XmlGeneratedDate` datetime DEFAULT NULL,
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`SunatDocumentCod`),
          CONSTRAINT `fk_sunat_document_payload_document` FOREIGN KEY (`SunatDocumentCod`) REFERENCES `sunat_document` (`SunatDocumentCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sunat_document_payload creada desde cero.' AS Mensaje;
    ELSE
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sunat_document_payload'
              AND column_name = 'UnsignedXmlFileName'
        ) THEN
            ALTER TABLE `sunat_document_payload`
                ADD COLUMN `UnsignedXmlFileName` varchar(180) DEFAULT NULL AFTER `UnsignedXml`;
            SELECT 'Columna UnsignedXmlFileName agregada exitosamente.' AS Mensaje;
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sunat_document_payload'
              AND column_name = 'UnsignedXml'
              AND is_nullable = 'NO'
        ) THEN
            ALTER TABLE `sunat_document_payload`
                MODIFY COLUMN `UnsignedXml` longtext DEFAULT NULL COMMENT 'XML UBL 2.1 generado sin firma';
            SELECT 'Columna UnsignedXml modificada a nullable.' AS Mensaje;
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sunat_document_payload'
              AND column_name = 'UnsignedXmlFileName'
              AND is_nullable = 'NO'
        ) THEN
            ALTER TABLE `sunat_document_payload`
                MODIFY COLUMN `UnsignedXmlFileName` varchar(180) DEFAULT NULL;
            SELECT 'Columna UnsignedXmlFileName modificada a nullable.' AS Mensaje;
        END IF;

        IF EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sunat_document_payload'
              AND column_name = 'XmlGeneratedDate'
              AND is_nullable = 'NO'
        ) THEN
            ALTER TABLE `sunat_document_payload`
                MODIFY COLUMN `XmlGeneratedDate` datetime DEFAULT NULL;
            SELECT 'Columna XmlGeneratedDate modificada a nullable.' AS Mensaje;
        END IF;

        SELECT 'Tabla sunat_document_payload ya existe. Validacion de estructura completada.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sunat_document_payload`();
DROP PROCEDURE `p_manage_sunat_document_payload`;
