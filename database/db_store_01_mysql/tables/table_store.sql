DROP PROCEDURE IF EXISTS `p_manage_store`;

DELIMITER $$

CREATE PROCEDURE `p_manage_store`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'store';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `store` (
  `StoreCod` varchar(4) NOT NULL COMMENT 'codigo de tienda',
  `Name` varchar(32) NOT NULL COMMENT 'nombre de tienda',
  `Description` varchar(128) DEFAULT NULL COMMENT 'descripcion de tienda',
  `Address` varchar(128) DEFAULT NULL COMMENT 'direccion',
  `UbigeoCod` varchar(12) DEFAULT NULL COMMENT 'codigo de ubigeo',
  `SunatAddressTypeCode` varchar(4) NOT NULL DEFAULT '0000' COMMENT 'Codigo SUNAT de local anexo del emisor. 0000 corresponde al domicilio fiscal/principal.',
  `CreationUser` varchar(16) NOT NULL,
  `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ModifyUser` varchar(16) DEFAULT NULL,
  `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Status` char(1) NOT NULL DEFAULT 'A',
  `CompanyCod` varchar(4) DEFAULT NULL COMMENT 'CÃ³digo legible de la empresa (FK => company.CompanyCod).',
  PRIMARY KEY (`StoreCod`),
  KEY `idx_store_companycod` (`CompanyCod`),
  CONSTRAINT `fk_store_company` FOREIGN KEY (`CompanyCod`) REFERENCES `company` (`CompanyCod`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `store_sequence`
--

        SELECT 'Tabla store creada desde cero.' AS Mensaje;

    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'store'
              AND column_name = 'SunatAddressTypeCode'
        ) THEN
            ALTER TABLE `store`
                ADD COLUMN `SunatAddressTypeCode` varchar(4) NOT NULL DEFAULT '0000' COMMENT 'Codigo SUNAT de local anexo del emisor. 0000 corresponde al domicilio fiscal/principal.' AFTER `UbigeoCod`;
            SELECT 'Columna SunatAddressTypeCode agregada exitosamente.' AS Mensaje;
        END IF;
        
        SELECT 'Tabla store ya existe. Validacion de estructura completada.' AS Mensaje;

    END IF;

END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_store`();
DROP PROCEDURE `p_manage_store`;
