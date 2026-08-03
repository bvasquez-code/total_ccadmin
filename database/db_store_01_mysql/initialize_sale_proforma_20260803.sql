-- Ejecutar despues de business_config_group y business_config.
-- Registra el tipo interno 99 para proformas. El talonario y su serie se
-- configuran por local desde la pantalla de talonarios.

DROP PROCEDURE IF EXISTS `p_initialize_sale_proforma_20260803`;

DELIMITER $$

CREATE PROCEDURE `p_initialize_sale_proforma_20260803`()
BEGIN
    DECLARE v_group_exists INT DEFAULT 0;
    DECLARE v_conflicting_code INT DEFAULT 0;

    SELECT COUNT(*) INTO v_group_exists
    FROM `business_config_group`
    WHERE `GroupCod` = 'SalesDocumentType';

    IF v_group_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe el grupo SalesDocumentType';
    END IF;

    -- ConfigCod tiene una llave unica global en el modelo actual.
    SELECT COUNT(*) INTO v_conflicting_code
    FROM `business_config`
    WHERE `ConfigCod` = '99'
      AND `GroupCod` <> 'SalesDocumentType';

    IF v_conflicting_code > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ConfigCod 99 ya pertenece a otro grupo de configuracion';
    END IF;

    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`, `ConfigName`, `ConfigDesc`,
        `CreationUser`, `CreationDate`, `Status`
    )
    SELECT
        bcg.`GroupId`,
        bcg.`GroupCod`,
        COALESCE((
            SELECT MAX(existing.`ConfigCorr`) + 1
            FROM `business_config` existing
            WHERE existing.`GroupCod` = bcg.`GroupCod`
        ), 1),
        '99',
        'PROFORMA',
        'Proforma',
        'Documento interno de venta que no se envia a facturacion electronica',
        'SYSTEM',
        CURRENT_TIMESTAMP,
        'A'
    FROM `business_config_group` bcg
    WHERE bcg.`GroupCod` = 'SalesDocumentType'
      AND NOT EXISTS (
          SELECT 1
          FROM `business_config` existing
          WHERE existing.`GroupCod` = 'SalesDocumentType'
            AND existing.`ConfigCod` = '99'
      );

    UPDATE `business_config`
    SET `ConfigVal` = 'PROFORMA',
        `ConfigName` = 'Proforma',
        `ConfigDesc` = 'Documento interno de venta que no se envia a facturacion electronica',
        `Status` = 'A',
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = CURRENT_TIMESTAMP
    WHERE `GroupCod` = 'SalesDocumentType'
      AND `ConfigCod` = '99';
END $$

DELIMITER ;

CALL `p_initialize_sale_proforma_20260803`();
DROP PROCEDURE `p_initialize_sale_proforma_20260803`;
