DROP PROCEDURE IF EXISTS `p_manage_store_virtual_config`;

DELIMITER $$

CREATE PROCEDURE `p_manage_store_virtual_config`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'store_virtual_config';

    IF v_table_exists = 0 THEN
        CREATE TABLE `store_virtual_config` (
          `StoreCod` varchar(4) NOT NULL COMMENT 'Codigo de la tienda configurada para venta virtual',
          `AllowsAutomaticDelivery` char(1) NOT NULL DEFAULT 'N' COMMENT 'Permite delivery automatico dentro del radio configurado (S/N)',
          `AutomaticDeliveryRadiusKm` decimal(8,2) DEFAULT NULL COMMENT 'Radio maximo en kilometros para delivery automatico',
          `AllowsScheduledDelivery` char(1) NOT NULL DEFAULT 'N' COMMENT 'Permite entregas programadas fuera del radio automatico (S/N)',
          `ScheduledDeliveryMaxRadiusKm` decimal(8,2) DEFAULT NULL COMMENT 'Radio maximo en kilometros para ofrecer entrega programada',
          `AllowsStorePickup` char(1) NOT NULL DEFAULT 'N' COMMENT 'Permite que el cliente recoja el pedido en tienda (S/N)',
          `PreparationTimeMinutes` int NOT NULL DEFAULT 0 COMMENT 'Tiempo estimado de preparacion del pedido expresado en minutos',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`StoreCod`),
          CONSTRAINT `fk_store_virtual_config_store` FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `chk_store_virtual_config_automatic` CHECK (`AllowsAutomaticDelivery` IN ('S','N')),
          CONSTRAINT `chk_store_virtual_config_scheduled` CHECK (`AllowsScheduledDelivery` IN ('S','N')),
          CONSTRAINT `chk_store_virtual_config_pickup` CHECK (`AllowsStorePickup` IN ('S','N')),
          CONSTRAINT `chk_store_virtual_config_auto_radius` CHECK (`AutomaticDeliveryRadiusKm` IS NULL OR `AutomaticDeliveryRadiusKm` >= 0),
          CONSTRAINT `chk_store_virtual_config_scheduled_radius` CHECK (`ScheduledDeliveryMaxRadiusKm` IS NULL OR `ScheduledDeliveryMaxRadiusKm` >= 0),
          CONSTRAINT `chk_store_virtual_config_radius_order` CHECK (`AutomaticDeliveryRadiusKm` IS NULL OR `ScheduledDeliveryMaxRadiusKm` IS NULL OR `ScheduledDeliveryMaxRadiusKm` >= `AutomaticDeliveryRadiusKm`),
          CONSTRAINT `chk_store_virtual_config_preparation` CHECK (`PreparationTimeMinutes` >= 0),
          CONSTRAINT `chk_store_virtual_config_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla store_virtual_config creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla store_virtual_config ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_store_virtual_config`();
DROP PROCEDURE `p_manage_store_virtual_config`;
