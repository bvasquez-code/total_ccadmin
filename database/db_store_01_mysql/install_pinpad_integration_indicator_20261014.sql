-- MySQL 8. Indicador S/N del pinpad integrado. N: terminal separado, confirmacion manual.
-- Conserva el valor y estado existentes al ejecutar nuevamente.
DROP PROCEDURE IF EXISTS `p_install_pinpad_integration_indicator_20261014`;
DELIMITER $$
CREATE PROCEDURE `p_install_pinpad_integration_indicator_20261014`()
BEGIN
    DECLARE v_group_id INT;
    DECLARE v_next_corr INT;

    IF EXISTS (SELECT 1 FROM `business_config`
        WHERE `ConfigCod` = 'IND_PINPAD_INTEGRATED' AND `GroupCod` <> 'SystemFunctionalityActivator') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El indicador de pinpad ya pertenece a otro grupo';
    END IF;

    SELECT MAX(`GroupId`) INTO v_group_id FROM `business_config_group`
    WHERE `GroupCod` = 'SystemFunctionalityActivator';
    IF v_group_id IS NULL THEN
        SELECT COALESCE(MAX(`GroupId`), 0) + 1 INTO v_group_id FROM `business_config_group`;
        INSERT INTO `business_config_group` (
            `GroupId`, `GroupCod`, `GroupName`, `GroupDesc`, `ConfigCorrName`, `ConfigCorrKey`,
            `ConfigCodName`, `ConfigCodKey`, `ConfigValName`, `ConfigValKey`, `ConfigDescName`, `ConfigDescKey`,
            `CreationUser`, `CreationDate`, `Status`
        ) VALUES (
            v_group_id, 'SystemFunctionalityActivator', 'Indicadores del sistema', 'Activacion de funcionalidades del sistema',
            'Orden', 'configOrder', 'Indicador', 'indicator', 'Activado (S/N)', 'value', 'Descripcion', 'description',
            'SYSTEM', CURRENT_TIMESTAMP, 'A'
        );
    END IF;
    SELECT COALESCE(MAX(`ConfigCorr`), 0) + 1 INTO v_next_corr FROM `business_config`
    WHERE `GroupCod` = 'SystemFunctionalityActivator';
    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`, `ConfigName`, `ConfigDesc`,
        `CreationUser`, `CreationDate`, `Status`
    ) SELECT v_group_id, 'SystemFunctionalityActivator', v_next_corr, 'IND_PINPAD_INTEGRATED', 'N', 'Pinpad integrado',
        'El pinpad esta integrado al sistema? S: conecta y verifica el cobro. N: ingresar importe y medio en el terminal y confirmar manualmente.',
        'SYSTEM', CURRENT_TIMESTAMP, 'A'
    WHERE NOT EXISTS (SELECT 1 FROM `business_config` WHERE `ConfigCod` = 'IND_PINPAD_INTEGRATED');
END $$
DELIMITER ;
CALL `p_install_pinpad_integration_indicator_20261014`();
DROP PROCEDURE `p_install_pinpad_integration_indicator_20261014`;
