DROP PROCEDURE IF EXISTS `p_manage_channel_delivery_type`;

DELIMITER $$

CREATE PROCEDURE `p_manage_channel_delivery_type`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'channel_delivery_type';

    IF v_table_exists = 0 THEN
        CREATE TABLE `channel_delivery_type` (
          `ChannelCod` varchar(16) NOT NULL COMMENT 'Codigo del canal comercial',
          `DeliveryTypeCod` varchar(24) NOT NULL COMMENT 'Codigo de la modalidad de entrega habilitada',
          `IsDefault` char(1) NOT NULL DEFAULT 'N' COMMENT 'Indica si es la modalidad predeterminada del canal (S/N)',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`ChannelCod`,`DeliveryTypeCod`),
          KEY `idx_channel_delivery_type_delivery` (`DeliveryTypeCod`),
          CONSTRAINT `fk_channel_delivery_type_channel` FOREIGN KEY (`ChannelCod`) REFERENCES `commercial_channel` (`ChannelCod`),
          CONSTRAINT `fk_channel_delivery_type_delivery` FOREIGN KEY (`DeliveryTypeCod`) REFERENCES `delivery_type` (`DeliveryTypeCod`),
          CONSTRAINT `chk_channel_delivery_type_default` CHECK (`IsDefault` IN ('S','N')),
          CONSTRAINT `chk_channel_delivery_type_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla channel_delivery_type creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla channel_delivery_type ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_channel_delivery_type`();

INSERT INTO `channel_delivery_type`
(`ChannelCod`, `DeliveryTypeCod`, `IsDefault`, `CreationUser`, `Status`)
SELECT 'IN_PERSON', 'IN_STORE', 'S', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `channel_delivery_type`
    WHERE `ChannelCod` = 'IN_PERSON' AND `DeliveryTypeCod` = 'IN_STORE'
);

INSERT INTO `channel_delivery_type`
(`ChannelCod`, `DeliveryTypeCod`, `IsDefault`, `CreationUser`, `Status`)
SELECT 'WEB', 'DELIVERY', 'S', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `channel_delivery_type`
    WHERE `ChannelCod` = 'WEB' AND `DeliveryTypeCod` = 'DELIVERY'
);

INSERT INTO `channel_delivery_type`
(`ChannelCod`, `DeliveryTypeCod`, `IsDefault`, `CreationUser`, `Status`)
SELECT 'WEB', 'SCHEDULED_DELIVERY', 'N', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `channel_delivery_type`
    WHERE `ChannelCod` = 'WEB' AND `DeliveryTypeCod` = 'SCHEDULED_DELIVERY'
);

INSERT INTO `channel_delivery_type`
(`ChannelCod`, `DeliveryTypeCod`, `IsDefault`, `CreationUser`, `Status`)
SELECT 'WEB', 'STORE_PICKUP', 'N', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `channel_delivery_type`
    WHERE `ChannelCod` = 'WEB' AND `DeliveryTypeCod` = 'STORE_PICKUP'
);

DROP PROCEDURE `p_manage_channel_delivery_type`;
