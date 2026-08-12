DROP PROCEDURE IF EXISTS `p_manage_client_address`;

DELIMITER $$

CREATE PROCEDURE `p_manage_client_address`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE v_column_exists INT DEFAULT 0;
    DECLARE v_index_exists INT DEFAULT 0;
    DECLARE v_constraint_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'client_address';

    IF v_table_exists = 0 THEN
        CREATE TABLE `client_address` (
          `ClientAddressID` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno de la direccion del cliente',
          `ClientCod` varchar(16) NOT NULL COMMENT 'Codigo del cliente propietario de la direccion',
          `Alias` varchar(64) DEFAULT NULL COMMENT 'Nombre corto para identificar la direccion, por ejemplo Casa u Oficina',
          `Names` varchar(256) NOT NULL COMMENT 'Nombres de la persona que recibira pedidos en esta direccion',
          `Phone` varchar(20) NOT NULL COMMENT 'Telefono de contacto para la entrega',
          `Address` varchar(256) NOT NULL COMMENT 'Direccion escrita de entrega',
          `Reference` varchar(256) DEFAULT NULL COMMENT 'Referencia adicional para ubicar la direccion',
          `CountryCod` varchar(3) DEFAULT NULL COMMENT 'Codigo ISO3 del pais seleccionado',
          `CountryName` varchar(150) DEFAULT NULL COMMENT 'Nombre del pais fotografiado al registrar la direccion',
          `StateName` varchar(150) DEFAULT NULL COMMENT 'Nombre del estado o departamento fotografiado al registrar la direccion',
          `CityName` varchar(150) DEFAULT NULL COMMENT 'Nombre de la ciudad o distrito fotografiado al registrar la direccion',
          `UbigeoCod` varchar(12) DEFAULT NULL COMMENT 'Codigo de distrito para Peru o codigo territorial ingresado para otros paises',
          `Latitude` decimal(10,8) DEFAULT NULL COMMENT 'Latitud geografica de la direccion',
          `Longitude` decimal(11,8) DEFAULT NULL COMMENT 'Longitud geografica de la direccion',
          `Instructions` varchar(256) DEFAULT NULL COMMENT 'Indicaciones adicionales para realizar la entrega',
          `IsDefault` char(1) NOT NULL DEFAULT 'N' COMMENT 'Indica si es la direccion predeterminada del cliente (S/N)',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`ClientAddressID`),
          KEY `idx_client_address_client` (`ClientCod`,`Status`,`IsDefault`),
          KEY `idx_client_address_country` (`CountryCod`),
          CONSTRAINT `fk_client_address_client` FOREIGN KEY (`ClientCod`) REFERENCES `client` (`ClientCod`),
          CONSTRAINT `fk_client_address_country` FOREIGN KEY (`CountryCod`) REFERENCES `country` (`CountryCod`),
          CONSTRAINT `chk_client_address_default` CHECK (`IsDefault` IN ('S','N')),
          CONSTRAINT `chk_client_address_latitude` CHECK (`Latitude` IS NULL OR (`Latitude` BETWEEN -90 AND 90)),
          CONSTRAINT `chk_client_address_longitude` CHECK (`Longitude` IS NULL OR (`Longitude` BETWEEN -180 AND 180)),
          CONSTRAINT `chk_client_address_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla client_address creada desde cero.' AS Mensaje;
    ELSE
        SELECT COUNT(*) INTO v_column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'client_address'
          AND column_name = 'CountryCod';

        IF v_column_exists = 0 THEN
            ALTER TABLE `client_address`
                ADD COLUMN `CountryCod` varchar(3) DEFAULT NULL
                    COMMENT 'Codigo ISO3 del pais seleccionado' AFTER `Reference`;
        END IF;

        SELECT COUNT(*) INTO v_column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'client_address'
          AND column_name = 'CountryName';

        IF v_column_exists = 0 THEN
            ALTER TABLE `client_address`
                ADD COLUMN `CountryName` varchar(150) DEFAULT NULL
                    COMMENT 'Nombre del pais fotografiado al registrar la direccion' AFTER `CountryCod`;
        END IF;

        SELECT COUNT(*) INTO v_column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'client_address'
          AND column_name = 'StateName';

        IF v_column_exists = 0 THEN
            ALTER TABLE `client_address`
                ADD COLUMN `StateName` varchar(150) DEFAULT NULL
                    COMMENT 'Nombre del estado o departamento fotografiado al registrar la direccion' AFTER `CountryName`;
        END IF;

        SELECT COUNT(*) INTO v_column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'client_address'
          AND column_name = 'CityName';

        IF v_column_exists = 0 THEN
            ALTER TABLE `client_address`
                ADD COLUMN `CityName` varchar(150) DEFAULT NULL
                    COMMENT 'Nombre de la ciudad o distrito fotografiado al registrar la direccion' AFTER `StateName`;
        END IF;

        ALTER TABLE `client_address`
            MODIFY COLUMN `UbigeoCod` varchar(12) DEFAULT NULL
                COMMENT 'Codigo de distrito para Peru o codigo territorial ingresado para otros paises';

        UPDATE `client_address` ca
        INNER JOIN `ubigeo_district` ud
            ON ud.`DistrictCod` = ca.`UbigeoCod`
        INNER JOIN `ubigeo_department` dp
            ON dp.`DepartmentCod` = ud.`DepartmentCod`
        INNER JOIN `country` c
            ON c.`CountryCod` = 'PER'
        SET ca.`CountryCod` = c.`CountryCod`,
            ca.`CountryName` = c.`CountryName`,
            ca.`StateName` = dp.`Name`,
            ca.`CityName` = ud.`Name`
        WHERE ca.`CountryCod` IS NULL
           OR ca.`CountryName` IS NULL
           OR ca.`StateName` IS NULL
           OR ca.`CityName` IS NULL;

        SELECT COUNT(*) INTO v_index_exists
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'client_address'
          AND index_name = 'idx_client_address_country';

        IF v_index_exists = 0 THEN
            ALTER TABLE `client_address`
                ADD KEY `idx_client_address_country` (`CountryCod`);
        END IF;

        SELECT COUNT(*) INTO v_constraint_exists
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'client_address'
          AND constraint_name = 'fk_client_address_country';

        IF v_constraint_exists = 0 THEN
            ALTER TABLE `client_address`
                ADD CONSTRAINT `fk_client_address_country`
                    FOREIGN KEY (`CountryCod`) REFERENCES `country` (`CountryCod`);
        END IF;

        SELECT 'Tabla client_address actualizada con ubicacion internacional.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_client_address`();
DROP PROCEDURE `p_manage_client_address`;
