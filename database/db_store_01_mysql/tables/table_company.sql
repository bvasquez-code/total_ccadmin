DROP PROCEDURE IF EXISTS `p_manage_company`;

DELIMITER $$

CREATE PROCEDURE `p_manage_company`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'company';

    IF v_table_exists = 0 THEN
        CREATE TABLE `company` (
          `CompanyCod` varchar(4) NOT NULL COMMENT 'Codigo legible de la empresa (4 A-Z0-9). PK y referencia desde store.',
          `TaxId` char(11) NOT NULL COMMENT 'RUC de la empresa (11 digitos).',
          `LegalName` varchar(200) NOT NULL COMMENT 'Razon Social registrada ante SUNAT.',
          `TradeName` varchar(200) DEFAULT NULL COMMENT 'Nombre Comercial.',
          `FiscalAddress` varchar(300) NOT NULL COMMENT 'Domicilio fiscal.',
          `Address` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'Direccion comercial corta para impresion/etiquetas',
          `UbigeoCod` varchar(6) NOT NULL COMMENT 'Codigo UBIGEO (6 digitos) del distrito del domicilio fiscal.',
          `Department` varchar(100) DEFAULT NULL COMMENT 'Departamento del domicilio fiscal para facturacion electronica.',
          `Province` varchar(100) DEFAULT NULL COMMENT 'Provincia del domicilio fiscal para facturacion electronica.',
          `District` varchar(100) DEFAULT NULL COMMENT 'Distrito del domicilio fiscal para facturacion electronica.',
          `CountryCode` char(2) NOT NULL DEFAULT 'PE' COMMENT 'Codigo de pais ISO-3166-1 alfa-2 (p.ej. PE).',
          `Phone` varchar(30) DEFAULT NULL COMMENT 'Telefono corporativo.',
          `Email` varchar(150) DEFAULT NULL COMMENT 'Correo corporativo.',
          `Website` varchar(150) DEFAULT NULL COMMENT 'Sitio web corporativo.',
          `LogoPath` varchar(500) DEFAULT NULL COMMENT 'Ruta/URL del logo corporativo.',
          `Status` char(1) NOT NULL DEFAULT 'A',
          `CreationUser` varchar(50) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(50) DEFAULT NULL,
          `ModifyDate` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (`CompanyCod`),
          UNIQUE KEY `uq_company_taxid` (`TaxId`),
          KEY `idx_company_legalname` (`LegalName`),
          KEY `idx_company_ubigeo` (`UbigeoCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Datos empresariales (identidad, RUC, domicilio y contacto). PK legible; UBIGEO a nivel distrito.';

        SELECT 'Tabla company creada desde cero.' AS Mensaje;
    ELSE
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'company'
              AND column_name = 'Department'
        ) THEN
            ALTER TABLE `company`
                ADD COLUMN `Department` varchar(100) DEFAULT NULL COMMENT 'Departamento del domicilio fiscal para facturacion electronica.' AFTER `UbigeoCod`;
            SELECT 'Columna Department agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'company'
              AND column_name = 'Province'
        ) THEN
            ALTER TABLE `company`
                ADD COLUMN `Province` varchar(100) DEFAULT NULL COMMENT 'Provincia del domicilio fiscal para facturacion electronica.' AFTER `Department`;
            SELECT 'Columna Province agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'company'
              AND column_name = 'District'
        ) THEN
            ALTER TABLE `company`
                ADD COLUMN `District` varchar(100) DEFAULT NULL COMMENT 'Distrito del domicilio fiscal para facturacion electronica.' AFTER `Province`;
            SELECT 'Columna District agregada exitosamente.' AS Mensaje;
        END IF;

        SELECT 'Tabla company ya existe. Validacion de estructura completada.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_company`();
DROP PROCEDURE `p_manage_company`;
