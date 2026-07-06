DROP PROCEDURE IF EXISTS `p_manage_product_tax_config`;

DELIMITER $$

CREATE PROCEDURE `p_manage_product_tax_config`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'product_tax_config';

    IF v_table_exists = 0 THEN
        CREATE TABLE `product_tax_config` (
          `ProductTaxConfigId` bigint NOT NULL AUTO_INCREMENT COMMENT 'identificador de configuracion tributaria',
          `ProductCod` varchar(20) NOT NULL COMMENT 'codigo de producto',
          `StoreCod` varchar(4) NOT NULL COMMENT 'codigo de tienda',
          `TaxCod` varchar(8) NOT NULL COMMENT 'codigo de tributo',
          `TaxAffectationCod` varchar(4) DEFAULT NULL COMMENT 'codigo de afectacion tributaria principal',
          `IsMainTax` char(1) NOT NULL DEFAULT 'N' COMMENT 'S=configuracion tributaria principal',
          `TaxRateValue` decimal(16,4) DEFAULT NULL COMMENT 'tasa porcentual aplicada',
          `FixedUnitAmount` decimal(16,4) DEFAULT NULL COMMENT 'monto fijo por unidad aplicado',
          `TaxCalculationType` char(1) NOT NULL DEFAULT 'P' COMMENT 'P=porcentaje, F=monto fijo por unidad, N=no aplica',
          `IsInformative` char(1) NOT NULL DEFAULT 'N' COMMENT 'S=tributo informativo sin impuesto real',
          `CalculationOrder` int NOT NULL DEFAULT '100' COMMENT 'orden de calculo',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          `ActiveTaxUniqueKey` varchar(64) GENERATED ALWAYS AS (
              (CASE WHEN (`Status` = _utf8mb4'A') THEN concat(`ProductCod`,_utf8mb4'|',`StoreCod`,_utf8mb4'|',`TaxCod`) ELSE NULL END)
          ) STORED,
          `ActiveMainTaxUniqueKey` varchar(64) GENERATED ALWAYS AS (
              (CASE WHEN (`Status` = _utf8mb4'A' AND `IsMainTax` = _utf8mb4'S') THEN concat(`ProductCod`,_utf8mb4'|',`StoreCod`) ELSE NULL END)
          ) STORED,
          PRIMARY KEY (`ProductTaxConfigId`),
          UNIQUE KEY `uk_product_tax_config_active_tax` (`ActiveTaxUniqueKey`),
          UNIQUE KEY `uk_product_tax_config_active_main` (`ActiveMainTaxUniqueKey`),
          KEY `fk_product_tax_config_product_config` (`ProductCod`,`StoreCod`),
          KEY `fk_product_tax_config_tax` (`TaxCod`),
          KEY `fk_product_tax_config_affectation` (`TaxAffectationCod`,`TaxCod`),
          CONSTRAINT `chk_product_tax_config_calc_type` CHECK (`TaxCalculationType` in (_utf8mb4'P',_utf8mb4'F',_utf8mb4'N')),
          CONSTRAINT `chk_product_tax_config_fixed_amount` CHECK (`FixedUnitAmount` IS NULL OR `FixedUnitAmount` >= 0),
          CONSTRAINT `chk_product_tax_config_informative` CHECK (`IsInformative` in (_utf8mb4'S',_utf8mb4'N')),
          CONSTRAINT `chk_product_tax_config_main` CHECK (`IsMainTax` in (_utf8mb4'S',_utf8mb4'N')),
          CONSTRAINT `chk_product_tax_config_main_affectation` CHECK (`IsMainTax` = _utf8mb4'N' OR `TaxAffectationCod` IS NOT NULL),
          CONSTRAINT `chk_product_tax_config_percent_rate` CHECK (`TaxCalculationType` <> _utf8mb4'P' OR `IsInformative` = _utf8mb4'S' OR `TaxRateValue` IS NOT NULL),
          CONSTRAINT `chk_product_tax_config_rate` CHECK (`TaxRateValue` IS NULL OR `TaxRateValue` >= 0),
          CONSTRAINT `fk_product_tax_config_affectation` FOREIGN KEY (`TaxAffectationCod`, `TaxCod`) REFERENCES `tax_affectation` (`TaxAffectationCod`, `TaxCod`),
          CONSTRAINT `fk_product_tax_config_product_config` FOREIGN KEY (`ProductCod`, `StoreCod`) REFERENCES `product_config` (`ProductCod`, `StoreCod`),
          CONSTRAINT `fk_product_tax_config_tax` FOREIGN KEY (`TaxCod`) REFERENCES `tax` (`TaxCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla product_tax_config creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla product_tax_config ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_product_tax_config`();
DROP PROCEDURE `p_manage_product_tax_config`;
