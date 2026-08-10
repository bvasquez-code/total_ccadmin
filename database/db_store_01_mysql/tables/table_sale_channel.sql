DROP PROCEDURE IF EXISTS `p_manage_sale_channel`;

DELIMITER $$

CREATE PROCEDURE `p_manage_sale_channel`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sale_channel';

    IF v_table_exists = 0 THEN
        CREATE TABLE `sale_channel` (
          `SaleCod` varchar(16) NOT NULL COMMENT 'Codigo de la venta relacionada',
          `ChannelCod` varchar(16) NOT NULL COMMENT 'Codigo del canal comercial que origino la venta',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`SaleCod`),
          KEY `idx_sale_channel_channel` (`ChannelCod`),
          CONSTRAINT `fk_sale_channel_sale` FOREIGN KEY (`SaleCod`) REFERENCES `sale_head` (`SaleCod`),
          CONSTRAINT `fk_sale_channel_channel` FOREIGN KEY (`ChannelCod`) REFERENCES `commercial_channel` (`ChannelCod`),
          CONSTRAINT `chk_sale_channel_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla sale_channel creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla sale_channel ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_sale_channel`();

INSERT INTO `sale_channel`
(`SaleCod`, `ChannelCod`, `CreationUser`, `CreationDate`, `ModifyUser`, `ModifyDate`, `Status`)
SELECT sh.`SaleCod`, 'IN_PERSON', sh.`CreationUser`, sh.`CreationDate`, sh.`ModifyUser`, sh.`ModifyDate`, 'A'
FROM `sale_head` sh
WHERE NOT EXISTS (
    SELECT 1 FROM `sale_channel` sc WHERE sc.`SaleCod` = sh.`SaleCod`
);

DROP PROCEDURE `p_manage_sale_channel`;
