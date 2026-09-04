DROP PROCEDURE IF EXISTS `p_manage_sunat_submission`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sunat_submission`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'sunat_submission'
    ) THEN
        CREATE TABLE `sunat_submission` (
          `SunatSubmissionCod` varchar(20) NOT NULL COMMENT 'PK. Codigo unico del envio registrado antes de invocar el servicio SUNAT',
          `StoreCod` varchar(4) NOT NULL COMMENT 'FK. Codigo del local que origina el documento electronico',
          `SourceModule` varchar(32) NOT NULL COMMENT 'Codigo descriptivo del modulo origen, por ejemplo SALE, CREDIT_NOTE o TRANSFER',
          `SourceDocumentCod` varchar(24) NOT NULL COMMENT 'Codigo del documento de negocio que origina el envio',
          `SourceDocumentType` varchar(32) NOT NULL COMMENT 'Tipo funcional del documento de negocio origen',
          `SunatDocumentType` char(2) NOT NULL COMMENT 'Tipo de documento SUNAT: 01=Factura, 03=Boleta, 07=Nota de credito, 08=Nota de debito, 09=Guia de remision',
          `Series` varchar(8) NOT NULL COMMENT 'Serie fiscal del documento enviado a SUNAT',
          `Correlative` int NOT NULL COMMENT 'Numero correlativo fiscal del documento enviado a SUNAT',
          `RequestType` varchar(32) NOT NULL COMMENT 'Codigo descriptivo del payload: INVOICE, RECEIPT, CREDIT_NOTE, DEBIT_NOTE o DESPATCH_ADVICE',
          `EndpointKey` varchar(64) NOT NULL COMMENT 'Codigo de configuracion usado para resolver la URL del servicio SUNAT',
          `PayloadJson` longtext NOT NULL COMMENT 'Copia exacta en JSON del payload disponible para trazabilidad y reenvio manual',
          `SendStatus` char(1) NOT NULL DEFAULT 'P' COMMENT 'Estado del envio local: P=Pendiente, W=Enviando, S=Enviado, E=Error',
          `SunatStatus` varchar(3) DEFAULT NULL COMMENT 'Ultimo estado electronico informado por el servicio SUNAT, por ejemplo ACE, OBS, REJ, TCK o ERR',
          `RemoteSunatDocumentCod` varchar(24) DEFAULT NULL COMMENT 'Codigo del documento asignado por el servicio de facturacion SUNAT',
          `SunatTicket` varchar(128) DEFAULT NULL COMMENT 'Ultimo ticket informado por SUNAT cuando el procesamiento es asincrono',
          `AttemptCount` int NOT NULL DEFAULT 0 COMMENT 'Cantidad total de intentos HTTP iniciados para el documento',
          `LastAttemptDate` datetime DEFAULT NULL COMMENT 'Fecha y hora de inicio del ultimo intento de envio',
          `LastSuccessDate` datetime DEFAULT NULL COMMENT 'Fecha y hora del ultimo envio procesado correctamente',
          `LastAttemptUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que origino el ultimo intento, incluido SISTEMA para procesos en segundo plano',
          `LastResponseStatus` varchar(32) DEFAULT NULL COMMENT 'Estado HTTP o estado general de la ultima respuesta del servicio SUNAT',
          `LastResponseJson` longtext DEFAULT NULL COMMENT 'Ultima respuesta completa del servicio SUNAT o cuerpo devuelto ante error HTTP',
          `LastErrorReason` longtext DEFAULT NULL COMMENT 'Motivo funcional o tecnico por el que el ultimo intento no se proceso correctamente',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro por primera vez el envio SUNAT',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion del registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`SunatSubmissionCod`),
          UNIQUE KEY `uk_sunat_submission_source` (`SourceModule`,`SourceDocumentCod`,`SunatDocumentType`),
          KEY `idx_sunat_submission_tray` (`Status`,`SendStatus`,`CreationDate`),
          KEY `idx_sunat_submission_store` (`StoreCod`,`CreationDate`),
          KEY `idx_sunat_submission_last_attempt` (`SendStatus`,`LastAttemptDate`),
          CONSTRAINT `fk_sunat_submission_store`
              FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `chk_sunat_submission_send_status`
              CHECK (`SendStatus` in (_utf8mb4'P',_utf8mb4'W',_utf8mb4'S',_utf8mb4'E')),
          CONSTRAINT `chk_sunat_submission_attempt_count`
              CHECK (`AttemptCount` >= 0),
          CONSTRAINT `chk_sunat_submission_status`
              CHECK (`Status` in (_utf8mb4'A',_utf8mb4'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Bandeja local y payload de los documentos enviados al servicio SUNAT';
    ELSE
        ALTER TABLE `sunat_submission`
            MODIFY COLUMN `SunatSubmissionCod` varchar(20) NOT NULL COMMENT 'PK. Codigo unico del envio registrado antes de invocar el servicio SUNAT',
            MODIFY COLUMN `StoreCod` varchar(4) NOT NULL COMMENT 'FK. Codigo del local que origina el documento electronico',
            MODIFY COLUMN `SourceModule` varchar(32) NOT NULL COMMENT 'Codigo descriptivo del modulo origen, por ejemplo SALE, CREDIT_NOTE o TRANSFER',
            MODIFY COLUMN `SourceDocumentCod` varchar(24) NOT NULL COMMENT 'Codigo del documento de negocio que origina el envio',
            MODIFY COLUMN `SourceDocumentType` varchar(32) NOT NULL COMMENT 'Tipo funcional del documento de negocio origen',
            MODIFY COLUMN `SunatDocumentType` char(2) NOT NULL COMMENT 'Tipo de documento SUNAT: 01=Factura, 03=Boleta, 07=Nota de credito, 08=Nota de debito, 09=Guia de remision',
            MODIFY COLUMN `Series` varchar(8) NOT NULL COMMENT 'Serie fiscal del documento enviado a SUNAT',
            MODIFY COLUMN `Correlative` int NOT NULL COMMENT 'Numero correlativo fiscal del documento enviado a SUNAT',
            MODIFY COLUMN `RequestType` varchar(32) NOT NULL COMMENT 'Codigo descriptivo del payload: INVOICE, RECEIPT, CREDIT_NOTE, DEBIT_NOTE o DESPATCH_ADVICE',
            MODIFY COLUMN `EndpointKey` varchar(64) NOT NULL COMMENT 'Codigo de configuracion usado para resolver la URL del servicio SUNAT',
            MODIFY COLUMN `PayloadJson` longtext NOT NULL COMMENT 'Copia exacta en JSON del payload disponible para trazabilidad y reenvio manual',
            MODIFY COLUMN `SendStatus` char(1) NOT NULL DEFAULT 'P' COMMENT 'Estado del envio local: P=Pendiente, W=Enviando, S=Enviado, E=Error',
            MODIFY COLUMN `SunatStatus` varchar(3) DEFAULT NULL COMMENT 'Ultimo estado electronico informado por el servicio SUNAT, por ejemplo ACE, OBS, REJ, TCK o ERR',
            MODIFY COLUMN `RemoteSunatDocumentCod` varchar(24) DEFAULT NULL COMMENT 'Codigo del documento asignado por el servicio de facturacion SUNAT',
            MODIFY COLUMN `SunatTicket` varchar(128) DEFAULT NULL COMMENT 'Ultimo ticket informado por SUNAT cuando el procesamiento es asincrono',
            MODIFY COLUMN `AttemptCount` int NOT NULL DEFAULT 0 COMMENT 'Cantidad total de intentos HTTP iniciados para el documento',
            MODIFY COLUMN `LastAttemptDate` datetime DEFAULT NULL COMMENT 'Fecha y hora de inicio del ultimo intento de envio',
            MODIFY COLUMN `LastSuccessDate` datetime DEFAULT NULL COMMENT 'Fecha y hora del ultimo envio procesado correctamente',
            MODIFY COLUMN `LastAttemptUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que origino el ultimo intento, incluido SISTEMA para procesos en segundo plano',
            MODIFY COLUMN `LastResponseStatus` varchar(32) DEFAULT NULL COMMENT 'Estado HTTP o estado general de la ultima respuesta del servicio SUNAT',
            MODIFY COLUMN `LastResponseJson` longtext DEFAULT NULL COMMENT 'Ultima respuesta completa del servicio SUNAT o cuerpo devuelto ante error HTTP',
            MODIFY COLUMN `LastErrorReason` longtext DEFAULT NULL COMMENT 'Motivo funcional o tecnico por el que el ultimo intento no se proceso correctamente',
            MODIFY COLUMN `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que registro por primera vez el envio SUNAT',
            MODIFY COLUMN `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del registro',
            MODIFY COLUMN `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion del registro',
            MODIFY COLUMN `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion del registro',
            MODIFY COLUMN `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo';
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sunat_submission`();
DROP PROCEDURE `p_manage_sunat_submission`;
