DROP PROCEDURE IF EXISTS `p_manage_sale_delivery`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sale_delivery`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE v_constraint_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sale_delivery';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sale_delivery` (
          `SaleCod` varchar(16) NOT NULL COMMENT 'Codigo de la venta relacionada',
          `DeliveryTypeCod` varchar(24) NOT NULL COMMENT 'Codigo de la modalidad de entrega o recojo',
          `DeliveryStatus` char(1) NOT NULL DEFAULT 'P' COMMENT 'Estado operativo de la entrega (P:Pendiente, S:Programada, R:En preparacion, L:Lista para recojo, D:Despachada, E:Entregada, X:Cancelada, F:Entrega fallida)',
          `ClientAddressID` bigint DEFAULT NULL COMMENT 'Direccion reutilizable del cliente que origino la fotografia de entrega',
          `IsThirdParty` char(1) NOT NULL DEFAULT 'N' COMMENT 'Indica si quien recibe o recoge es distinto del comprador (S/N)',
          `Names` varchar(256) NOT NULL COMMENT 'Nombres de la persona que recibira o recogera el pedido',
          `DocumentType` char(2) DEFAULT NULL COMMENT 'Tipo de documento de la persona que recibira o recogera el pedido',
          `DocumentNumber` varchar(16) DEFAULT NULL COMMENT 'Numero de documento de la persona que recibira o recogera el pedido',
          `Phone` varchar(20) NOT NULL COMMENT 'Telefono de contacto de la persona que recibira o recogera el pedido',
          `Email` varchar(128) DEFAULT NULL COMMENT 'Correo de contacto asociado a la entrega',
          `Address` varchar(256) DEFAULT NULL COMMENT 'Fotografia de la direccion de destino utilizada por la venta',
          `Reference` varchar(256) DEFAULT NULL COMMENT 'Referencia para ubicar el destino de la entrega',
          `UbigeoCod` varchar(12) DEFAULT NULL COMMENT 'Codigo de ubigeo del destino de la entrega',
          `Latitude` decimal(10,8) DEFAULT NULL COMMENT 'Latitud del destino utilizada al confirmar la venta',
          `Longitude` decimal(11,8) DEFAULT NULL COMMENT 'Longitud del destino utilizada al confirmar la venta',
          `Instructions` varchar(256) DEFAULT NULL COMMENT 'Indicaciones adicionales para entregar o recoger el pedido',
          `EstimatedDistanceKm` decimal(8,2) DEFAULT NULL COMMENT 'Distancia estimada en kilometros desde la tienda de la venta',
          `ScheduledFrom` datetime DEFAULT NULL COMMENT 'Inicio de la ventana programada de atencion',
          `ScheduledTo` datetime DEFAULT NULL COMMENT 'Fin de la ventana programada de atencion',
          `ShippingProviderCod` varchar(16) DEFAULT NULL COMMENT 'Codigo del proveedor asignado al envio',
          `TrackingNumber` varchar(64) DEFAULT NULL COMMENT 'Numero o codigo de seguimiento proporcionado por el proveedor',
          `AgencyName` varchar(128) DEFAULT NULL COMMENT 'Fotografia del nombre de la agencia o sucursal de destino',
          `AgencyAddress` varchar(256) DEFAULT NULL COMMENT 'Fotografia de la direccion de la agencia o sucursal de destino',
          `ReadyDate` datetime DEFAULT NULL COMMENT 'Fecha en que el pedido quedo listo para entregar o recoger',
          `DispatchDate` datetime DEFAULT NULL COMMENT 'Fecha en que el pedido fue despachado',
          `DeliveredDate` datetime DEFAULT NULL COMMENT 'Fecha en que el pedido fue entregado',
          `Commenter` varchar(256) DEFAULT NULL COMMENT 'Comentario operativo sobre la entrega',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`SaleCod`),
          KEY `idx_sale_delivery_type_status` (`DeliveryTypeCod`,`DeliveryStatus`,`Status`),
          KEY `idx_sale_delivery_client_address` (`ClientAddressID`),
          KEY `idx_sale_delivery_provider` (`ShippingProviderCod`),
          KEY `idx_sale_delivery_schedule` (`ScheduledFrom`,`ScheduledTo`),
          CONSTRAINT `fk_sale_delivery_sale` FOREIGN KEY (`SaleCod`) REFERENCES `sale_head` (`SaleCod`),
          CONSTRAINT `fk_sale_delivery_type` FOREIGN KEY (`DeliveryTypeCod`) REFERENCES `delivery_type` (`DeliveryTypeCod`),
          CONSTRAINT `fk_sale_delivery_client_address` FOREIGN KEY (`ClientAddressID`) REFERENCES `client_address` (`ClientAddressID`),
          CONSTRAINT `fk_sale_delivery_provider` FOREIGN KEY (`ShippingProviderCod`) REFERENCES `shipping_provider` (`ShippingProviderCod`),
          CONSTRAINT `chk_sale_delivery_third_party` CHECK (`IsThirdParty` IN ('S','N')),
          CONSTRAINT `chk_sale_delivery_latitude` CHECK (`Latitude` IS NULL OR (`Latitude` BETWEEN -90 AND 90)),
          CONSTRAINT `chk_sale_delivery_longitude` CHECK (`Longitude` IS NULL OR (`Longitude` BETWEEN -180 AND 180)),
          CONSTRAINT `chk_sale_delivery_distance` CHECK (`EstimatedDistanceKm` IS NULL OR `EstimatedDistanceKm` >= 0),
          CONSTRAINT `chk_sale_delivery_schedule` CHECK (`ScheduledFrom` IS NULL OR `ScheduledTo` IS NULL OR `ScheduledTo` >= `ScheduledFrom`),
          CONSTRAINT `chk_sale_delivery_status_value` CHECK (`DeliveryStatus` IN ('P','S','R','L','D','E','X','F')),
          CONSTRAINT `chk_sale_delivery_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sale_delivery creada desde cero.' AS Mensaje;
    ELSE
        SELECT COUNT(*) INTO v_constraint_exists
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'sale_delivery'
          AND constraint_name = 'chk_sale_delivery_status_value';

        IF v_constraint_exists > 0 THEN
            ALTER TABLE `sale_delivery`
                DROP CHECK `chk_sale_delivery_status_value`;
        END IF;

        UPDATE `sale_delivery`
        SET `DeliveryStatus` = CASE `DeliveryStatus`
            WHEN 'PENDING' THEN 'P'
            WHEN 'SCHEDULED' THEN 'S'
            WHEN 'PREPARING' THEN 'R'
            WHEN 'READY_FOR_PICKUP' THEN 'L'
            WHEN 'DISPATCHED' THEN 'D'
            WHEN 'DELIVERED' THEN 'E'
            WHEN 'CANCELLED' THEN 'X'
            WHEN 'FAILED_DELIVERY' THEN 'F'
            ELSE `DeliveryStatus`
        END;

        ALTER TABLE `sale_delivery`
            MODIFY COLUMN `DeliveryStatus` char(1) NOT NULL DEFAULT 'P'
                COMMENT 'Estado operativo de la entrega (P:Pendiente, S:Programada, R:En preparacion, L:Lista para recojo, D:Despachada, E:Entregada, X:Cancelada, F:Entrega fallida)',
            ADD CONSTRAINT `chk_sale_delivery_status_value`
                CHECK (`DeliveryStatus` IN ('P','S','R','L','D','E','X','F'));

        SELECT 'Tabla sale_delivery actualizada con estados operativos de un caracter.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sale_delivery`();
DROP PROCEDURE `p_manage_sale_delivery`;
