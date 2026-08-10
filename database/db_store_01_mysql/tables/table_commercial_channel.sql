DROP PROCEDURE IF EXISTS `p_manage_commercial_channel`;

DELIMITER $$

CREATE PROCEDURE `p_manage_commercial_channel`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'commercial_channel';

    IF v_table_exists = 0 THEN
        CREATE TABLE `commercial_channel` (
          `ChannelCod` varchar(16) NOT NULL COMMENT 'Codigo descriptivo del canal comercial',
          `Name` varchar(64) NOT NULL COMMENT 'Nombre visible del canal comercial',
          `Description` varchar(256) DEFAULT NULL COMMENT 'Descripcion funcional del canal comercial',
          `IsPublic` char(1) NOT NULL DEFAULT 'N' COMMENT 'Indica si el canal puede ser utilizado por aplicaciones publicas (S/N)',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`ChannelCod`),
          CONSTRAINT `chk_commercial_channel_public` CHECK (`IsPublic` IN ('S','N')),
          CONSTRAINT `chk_commercial_channel_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla commercial_channel creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla commercial_channel ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_commercial_channel`();

INSERT INTO `commercial_channel`
(`ChannelCod`, `Name`, `Description`, `IsPublic`, `CreationUser`, `Status`)
SELECT 'IN_PERSON', 'Venta presencial', 'Venta originada en el sistema interno y atendida presencialmente', 'N', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `commercial_channel` WHERE `ChannelCod` = 'IN_PERSON'
);

INSERT INTO `commercial_channel`
(`ChannelCod`, `Name`, `Description`, `IsPublic`, `CreationUser`, `Status`)
SELECT 'WEB', 'Tienda virtual', 'Venta originada desde la aplicacion publica de tienda virtual', 'S', 'SISTEMA', 'A'
WHERE NOT EXISTS (
    SELECT 1 FROM `commercial_channel` WHERE `ChannelCod` = 'WEB'
);

DROP PROCEDURE `p_manage_commercial_channel`;
