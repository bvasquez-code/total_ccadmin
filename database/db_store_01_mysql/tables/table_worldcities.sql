-- Catalogo geografico estatico importado: conserva el contrato original,
-- sin auditoria, estado, PK, FK ni indices adicionales.
DROP PROCEDURE IF EXISTS `p_manage_worldcities`;

DELIMITER $$

CREATE PROCEDURE `p_manage_worldcities`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'worldcities';

    IF v_table_exists = 0 THEN
        CREATE TABLE `worldcities` (
          `city` varchar(150) DEFAULT NULL COMMENT 'Nombre de la ciudad en el catalogo de origen',
          `city_ascii` varchar(150) DEFAULT NULL COMMENT 'Nombre de la ciudad normalizado en caracteres ASCII',
          `lat` double DEFAULT NULL COMMENT 'Latitud geografica en grados decimales',
          `lng` double DEFAULT NULL COMMENT 'Longitud geografica en grados decimales',
          `country` varchar(150) DEFAULT NULL COMMENT 'Nombre del pais de la ciudad',
          `iso2` varchar(150) DEFAULT NULL COMMENT 'Codigo de pais alfa-2 del catalogo de origen',
          `iso3` varchar(150) DEFAULT NULL COMMENT 'Codigo de pais alfa-3 del catalogo de origen',
          `admin_name` varchar(150) DEFAULT NULL COMMENT 'Nombre de la division administrativa de la ciudad',
          `capital` varchar(150) DEFAULT NULL COMMENT 'Clasificacion de capital del catalogo de origen',
          `population` int DEFAULT NULL COMMENT 'Poblacion referencial expresada en habitantes',
          `id` int DEFAULT NULL COMMENT 'Identificador de la ciudad en el catalogo de origen'
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Catalogo geografico estatico de ciudades del mundo; se conserva durante la limpieza';

        SELECT 'Tabla worldcities creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla worldcities ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_worldcities`();
DROP PROCEDURE `p_manage_worldcities`;
