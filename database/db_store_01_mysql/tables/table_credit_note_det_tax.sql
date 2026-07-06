DROP PROCEDURE IF EXISTS `p_manage_credit_note_det_tax`;

DELIMITER $$

CREATE PROCEDURE `p_manage_credit_note_det_tax`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'credit_note_det_tax';

    IF v_table_exists = 0 THEN
        CREATE TABLE `credit_note_det_tax` (
          `CreditNoteCod` varchar(16) NOT NULL COMMENT 'codigo de nota de credito',
          `ItemNumber` int NOT NULL COMMENT 'numero de item del detalle de nota de credito',
          `TaxLineNumber` int NOT NULL COMMENT 'numero de linea tributaria del detalle',
          `TaxCod` varchar(8) NOT NULL COMMENT 'codigo de tributo',
          `SunatTaxCod` varchar(8) DEFAULT NULL COMMENT 'codigo SUNAT del tributo',
          `TaxName` varchar(32) NOT NULL COMMENT 'nombre snapshot del tributo',
          `TaxAffectationCod` varchar(4) DEFAULT NULL COMMENT 'codigo de afectacion tributaria',
          `TaxAffectationName` varchar(64) DEFAULT NULL COMMENT 'nombre snapshot de afectacion',
          `TaxCalculationType` char(1) NOT NULL DEFAULT 'P' COMMENT 'P=porcentaje, F=monto fijo por unidad, N=no aplica',
          `IsInformative` char(1) NOT NULL DEFAULT 'N' COMMENT 'S=tributo informativo sin impuesto real',
          `TaxRateValue` decimal(16,4) DEFAULT NULL COMMENT 'tasa porcentual aplicada',
          `FixedUnitAmount` decimal(16,4) DEFAULT NULL COMMENT 'monto fijo por unidad aplicado',
          `TaxBaseAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'base tributaria de la linea',
          `TaxQuantity` decimal(16,4) NOT NULL DEFAULT '0.0000' COMMENT 'cantidad base para tributos por unidad',
          `TaxAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'monto de tributo calculado',
          `CalculationOrder` int NOT NULL DEFAULT '100' COMMENT 'orden de calculo',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`CreditNoteCod`,`ItemNumber`,`TaxLineNumber`),
          KEY `fk_credit_note_det_tax_tax` (`TaxCod`),
          KEY `fk_credit_note_det_tax_affectation` (`TaxAffectationCod`,`TaxCod`),
          CONSTRAINT `chk_credit_note_det_tax_amount` CHECK (`TaxAmount` >= 0),
          CONSTRAINT `chk_credit_note_det_tax_base` CHECK (`TaxBaseAmount` >= 0),
          CONSTRAINT `chk_credit_note_det_tax_calc_type` CHECK (`TaxCalculationType` in (_utf8mb4'P',_utf8mb4'F',_utf8mb4'N')),
          CONSTRAINT `chk_credit_note_det_tax_fixed_amount` CHECK (`FixedUnitAmount` IS NULL OR `FixedUnitAmount` >= 0),
          CONSTRAINT `chk_credit_note_det_tax_informative` CHECK (`IsInformative` in (_utf8mb4'S',_utf8mb4'N')),
          CONSTRAINT `chk_credit_note_det_tax_quantity` CHECK (`TaxQuantity` >= 0),
          CONSTRAINT `chk_credit_note_det_tax_rate` CHECK (`TaxRateValue` IS NULL OR `TaxRateValue` >= 0),
          CONSTRAINT `fk_credit_note_det_tax_affectation` FOREIGN KEY (`TaxAffectationCod`, `TaxCod`) REFERENCES `tax_affectation` (`TaxAffectationCod`, `TaxCod`),
          CONSTRAINT `fk_credit_note_det_tax_detail` FOREIGN KEY (`CreditNoteCod`, `ItemNumber`) REFERENCES `credit_note_det` (`CreditNoteCod`, `ItemNumber`),
          CONSTRAINT `fk_credit_note_det_tax_tax` FOREIGN KEY (`TaxCod`) REFERENCES `tax` (`TaxCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla credit_note_det_tax creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla credit_note_det_tax ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_credit_note_det_tax`();
DROP PROCEDURE `p_manage_credit_note_det_tax`;
