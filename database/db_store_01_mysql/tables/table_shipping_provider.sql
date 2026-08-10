DROP PROCEDURE IF EXISTS `p_manage_shipping_provider`;

DELIMITER $$

CREATE PROCEDURE `p_manage_shipping_provider`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'shipping_provider';

    IF v_table_exists = 0 THEN
        CREATE TABLE `shipping_provider` (
          `ShippingProviderCod` varchar(16) NOT NULL COMMENT 'Codigo del proveedor encargado del envio',
          `ProviderType` varchar(16) NOT NULL COMMENT 'Tipo de proveedor (INTERNAL, COURIER, AGENCY u OTHER)',
          `Name` varchar(128) NOT NULL COMMENT 'Nombre comercial o visible del proveedor',
          `BusinessName` varchar(128) DEFAULT NULL COMMENT 'Razon social del proveedor',
          `DocumentNumber` varchar(16) DEFAULT NULL COMMENT 'Numero de documento tributario o de identidad del proveedor',
          `Phone` varchar(20) DEFAULT NULL COMMENT 'Telefono de contacto del proveedor',
          `Email` varchar(128) DEFAULT NULL COMMENT 'Correo electronico de contacto del proveedor',
          `TrackingUrl` varchar(512) DEFAULT NULL COMMENT 'Direccion web utilizada para consultar el seguimiento',
          `Description` varchar(256) DEFAULT NULL COMMENT 'Descripcion u observaciones del proveedor',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`ShippingProviderCod`),
          KEY `idx_shipping_provider_type` (`ProviderType`,`Status`),
          KEY `idx_shipping_provider_document` (`DocumentNumber`),
          CONSTRAINT `chk_shipping_provider_type` CHECK (`ProviderType` IN ('INTERNAL','COURIER','AGENCY','OTHER')),
          CONSTRAINT `chk_shipping_provider_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla shipping_provider creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla shipping_provider ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_shipping_provider`();
DROP PROCEDURE `p_manage_shipping_provider`;
