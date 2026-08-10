DROP PROCEDURE IF EXISTS `p_manage_client_address`;

DELIMITER $$

CREATE PROCEDURE `p_manage_client_address`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

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
          `UbigeoCod` varchar(12) DEFAULT NULL COMMENT 'Codigo de ubigeo de la direccion',
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
          CONSTRAINT `fk_client_address_client` FOREIGN KEY (`ClientCod`) REFERENCES `client` (`ClientCod`),
          CONSTRAINT `chk_client_address_default` CHECK (`IsDefault` IN ('S','N')),
          CONSTRAINT `chk_client_address_latitude` CHECK (`Latitude` IS NULL OR (`Latitude` BETWEEN -90 AND 90)),
          CONSTRAINT `chk_client_address_longitude` CHECK (`Longitude` IS NULL OR (`Longitude` BETWEEN -180 AND 180)),
          CONSTRAINT `chk_client_address_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla client_address creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla client_address ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_client_address`();
DROP PROCEDURE `p_manage_client_address`;
