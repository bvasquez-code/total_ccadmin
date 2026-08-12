DROP PROCEDURE IF EXISTS `p_manage_state`;

DELIMITER $$

CREATE PROCEDURE `p_manage_state`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'state';

    IF v_table_exists = 0 THEN
        CREATE TABLE `state` (
          `StateId` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador interno del estado, region o departamento',
          `CountryCod` varchar(3) NOT NULL COMMENT 'Codigo ISO3 del pais al que pertenece el estado, region o departamento',
          `StateName` varchar(150) NOT NULL COMMENT 'Nombre del estado, region o departamento',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Ultimo usuario que modifico el registro',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de ultima modificacion del registro',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro (A:Activo, I:Inactivo)',
          PRIMARY KEY (`StateId`),
          UNIQUE KEY `uk_state_country_name` (`CountryCod`,`StateName`),
          KEY `idx_state_country` (`CountryCod`),
          CONSTRAINT `fk_state_country` FOREIGN KEY (`CountryCod`) REFERENCES `country` (`CountryCod`),
          CONSTRAINT `chk_state_status` CHECK (`Status` IN ('A','I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Catalogo internacional de estados, regiones o departamentos por pais';

        SELECT 'Tabla state creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla state ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_state`();
DROP PROCEDURE `p_manage_state`;
