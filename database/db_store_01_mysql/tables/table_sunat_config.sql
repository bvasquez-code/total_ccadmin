DROP PROCEDURE IF EXISTS `p_manage_sunat_config`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sunat_config`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sunat_config';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sunat_config` (
          `SunatConfigCod` varchar(24) NOT NULL,
          `IssuerRuc` varchar(11) NOT NULL COMMENT 'RUC emisor',
          `SolUser` varchar(64) NOT NULL COMMENT 'Usuario SOL en texto plano para fase inicial',
          `SolPassword` varchar(128) NOT NULL COMMENT 'Clave SOL en texto plano para fase inicial',
          `CertificatePath` varchar(500) NOT NULL COMMENT 'Ruta del certificado digital',
          `CertificatePassword` varchar(128) NOT NULL COMMENT 'Clave del certificado digital en texto plano para fase inicial',
          `CertificateType` varchar(8) NOT NULL DEFAULT 'P12' COMMENT 'P12, PFX o JKS',
          `Environment` varchar(16) NOT NULL COMMENT 'BETA o PRODUCCION',
          `StorageBasePath` varchar(500) NOT NULL COMMENT 'Ruta base de archivos SUNAT',
          `InvoiceEndpoint` varchar(500) NOT NULL COMMENT 'Endpoint de comprobantes individuales',
          `SummaryEndpoint` varchar(500) NOT NULL COMMENT 'Endpoint de resumen diario/comunicacion de baja',
          `TicketEndpoint` varchar(500) NOT NULL COMMENT 'Endpoint de consulta ticket',
          `MaxSendAttempts` int NOT NULL DEFAULT 3,
          `MaxTicketAttempts` int NOT NULL DEFAULT 3,
          `SchedulerEnabled` char(1) NOT NULL DEFAULT 'N',
          `AutomaticRetryEnabled` char(1) NOT NULL DEFAULT 'N',
          `ActiveConfig` char(1) NOT NULL DEFAULT 'N',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`SunatConfigCod`),
          KEY `idx_sunat_config_ruc_env` (`IssuerRuc`, `Environment`),
          KEY `idx_sunat_config_active` (`ActiveConfig`, `Status`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sunat_config creada desde cero.' AS Mensaje;
    ELSE
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'sunat_config'
              AND column_name = 'AutomaticRetryEnabled'
        ) THEN
            ALTER TABLE `sunat_config`
                ADD COLUMN `AutomaticRetryEnabled` char(1) NOT NULL DEFAULT 'N' AFTER `SchedulerEnabled`;
            SELECT 'Columna AutomaticRetryEnabled agregada exitosamente.' AS Mensaje;
        END IF;

        SELECT 'Tabla sunat_config ya existe. Validacion de estructura completada.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sunat_config`();
DROP PROCEDURE `p_manage_sunat_config`;
