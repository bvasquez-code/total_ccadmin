DROP PROCEDURE IF EXISTS `p_manage_country`;

DELIMITER $$

CREATE PROCEDURE `p_manage_country`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'country';

    IF v_table_exists = 0 THEN
        CREATE TABLE `country` (
          `CountryCod` varchar(3) NOT NULL COMMENT 'Codigo ISO 3166-1 alfa-3 del pais y clave primaria del catalogo',
          `CountryIso2` varchar(2) DEFAULT NULL COMMENT 'Codigo ISO 3166-1 alfa-2 del pais',
          `CountryName` varchar(150) NOT NULL COMMENT 'Nombre oficial o comercial del pais',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`CountryCod`),
          UNIQUE KEY `uk_country_iso2` (`CountryIso2`),
          KEY `idx_country_name` (`CountryName`),
          CONSTRAINT `chk_country_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Catalogo internacional de paises utilizado para ubicaciones y direcciones';

        SELECT 'Tabla country creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla country ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_country`();
DROP PROCEDURE `p_manage_country`;
