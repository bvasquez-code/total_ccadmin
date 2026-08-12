DROP PROCEDURE IF EXISTS `p_manage_city`;

DELIMITER $$

CREATE PROCEDURE `p_manage_city`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'city';

    IF v_table_exists = 0 THEN
        CREATE TABLE `city` (
          `CityId` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno de la ciudad o localidad',
          `StateId` bigint NOT NULL COMMENT 'Identificador del estado, region o departamento al que pertenece la ciudad',
          `CityName` varchar(150) NOT NULL COMMENT 'Nombre oficial de la ciudad o localidad',
          `CityNameAscii` varchar(150) DEFAULT NULL COMMENT 'Nombre de la ciudad o localidad normalizado en caracteres ASCII',
          `Latitude` double DEFAULT NULL COMMENT 'Latitud geografica referencial de la ciudad expresada en grados decimales',
          `Longitude` double DEFAULT NULL COMMENT 'Longitud geografica referencial de la ciudad expresada en grados decimales',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`CityId`),
          UNIQUE KEY `uk_city_state_name` (`StateId`,`CityName`),
          KEY `idx_city_state` (`StateId`),
          CONSTRAINT `fk_city_state` FOREIGN KEY (`StateId`) REFERENCES `state` (`StateId`),
          CONSTRAINT `chk_city_latitude` CHECK (`Latitude` IS NULL OR `Latitude` BETWEEN -90 AND 90),
          CONSTRAINT `chk_city_longitude` CHECK (`Longitude` IS NULL OR `Longitude` BETWEEN -180 AND 180),
          CONSTRAINT `chk_city_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Catalogo internacional de ciudades y localidades asociado a estados o regiones';

        SELECT 'Tabla city creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla city ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_city`();
DROP PROCEDURE `p_manage_city`;
