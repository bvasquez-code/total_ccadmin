DROP PROCEDURE IF EXISTS `p_manage_delivery_type`;

DELIMITER $$

CREATE PROCEDURE `p_manage_delivery_type`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'delivery_type';

    IF v_table_exists = 0 THEN
        CREATE TABLE `delivery_type` (
          `DeliveryTypeCod` varchar(24) NOT NULL COMMENT 'Codigo descriptivo de la modalidad de entrega',
          `Name` varchar(64) NOT NULL COMMENT 'Nombre visible de la modalidad de entrega',
          `Description` varchar(256) DEFAULT NULL COMMENT 'Descripcion funcional de la modalidad de entrega',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`DeliveryTypeCod`),
          CONSTRAINT `chk_delivery_type_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla delivery_type creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla delivery_type ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_delivery_type`();

INSERT INTO `delivery_type`
(`DeliveryTypeCod`, `Name`, `Description`, `CreationUser`, `Status`)
SELECT 'IN_STORE', 'Entrega presencial', 'Venta y entrega completadas en el local', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `delivery_type` WHERE `DeliveryTypeCod` = 'IN_STORE'
);

INSERT INTO `delivery_type`
(`DeliveryTypeCod`, `Name`, `Description`, `CreationUser`, `Status`)
SELECT 'DELIVERY', 'Delivery automatico', 'Entrega atendida directamente dentro del radio automatico de la tienda', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `delivery_type` WHERE `DeliveryTypeCod` = 'DELIVERY'
);

INSERT INTO `delivery_type`
(`DeliveryTypeCod`, `Name`, `Description`, `CreationUser`, `Status`)
SELECT 'SCHEDULED_DELIVERY', 'Entrega programada', 'Entrega coordinada mediante programacion, courier, agencia u otro proveedor', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `delivery_type` WHERE `DeliveryTypeCod` = 'SCHEDULED_DELIVERY'
);

INSERT INTO `delivery_type`
(`DeliveryTypeCod`, `Name`, `Description`, `CreationUser`, `Status`)
SELECT 'STORE_PICKUP', 'Recojo en tienda', 'Pedido preparado para ser recogido en la tienda de la venta', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `delivery_type` WHERE `DeliveryTypeCod` = 'STORE_PICKUP'
);

DROP PROCEDURE `p_manage_delivery_type`;
