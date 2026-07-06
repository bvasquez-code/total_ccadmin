DROP PROCEDURE IF EXISTS `p_manage_tax`;

DELIMITER $$

CREATE PROCEDURE `p_manage_tax`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    -- 1. Verificamos si la tabla existe
    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables 
    WHERE table_schema = DATABASE() 
    AND table_name = 'tax';

    IF v_table_exists = 0 THEN
        -- =============================================
        -- CASO: LA TABLA NO EXISTE -> CREARLA COMPLETA
        -- =============================================
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tax` (
  `TaxCod` varchar(8) NOT NULL COMMENT 'codigo de impuesto',
  `SunatTaxCod` varchar(8) DEFAULT NULL COMMENT 'codigo SUNAT del tributo',
  `TaxRateValue` decimal(16,4) NOT NULL COMMENT 'valor porcentual sobre 100 del impuesto',
  `FixedUnitAmount` decimal(16,4) NOT NULL DEFAULT '0.0000' COMMENT 'monto fijo por unidad cuando corresponde',
  `TaxCalculationType` char(1) NOT NULL DEFAULT 'P' COMMENT 'P=porcentaje, F=monto fijo por unidad, N=no aplica',
  `IsInformative` char(1) NOT NULL DEFAULT 'N' COMMENT 'S=tributo informativo sin impuesto real',
  `CalculationOrder` int NOT NULL DEFAULT '100' COMMENT 'orden de calculo del tributo',
  `Name` varchar(32) NOT NULL COMMENT 'nombre del impuesto',
  `Description` varchar(64) NOT NULL COMMENT 'descripcion del impuesto',
  `CreationUser` varchar(16) NOT NULL,
  `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ModifyUser` varchar(16) DEFAULT NULL,
  `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `Status` char(1) NOT NULL DEFAULT 'A',
  PRIMARY KEY (`TaxCod`),
  CONSTRAINT `chk_tax_calculation_type` CHECK (`TaxCalculationType` in (_utf8mb4'P',_utf8mb4'F',_utf8mb4'N')),
  CONSTRAINT `chk_tax_fixed_unit_amount` CHECK (`FixedUnitAmount` >= 0),
  CONSTRAINT `chk_tax_informative` CHECK (`IsInformative` in (_utf8mb4'S',_utf8mb4'N')),
  CONSTRAINT `chk_tax_rate_value` CHECK (`TaxRateValue` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trx_payments`
--

        SELECT 'Tabla tax creada desde cero.' AS Mensaje;

    ELSE
        -- =============================================
        -- CASO: LA TABLA YA EXISTE -> APLICAR ALTERS
        -- =============================================
        
        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND column_name = 'SunatTaxCod'
        ) THEN
            ALTER TABLE `tax` ADD COLUMN `SunatTaxCod` varchar(8) DEFAULT NULL COMMENT 'codigo SUNAT del tributo' AFTER `TaxCod`;
            SELECT 'Columna SunatTaxCod agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND column_name = 'FixedUnitAmount'
        ) THEN
            ALTER TABLE `tax` ADD COLUMN `FixedUnitAmount` decimal(16,4) NOT NULL DEFAULT '0.0000' COMMENT 'monto fijo por unidad cuando corresponde' AFTER `TaxRateValue`;
            SELECT 'Columna FixedUnitAmount agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND column_name = 'TaxCalculationType'
        ) THEN
            ALTER TABLE `tax` ADD COLUMN `TaxCalculationType` char(1) NOT NULL DEFAULT 'P' COMMENT 'P=porcentaje, F=monto fijo por unidad, N=no aplica' AFTER `FixedUnitAmount`;
            SELECT 'Columna TaxCalculationType agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND column_name = 'IsInformative'
        ) THEN
            ALTER TABLE `tax` ADD COLUMN `IsInformative` char(1) NOT NULL DEFAULT 'N' COMMENT 'S=tributo informativo sin impuesto real' AFTER `TaxCalculationType`;
            SELECT 'Columna IsInformative agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND column_name = 'CalculationOrder'
        ) THEN
            ALTER TABLE `tax` ADD COLUMN `CalculationOrder` int NOT NULL DEFAULT '100' COMMENT 'orden de calculo del tributo' AFTER `IsInformative`;
            SELECT 'Columna CalculationOrder agregada exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND constraint_name = 'chk_tax_calculation_type'
        ) THEN
            ALTER TABLE `tax` ADD CONSTRAINT `chk_tax_calculation_type`
            CHECK (`TaxCalculationType` in (_utf8mb4'P',_utf8mb4'F',_utf8mb4'N'));
            SELECT 'Check chk_tax_calculation_type agregado exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND constraint_name = 'chk_tax_fixed_unit_amount'
        ) THEN
            ALTER TABLE `tax` ADD CONSTRAINT `chk_tax_fixed_unit_amount`
            CHECK (`FixedUnitAmount` >= 0);
            SELECT 'Check chk_tax_fixed_unit_amount agregado exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND constraint_name = 'chk_tax_informative'
        ) THEN
            ALTER TABLE `tax` ADD CONSTRAINT `chk_tax_informative`
            CHECK (`IsInformative` in (_utf8mb4'S',_utf8mb4'N'));
            SELECT 'Check chk_tax_informative agregado exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'tax'
            AND constraint_name = 'chk_tax_rate_value'
        ) THEN
            ALTER TABLE `tax` ADD CONSTRAINT `chk_tax_rate_value`
            CHECK (`TaxRateValue` >= 0);
            SELECT 'Check chk_tax_rate_value agregado exitosamente.' AS Mensaje;
        END IF;
        
        SELECT 'Tabla tax ya existe. Validacion de estructura completada.' AS Mensaje;

    END IF;

END $$

DELIMITER ;

-- Ejecutar y limpiar
CALL `p_manage_tax`();
DROP PROCEDURE `p_manage_tax`;
