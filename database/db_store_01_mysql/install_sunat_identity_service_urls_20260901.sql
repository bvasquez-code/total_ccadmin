-- Configura las URLs consumidas por ws_wa_store_ccadmin para consultar
-- ws_wa_sunat_identity. Script idempotente para MySQL 8.

DROP PROCEDURE IF EXISTS `p_install_sunat_identity_service_urls_20260901`;

DELIMITER $$

CREATE PROCEDURE `p_install_sunat_identity_service_urls_20260901`()
BEGIN
    DECLARE v_group_id INT DEFAULT NULL;
    DECLARE v_conflicting_codes INT DEFAULT 0;

    SELECT MAX(`GroupId`) INTO v_group_id
    FROM `business_config_group`
    WHERE `GroupCod` = 'SunatIdentityServiceUrl';

    IF v_group_id IS NULL THEN
        SELECT COALESCE(MAX(`GroupId`), 0) + 1 INTO v_group_id
        FROM `business_config_group`;

        INSERT INTO `business_config_group` (
            `GroupId`, `GroupCod`,
            `GroupIdName`, `GroupIdKey`,
            `GroupCodName`, `GroupCodKey`,
            `ConfigCorrName`, `ConfigCorrKey`,
            `ConfigCodName`, `ConfigCodKey`,
            `ConfigValName`, `ConfigValKey`,
            `ConfigNameName`, `ConfigNameKey`,
            `ConfigDescName`, `ConfigDescKey`,
            `CreationUserName`, `CreationUserKey`,
            `CreationDateName`, `CreationDateKey`,
            `ModifyUserName`, `ModifyUserKey`,
            `ModifyDateName`, `ModifyDateKey`,
            `StatusName`, `StatusKey`,
            `GroupName`, `GroupDesc`,
            `CreationUser`, `CreationDate`, `Status`
        ) VALUES (
            v_group_id, 'SunatIdentityServiceUrl',
            'Identificador del grupo', 'groupId',
            'Codigo del grupo', 'groupCode',
            'Orden de la URL', 'configOrder',
            'Codigo de la URL', 'urlCode',
            'URL del metodo', 'methodUrl',
            'Nombre del metodo', 'methodName',
            'Descripcion de la URL', 'urlDescription',
            'Usuario de creacion', 'creationUser',
            'Fecha de creacion', 'creationDate',
            'Usuario de modificacion', 'modifyUser',
            'Fecha de modificacion', 'modifyDate',
            'Estado', 'status',
            'URLs del servicio de identidad SUNAT',
            'Endpoints externos usados por ws_wa_store_ccadmin para consultar identidades',
            'SYSTEM', CURRENT_TIMESTAMP, 'A'
        );
    ELSE
        UPDATE `business_config_group`
        SET `ConfigCodName` = 'Codigo de la URL',
            `ConfigCodKey` = 'urlCode',
            `ConfigValName` = 'URL del metodo',
            `ConfigValKey` = 'methodUrl',
            `ConfigNameName` = 'Nombre del metodo',
            `ConfigNameKey` = 'methodName',
            `ConfigDescName` = 'Descripcion de la URL',
            `ConfigDescKey` = 'urlDescription',
            `GroupName` = 'URLs del servicio de identidad SUNAT',
            `GroupDesc` = 'Endpoints externos usados por ws_wa_store_ccadmin para consultar identidades',
            `ModifyUser` = 'SYSTEM',
            `ModifyDate` = CURRENT_TIMESTAMP,
            `Status` = 'A'
        WHERE `GroupCod` = 'SunatIdentityServiceUrl';
    END IF;

    SELECT COUNT(*) INTO v_conflicting_codes
    FROM `business_config`
    WHERE `ConfigCod` IN (
        'SunatIdentityFindCompanyByRucUrl',
        'SunatIdentityFindPersonByDocumentUrl'
    )
      AND `GroupCod` <> 'SunatIdentityServiceUrl';

    IF v_conflicting_codes > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un codigo de URL SUNAT ya pertenece a otro grupo';
    END IF;

    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`,
        `ConfigName`, `ConfigDesc`, `CreationUser`, `CreationDate`, `Status`
    )
    SELECT
        v_group_id,
        'SunatIdentityServiceUrl',
        source.`NextCorr`,
        'SunatIdentityFindCompanyByRucUrl',
        'http://localhost:8093/api/v1/sunatIdentity/findCompanyByRuc',
        'Buscar empresa por RUC',
        'URL de ws_wa_sunat_identity para buscar una empresa por RUC',
        'SYSTEM', CURRENT_TIMESTAMP, 'A'
    FROM (
        SELECT COALESCE(MAX(`ConfigCorr`), 0) + 1 AS `NextCorr`
        FROM `business_config`
        WHERE `GroupCod` = 'SunatIdentityServiceUrl'
    ) source
    WHERE NOT EXISTS (
        SELECT 1
        FROM `business_config` configured
        WHERE configured.`GroupCod` = 'SunatIdentityServiceUrl'
          AND configured.`ConfigCod` = 'SunatIdentityFindCompanyByRucUrl'
    );

    INSERT INTO `business_config` (
        `GroupId`, `GroupCod`, `ConfigCorr`, `ConfigCod`, `ConfigVal`,
        `ConfigName`, `ConfigDesc`, `CreationUser`, `CreationDate`, `Status`
    )
    SELECT
        v_group_id,
        'SunatIdentityServiceUrl',
        source.`NextCorr`,
        'SunatIdentityFindPersonByDocumentUrl',
        'http://localhost:8093/api/v1/sunatIdentity/findPersonByDocument',
        'Buscar persona por documento',
        'URL de ws_wa_sunat_identity para buscar una persona por documento',
        'SYSTEM', CURRENT_TIMESTAMP, 'A'
    FROM (
        SELECT COALESCE(MAX(`ConfigCorr`), 0) + 1 AS `NextCorr`
        FROM `business_config`
        WHERE `GroupCod` = 'SunatIdentityServiceUrl'
    ) source
    WHERE NOT EXISTS (
        SELECT 1
        FROM `business_config` configured
        WHERE configured.`GroupCod` = 'SunatIdentityServiceUrl'
          AND configured.`ConfigCod` = 'SunatIdentityFindPersonByDocumentUrl'
    );

    UPDATE `business_config`
    SET `GroupId` = v_group_id,
        `ConfigVal` = COALESCE(
            NULLIF(TRIM(`ConfigVal`), ''),
            'http://localhost:8093/api/v1/sunatIdentity/findCompanyByRuc'
        ),
        `ConfigName` = 'Buscar empresa por RUC',
        `ConfigDesc` = 'URL de ws_wa_sunat_identity para buscar una empresa por RUC',
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = CURRENT_TIMESTAMP,
        `Status` = 'A'
    WHERE `GroupCod` = 'SunatIdentityServiceUrl'
      AND `ConfigCod` = 'SunatIdentityFindCompanyByRucUrl';

    UPDATE `business_config`
    SET `GroupId` = v_group_id,
        `ConfigVal` = COALESCE(
            NULLIF(TRIM(`ConfigVal`), ''),
            'http://localhost:8093/api/v1/sunatIdentity/findPersonByDocument'
        ),
        `ConfigName` = 'Buscar persona por documento',
        `ConfigDesc` = 'URL de ws_wa_sunat_identity para buscar una persona por documento',
        `ModifyUser` = 'SYSTEM',
        `ModifyDate` = CURRENT_TIMESTAMP,
        `Status` = 'A'
    WHERE `GroupCod` = 'SunatIdentityServiceUrl'
      AND `ConfigCod` = 'SunatIdentityFindPersonByDocumentUrl';
END $$

DELIMITER ;

CALL `p_install_sunat_identity_service_urls_20260901`();
DROP PROCEDURE `p_install_sunat_identity_service_urls_20260901`;
