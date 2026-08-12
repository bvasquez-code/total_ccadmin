DROP PROCEDURE IF EXISTS `p_manage_trx_payments_document`;

DELIMITER $$

CREATE PROCEDURE `p_manage_trx_payments_document`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'trx_payments_document';

    IF v_table_exists = 0 THEN
        CREATE TABLE `trx_payments_document` (
          `TrxPaymentDocumentId` bigint NOT NULL AUTO_INCREMENT
              COMMENT 'Identificador unico del documento o informacion asociada al pago',
          `TrxPaymentId` bigint NOT NULL
              COMMENT 'Identificador del pago al que pertenece el documento o informacion',
          `DocumentType` varchar(32) NOT NULL
              COMMENT 'Tipo funcional de informacion: PAYMENT_PROOF, PINPAD_RECEIPT, PINPAD_RESPONSE u OTHER',
          `ContentEncoding` varchar(16) NOT NULL
              COMMENT 'Formato almacenado en Content: BASE64, TEXT o JSON',
          `Content` longtext NOT NULL
              COMMENT 'Contenido del documento en Base64 o texto devuelto por el dispositivo, segun ContentEncoding',
          `FileName` varchar(255) DEFAULT NULL
              COMMENT 'Nombre original del archivo cuando el contenido proviene de un documento adjunto',
          `ContentType` varchar(100) DEFAULT NULL
              COMMENT 'Tipo MIME del contenido, por ejemplo image/jpeg, image/png, application/pdf o text/plain',
          `SizeBytes` bigint DEFAULT NULL
              COMMENT 'Tamanio en bytes del contenido original antes de convertirlo a Base64',
          `Sha256Hash` char(64) DEFAULT NULL
              COMMENT 'Hash SHA-256 del contenido original para verificar integridad o detectar duplicados',
          `SourceType` varchar(16) NOT NULL DEFAULT 'WEB'
              COMMENT 'Origen de la informacion: WEB, PINPAD, POS, SYSTEM u OTHER',
          `PurgeAfterDate` datetime DEFAULT NULL
              COMMENT 'Fecha desde la cual el contenido puede ser eliminado por el proceso automatico de limpieza',
          `CreationUser` varchar(16) NOT NULL
              COMMENT 'Usuario que registro la informacion',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
              COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL
              COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
              COMMENT 'Fecha de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A'
              COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`TrxPaymentDocumentId`),
          KEY `fk_trx_payments_document_payment` (`TrxPaymentId`),
          KEY `idx_trx_payments_document_type` (`DocumentType`),
          KEY `idx_trx_payments_document_purge` (`PurgeAfterDate`, `Status`),
          CONSTRAINT `fk_trx_payments_document_payment`
              FOREIGN KEY (`TrxPaymentId`) REFERENCES `trx_payments` (`TrxPaymentId`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla trx_payments_document creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla trx_payments_document ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_trx_payments_document`();
DROP PROCEDURE `p_manage_trx_payments_document`;
