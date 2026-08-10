DROP PROCEDURE IF EXISTS `p_manage_presale_channel`;

DELIMITER $$

CREATE PROCEDURE `p_manage_presale_channel`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'presale_channel';

    IF v_table_exists = 0 THEN
        CREATE TABLE `presale_channel` (
          `PresaleCod` varchar(16) NOT NULL COMMENT 'Codigo de la preventa relacionada',
          `ChannelCod` varchar(16) NOT NULL COMMENT 'Codigo del canal comercial que origino la preventa',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`PresaleCod`),
          KEY `idx_presale_channel_channel` (`ChannelCod`),
          CONSTRAINT `fk_presale_channel_presale` FOREIGN KEY (`PresaleCod`) REFERENCES `presale_head` (`PresaleCod`),
          CONSTRAINT `fk_presale_channel_channel` FOREIGN KEY (`ChannelCod`) REFERENCES `commercial_channel` (`ChannelCod`),
          CONSTRAINT `chk_presale_channel_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla presale_channel creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla presale_channel ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_presale_channel`();

INSERT INTO `presale_channel`
(`PresaleCod`, `ChannelCod`, `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`)
SELECT ph.`PresaleCod`, 'IN_PERSON', ph.`CreationUser`, ph.`CreationDate`, ph.`ModifyUser`, ph.`ModifyDate`, 'A'
FROM `presale_head` ph
WHERE NOT EXISTS (
    SELECT 1 FROM `presale_channel` pc WHERE pc.`PresaleCod` = ph.`PresaleCod`
);

DROP PROCEDURE `p_manage_presale_channel`;
