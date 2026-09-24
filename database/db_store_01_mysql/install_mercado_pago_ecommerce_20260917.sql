-- MySQL 8. Ejecutar tambien tables/table_mercado_pago_attempt.sql antes del backend.
-- No contiene credenciales reales. Reejecutar conserva valores y estado configurados.
DROP PROCEDURE IF EXISTS p_install_mercado_pago_ecommerce;
DELIMITER $$
CREATE PROCEDURE p_install_mercado_pago_ecommerce()
BEGIN
    DECLARE v_group_id INT;
    DECLARE v_next_corr INT;
    IF EXISTS (SELECT 1 FROM business_config WHERE ConfigCod = 'MercadoPagoCheckout'
               AND GroupCod <> 'MercadoPagoEcommerce') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'MercadoPagoCheckout ya pertenece a otro grupo';
    END IF;
    SELECT MAX(GroupId) INTO v_group_id FROM business_config_group WHERE GroupCod = 'MercadoPagoEcommerce';
    IF v_group_id IS NULL THEN
        SELECT COALESCE(MAX(GroupId), 0) + 1 INTO v_group_id FROM business_config_group;
        INSERT INTO business_config_group (
            GroupId, GroupCod, GroupIdName, GroupIdKey, GroupCodName, GroupCodKey,
            ConfigCorrName, ConfigCorrKey, ConfigCodName, ConfigCodKey,
            ConfigValName, ConfigValKey, ConfigNameName, ConfigNameKey, ConfigDescName, ConfigDescKey,
            Str1ConfigName, Str1ConfigKey, Str2ConfigName, Str2ConfigKey, Str3ConfigName, Str3ConfigKey,
            Num1ConfigName, Num1ConfigKey, Sta1ConfigName, Sta1ConfigKey, Sta2ConfigName, Sta2ConfigKey,
            CreationUserName, CreationUserKey, CreationDateName, CreationDateKey,
            ModifyUserName, ModifyUserKey, ModifyDateName, ModifyDateKey, StatusName, StatusKey,
            GroupName, GroupDesc, CreationUser, CreationDate, Status
        ) VALUES (
            v_group_id, 'MercadoPagoEcommerce', 'Identificador del grupo', 'groupId', 'Codigo del grupo', 'groupCode',
            'Orden', 'configOrder', 'Codigo', 'configCode',
            'Public Key', 'publicKey', 'Nombre', 'configName', 'Descripcion', 'configDescription',
            'Idioma', 'locale', 'Moneda ISO', 'currencyCode', 'Access Token (privado)', 'accessToken',
            'Maximo de cuotas', 'maxInstallments', 'Habilitado (S/N)', 'enabled', 'Pruebas (S/N)', 'testMode',
            'Usuario de creacion', 'creationUser', 'Fecha de creacion', 'creationDate',
            'Usuario de modificacion', 'modifyUser', 'Fecha de modificacion', 'modifyDate', 'Estado', 'status',
            'Mercado Pago - Ecommerce', 'Checkout de tarjetas TC001 y TD001 con credenciales privadas en backend',
            'SYSTEM', CURRENT_TIMESTAMP, 'A'
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM business_config WHERE ConfigCod = 'MercadoPagoCheckout') THEN
        SELECT COALESCE(MAX(ConfigCorr), 0) + 1 INTO v_next_corr FROM business_config WHERE GroupCod = 'MercadoPagoEcommerce';
        INSERT INTO business_config (
            GroupId, GroupCod, ConfigCorr, ConfigCod, ConfigVal, ConfigName, ConfigDesc,
            Str1Config, Str2Config, Str3Config, Num1Config, Sta1Config, Sta2Config,
            CreationUser, CreationDate, Status
        ) VALUES (
            v_group_id, 'MercadoPagoEcommerce', v_next_corr, 'MercadoPagoCheckout', '',
            'Checkout tarjetas Mercado Pago', 'Completar ConfigVal con Public Key y Str3Config con Access Token; activar Sta1Config=S despues de configurar.',
            'es-PE', 'PEN', '', 1, 'N', 'S', 'SYSTEM', CURRENT_TIMESTAMP, 'A'
        );
    END IF;
END$$
DELIMITER ;
CALL p_install_mercado_pago_ecommerce();
DROP PROCEDURE IF EXISTS p_install_mercado_pago_ecommerce;

-- Consulta sin exponer la credencial privada:
SELECT GroupCod, ConfigCod, ConfigVal AS PublicKey, Str1Config AS Locale,
       Str2Config AS CurrencyCod, Num1Config AS MaxInstallments,
       Sta1Config AS Enabled, Sta2Config AS TestMode
FROM business_config WHERE GroupCod = 'MercadoPagoEcommerce';
