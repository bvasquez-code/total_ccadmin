DROP PROCEDURE IF EXISTS `p_manage_tax_affectation`;

DELIMITER $$

CREATE PROCEDURE `p_manage_tax_affectation`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'tax_affectation';

    IF v_table_exists = 0 THEN
        CREATE TABLE `tax_affectation` (
          `TaxAffectationCod` varchar(4) NOT NULL COMMENT 'codigo de afectacion tributaria',
          `TaxCod` varchar(8) NOT NULL COMMENT 'codigo de tributo permitido para la afectacion',
          `Name` varchar(64) NOT NULL COMMENT 'nombre de afectacion',
          `Description` varchar(128) NOT NULL COMMENT 'descripcion de afectacion',
          `IsTaxed` char(1) NOT NULL DEFAULT 'N' COMMENT 'S=operacion gravada con impuesto real',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A',
          PRIMARY KEY (`TaxAffectationCod`),
          UNIQUE KEY `uk_tax_affectation_tax` (`TaxAffectationCod`,`TaxCod`),
          KEY `fk_tax_affectation_tax` (`TaxCod`),
          CONSTRAINT `chk_tax_affectation_istaxed` CHECK (`IsTaxed` in (_utf8mb4'S',_utf8mb4'N')),
          CONSTRAINT `fk_tax_affectation_tax` FOREIGN KEY (`TaxCod`) REFERENCES `tax` (`TaxCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla tax_affectation creada desde cero.' AS Mensaje;
    ELSE
        IF NOT EXISTS (
            SELECT * FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'tax_affectation'
            AND index_name = 'uk_tax_affectation_tax'
        ) THEN
            ALTER TABLE `tax_affectation` ADD UNIQUE KEY `uk_tax_affectation_tax` (`TaxAffectationCod`,`TaxCod`);
            SELECT 'Indice uk_tax_affectation_tax agregado exitosamente.' AS Mensaje;
        END IF;

        IF NOT EXISTS (
            SELECT * FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'tax_affectation'
            AND constraint_name = 'chk_tax_affectation_istaxed'
        ) THEN
            ALTER TABLE `tax_affectation` ADD CONSTRAINT `chk_tax_affectation_istaxed`
            CHECK (`IsTaxed` in (_utf8mb4'S',_utf8mb4'N'));
            SELECT 'Check chk_tax_affectation_istaxed agregado exitosamente.' AS Mensaje;
        END IF;

        SELECT 'Tabla tax_affectation ya existe. Validacion de estructura completada.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_tax_affectation`();
DROP PROCEDURE `p_manage_tax_affectation`;
