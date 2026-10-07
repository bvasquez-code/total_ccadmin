-- MySQL 8. Ejecutar despues de tables/table_trx_payments.sql y
-- tables/table_trx_payments_document.sql. Conserva URLs personalizadas, tokens y tiempos.
-- Las URLs son usadas por el NAVEGADOR en cada PC. Las claves no se almacenan aqui.
DROP PROCEDURE IF EXISTS `p_install_pinpad_service_urls_20261007`;
DELIMITER $$
CREATE PROCEDURE `p_install_pinpad_service_urls_20261007`()
BEGIN
    DECLARE v_group_id INT DEFAULT NULL;
    DECLARE v_next_corr INT DEFAULT 0;

    IF EXISTS (
        SELECT 1 FROM `business_config`
        WHERE `ConfigCod` IN ('PinpadLoginUrl', 'PinpadRegisterPaymentUrl', 'PinpadPaymentStatusUrl', 'PinpadPaymentAckUrl')
          AND `GroupCod` <> 'PinpadServiceUrl'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un codigo de URL pinpad ya pertenece a otro grupo';
    END IF;

    SELECT MAX(`GroupId`) INTO v_group_id FROM `business_config_group`
    WHERE `GroupCod` = 'PinpadServiceUrl';
    IF v_group_id IS NULL THEN
        SELECT COALESCE(MAX(`GroupId`), 0) + 1 INTO v_group_id FROM `business_config_group`;
        INSERT INTO `business_config_group` (
            `GroupId`, `GroupCod`, `GroupName`, `GroupDesc`,
            `ConfigCorrName`, `ConfigCorrKey`, `ConfigCodName`, `ConfigCodKey`,
            `ConfigValName`, `ConfigValKey`, `ConfigNameName`, `ConfigNameKey`,
            `ConfigDescName`, `ConfigDescKey`, `Str3ConfigName`, `Str3ConfigKey`,
            `Num1ConfigName`, `Num1ConfigKey`, `Num2ConfigName`, `Num2ConfigKey`,
            `CreationUser`, `CreationDate`, `Status`
        ) VALUES (
            v_group_id, 'PinpadServiceUrl', 'URLs del agente pinpad',
            'URLs locales usadas por el navegador en cada PC; tiempos en la URL de registro',
            'Orden', 'configOrder', 'Codigo de URL', 'urlCode',
            'URL del metodo', 'methodUrl', 'Nombre del metodo', 'methodName',
            'Descripcion', 'urlDescription', 'Reservado (no usar para claves)', 'reserved',
            'Espera del pago en segundos (solo registro)', 'waitSeconds',
            'Intervalo de consulta en ms (solo registro)', 'pollMillis',
            'SYSTEM', CURRENT_TIMESTAMP, 'A'
        );
    END IF;

    SELECT COALESCE(MAX(`ConfigCorr`), 0) INTO v_next_corr
    FROM `business_config` WHERE `GroupCod` = 'PinpadServiceUrl';

    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`, `ConfigName`, `ConfigDesc`,
        `Str3Config`, `Num1Config`, `Num2Config`, `CreationUser`, `CreationDate`, `Status`
    )
    SELECT v_group_id, 'PinpadServiceUrl', v_next_corr + 1, 'PinpadRegisterPaymentUrl',
        'http://127.0.0.1:8094/pinpad/payment/register', 'Iniciar o recuperar cobro local',
        'POST cobro con token local. Num1Config: espera 0..180 s; Num2Config: consulta 100..5000 ms',
        NULL, 130, 1000, 'SYSTEM', CURRENT_TIMESTAMP, 'A'
    WHERE NOT EXISTS (SELECT 1 FROM `business_config` WHERE `ConfigCod` = 'PinpadRegisterPaymentUrl');

    SELECT COALESCE(MAX(`ConfigCorr`), 0) INTO v_next_corr
    FROM `business_config` WHERE `GroupCod` = 'PinpadServiceUrl';
    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`, `ConfigName`, `ConfigDesc`,
        `CreationUser`, `CreationDate`, `Status`
    )
    SELECT v_group_id, 'PinpadServiceUrl', v_next_corr + 1, 'PinpadPaymentStatusUrl',
        'http://127.0.0.1:8094/pinpad/payment/status', 'Consultar cobro local',
        'POST consulta con token local; no crea un segundo cobro', 'SYSTEM', CURRENT_TIMESTAMP, 'A'
    WHERE NOT EXISTS (SELECT 1 FROM `business_config` WHERE `ConfigCod` = 'PinpadPaymentStatusUrl');

    SELECT COALESCE(MAX(`ConfigCorr`), 0) INTO v_next_corr
    FROM `business_config` WHERE `GroupCod` = 'PinpadServiceUrl';
    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`, `ConfigName`, `ConfigDesc`,
        `CreationUser`, `CreationDate`, `Status`
    )
    SELECT v_group_id, 'PinpadServiceUrl', v_next_corr + 1, 'PinpadPaymentAckUrl',
        'http://127.0.0.1:8094/pinpad/payment/ack', 'Confirmar guardado central',
        'POST confirmacion con token autorizado solamente despues del guardado central',
        'SYSTEM', CURRENT_TIMESTAMP, 'A'
    WHERE NOT EXISTS (SELECT 1 FROM `business_config` WHERE `ConfigCod` = 'PinpadPaymentAckUrl');

    SELECT COALESCE(MAX(`ConfigCorr`), 0) INTO v_next_corr
    FROM `business_config` WHERE `GroupCod` = 'PinpadServiceUrl';
    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`, `ConfigName`, `ConfigDesc`,
        `CreationUser`, `CreationDate`, `Status`
    )
    SELECT v_group_id, 'PinpadServiceUrl', v_next_corr + 1, 'PinpadLoginUrl',
        'http://127.0.0.1:8094/pinpad/login', 'Obtener token local',
        'POST login automatico; devuelve un token temporal para los servicios del pinpad',
        'SYSTEM', CURRENT_TIMESTAMP, 'A'
    WHERE NOT EXISTS (SELECT 1 FROM `business_config` WHERE `ConfigCod` = 'PinpadLoginUrl');
    -- Migra solamente los valores por defecto de las versiones anteriores.
    UPDATE `business_config`
    SET `ConfigVal` = CASE `ConfigCod`
            WHEN 'PinpadRegisterPaymentUrl' THEN 'http://127.0.0.1:8094/pinpad/payment/register'
            WHEN 'PinpadPaymentStatusUrl' THEN 'http://127.0.0.1:8094/pinpad/payment/status'
            WHEN 'PinpadPaymentAckUrl' THEN 'http://127.0.0.1:8094/pinpad/payment/ack'
        END,
        `ConfigDesc` = CASE `ConfigCod`
            WHEN 'PinpadRegisterPaymentUrl' THEN 'POST cobro con token local. Num1Config: espera 0..180 s; Num2Config: consulta 100..5000 ms'
            WHEN 'PinpadPaymentStatusUrl' THEN 'POST consulta con token local; no crea un segundo cobro'
            WHEN 'PinpadPaymentAckUrl' THEN 'POST confirmacion con token autorizado solamente despues del guardado central'
        END,
        `ModifyUser` = 'SYSTEM', `ModifyDate` = CURRENT_TIMESTAMP
    WHERE `GroupCod` = 'PinpadServiceUrl'
      AND `ConfigCod` IN ('PinpadRegisterPaymentUrl', 'PinpadPaymentStatusUrl', 'PinpadPaymentAckUrl')
      AND `ConfigVal` IN ('http://127.0.0.1:6669/payment-pinpad',
          'http://127.0.0.1:6669/payment-pinpad/{paymentId}/status',
          'http://127.0.0.1:6669/payment-pinpad/{paymentId}/ack',
          'http://127.0.0.1:6670/browser-pinpad',
          'http://127.0.0.1:8094/browser-pinpad');
END $$
DELIMITER ;
CALL `p_install_pinpad_service_urls_20261007`();
DROP PROCEDURE `p_install_pinpad_service_urls_20261007`;
