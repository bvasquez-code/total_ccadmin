-- MySQL 8. Ejecutar COMPLETO, con db_store_lts seleccionada, sin --force.
-- Requiere una copia completa y aplicaciones/workers/eventos detenidos.
-- TRUNCATE realiza commits implicitos: ante un error, restaurar la copia.
-- Nunca ejecutar sobre la base de trabajo. No cambia tablas ni rutinas de ella.
-- Las rutinas auxiliares se crean exclusivamente en db_store_lts.
-- TRUNCATE para tablas vacias; DELETE selectivo para las excepciones protegidas.
-- payment_method intacta; business_config solo pierde el token privado de Mercado Pago.
-- Conserva D0000001 y sus definiciones T001; reconstruye stock cero e instala cinco talonarios.
-- Conserva tambien la marca GENERICO y la categoria padre GENERICO con sus descendientes.
-- Conserva los catalogos geograficos, incluido worldcities si esta instalado.
SET @InitializationSelectedDatabase = DATABASE();

DROP PROCEDURE IF EXISTS `db_store_lts`.`p_initialize_clean_store`;
DELIMITER $$
CREATE PROCEDURE `db_store_lts`.`p_initialize_clean_store`()
BEGIN
    DECLARE v_PreviousForeignKeyChecks int DEFAULT 1;
    DECLARE v_PreviousGroupConcatMaxLen bigint DEFAULT 1024;
    DECLARE v_PreparedStatementActive boolean DEFAULT FALSE;
    DECLARE v_TableName varchar(64);
    DECLARE v_Message varchar(128);
    DECLARE v_TableCount int DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        IF v_PreparedStatementActive THEN
            DEALLOCATE PREPARE InitializationStatement;
        END IF;
        SET SESSION FOREIGN_KEY_CHECKS = v_PreviousForeignKeyChecks;
        SET SESSION group_concat_max_len = v_PreviousGroupConcatMaxLen;
        RESIGNAL;
    END;

    SET v_PreviousForeignKeyChecks = @@SESSION.FOREIGN_KEY_CHECKS;
    SET v_PreviousGroupConcatMaxLen = @@SESSION.group_concat_max_len;
    IF COALESCE(@InitializationSelectedDatabase, '') <> 'db_store_lts'
        OR DATABASE() <> 'db_store_lts' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Seleccione db_store_lts: limpieza cancelada antes de modificar datos';
    END IF;
    IF @@SESSION.autocommit <> 1 OR v_PreviousForeignKeyChecks <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Use una conexion nueva con autocommit=1 y FOREIGN_KEY_CHECKS=1';
    END IF;

    -- Tablas temporales de trabajo: no se exportan ni requieren auditoria.
    DROP TEMPORARY TABLE IF EXISTS `_initialization_table_plan`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_users`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_people`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_user_profiles`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_store_sequences`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_company`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_store`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_period`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_table_counts`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_fk_checks`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_protected_categories`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_counterfoil`;
    DROP TEMPORARY TABLE IF EXISTS `_initialization_counterfoil_store`;

    CREATE TEMPORARY TABLE `_initialization_table_plan` (
        `TableName` varchar(64) NOT NULL COMMENT 'Tabla clasificada para la inicializacion',
        `TableAction` char(1) NOT NULL COMMENT 'C: vaciar; K: conservar; R: reinicializar; S: eliminar selectivamente',
        `KeepCondition` text DEFAULT NULL COMMENT 'Predicado de filas protegidas para la accion S',
        PRIMARY KEY (`TableName`)
    );
    INSERT INTO `_initialization_table_plan` (`TableName`, `TableAction`) VALUES
        ('app_file', 'S'),
        ('app_menu', 'K'),
        ('app_profile', 'K'),
        ('app_session', 'C'),
        ('app_session_history', 'C'),
        ('app_user', 'R'),
        ('brand', 'S'),
        ('bulk_load_destination', 'C'),
        ('bulk_load_det', 'C'),
        ('bulk_load_head', 'C'),
        ('business_config', 'K'),
        ('business_config_group', 'K'),
        ('business_config_table', 'K'),
        ('carrier', 'C'),
        ('cash_register', 'R'),
        ('cash_session', 'C'),
        ('cash_session_item', 'C'),
        ('category', 'S'),
        ('channel_delivery_type', 'K'),
        ('city', 'K'),
        ('client', 'C'),
        ('client_account', 'C'),
        ('client_address', 'C'),
        ('commercial_channel', 'K'),
        ('company', 'R'),
        ('counterfoil', 'R'),
        ('counterfoil_store', 'R'),
        ('country', 'K'),
        ('credit_note_application', 'C'),
        ('credit_note_det', 'C'),
        ('credit_note_det_tax', 'C'),
        ('credit_note_det_warehouse', 'C'),
        ('credit_note_document', 'C'),
        ('credit_note_head', 'C'),
        ('currency', 'K'),
        ('delivery_type', 'K'),
        ('error_store', 'C'),
        ('kardex', 'C'),
        ('kardex_zone', 'C'),
        ('mercado_pago_attempt', 'C'),
        ('payment_method', 'K'),
        ('period', 'R'),
        ('person', 'R'),
        ('presale_channel', 'C'),
        ('presale_det', 'C'),
        ('presale_det_warehouse', 'C'),
        ('presale_head', 'C'),
        ('product', 'S'),
        ('product_barcode', 'S'),
        ('product_config', 'S'),
        ('product_info', 'R'),
        ('product_info_warehouse', 'R'),
        ('product_picture', 'S'),
        ('product_price_history', 'C'),
        ('product_ranking', 'C'),
        ('product_search', 'R'),
        ('product_tax_config', 'S'),
        ('product_traceability', 'C'),
        ('product_variant', 'S'),
        ('profile_menu', 'K'),
        ('promotion', 'C'),
        ('promotion_coupon', 'C'),
        ('promotion_product', 'C'),
        ('promotion_store', 'C'),
        ('pucharse_det', 'C'),
        ('pucharse_det_delivery', 'C'),
        ('pucharse_head', 'C'),
        ('pucharse_request_det', 'C'),
        ('pucharse_request_head', 'C'),
        ('sale_applied_tax', 'C'),
        ('sale_billing', 'C'),
        ('sale_channel', 'C'),
        ('sale_delivery', 'C'),
        ('sale_det', 'C'),
        ('sale_det_tax', 'C'),
        ('sale_det_warehouse', 'C'),
        ('sale_document', 'C'),
        ('sale_head', 'C'),
        ('sale_payments', 'C'),
        ('shipping_provider', 'C'),
        ('state', 'K'),
        ('stock_entry_det', 'C'),
        ('stock_entry_head', 'C'),
        ('stock_exit_det', 'C'),
        ('stock_exit_head', 'C'),
        ('store', 'R'),
        ('store_sequence', 'R'),
        ('store_virtual_config', 'C'),
        ('sunat_config', 'C'),
        ('sunat_document', 'C'),
        ('sunat_document_attempt', 'C'),
        ('sunat_document_file', 'C'),
        ('sunat_document_payload', 'C'),
        ('sunat_submission', 'C'),
        ('supplier', 'C'),
        ('system_document', 'C'),
        ('table_sequence', 'R'),
        ('tax', 'K'),
        ('tax_affectation', 'K'),
        ('transfer_det', 'C'),
        ('transfer_document', 'C'),
        ('transfer_head', 'C'),
        ('transfer_request_det', 'C'),
        ('transfer_request_head', 'C'),
        ('trx_payments', 'C'),
        ('trx_payments_document', 'C'),
        ('ubigeo_department', 'K'),
        ('ubigeo_district', 'K'),
        ('ubigeo_province', 'K'),
        ('user_profile', 'R'),
        ('user_store', 'R'),
        ('virtual_cart', 'C'),
        ('warehouse', 'R'),
        ('worldcities', 'K');

    -- DELETE en tablas con excepciones: las filas protegidas no se reinsertan ni modifican.
    UPDATE `_initialization_table_plan` SET KeepCondition = 'ProductCod = ''D0000001'''
    WHERE TableName IN ('product', 'product_variant', 'product_barcode', 'product_picture');
    UPDATE `_initialization_table_plan`
    SET KeepCondition = 'ProductCod = ''D0000001'' AND StoreCod = ''T001'''
    WHERE TableName IN ('product_config', 'product_tax_config');
    UPDATE `_initialization_table_plan`
    SET KeepCondition = 'UPPER(TRIM(brand.BrandName)) = ''GENERICO'' OR EXISTS (SELECT 1 FROM product p WHERE p.ProductCod = ''D0000001'' AND p.BrandCod = brand.BrandCod)'
    WHERE TableName = 'brand';
    UPDATE `_initialization_table_plan`
    SET KeepCondition = 'EXISTS (SELECT 1 FROM _initialization_protected_categories c WHERE c.CategoryCod = category.CategoryCod)'
    WHERE TableName = 'category';
    UPDATE `_initialization_table_plan`
    SET KeepCondition = 'EXISTS (SELECT 1 FROM payment_method pm WHERE pm.FileCod = app_file.FileCod OR (pm.Route IS NOT NULL AND pm.Route <> '''' AND pm.Route = app_file.Route)) OR EXISTS (SELECT 1 FROM product_picture pp WHERE pp.ProductCod = ''D0000001'' AND pp.FileCod = app_file.FileCod)'
    WHERE TableName = 'app_file';

    -- Una tabla nueva debe clasificarse expresamente; no dejar datos sin limpiar.
    SELECT MIN(t.table_name) INTO v_TableName
    FROM information_schema.tables t
    LEFT JOIN `_initialization_table_plan` p ON p.TableName = t.table_name
    WHERE t.table_schema = DATABASE() AND t.table_type = 'BASE TABLE'
      AND p.TableName IS NULL;
    IF v_TableName IS NOT NULL THEN
        SET v_Message = CONCAT('Tabla sin clasificar: ', v_TableName, '; no se ha limpiado ningun dato');
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
    END IF;

    -- worldcities es un catalogo estatico opcional; si existe, se conserva integro.
    -- Las tablas C opcionales pueden faltar; no las que usa la automatizacion de tienda.
    SELECT MIN(p.TableName) INTO v_TableName
    FROM `_initialization_table_plan` p
    LEFT JOIN information_schema.tables t
      ON t.table_schema = DATABASE() AND t.table_name = p.TableName
      AND t.table_type = 'BASE TABLE'
    WHERE (p.TableAction = 'R'
        OR (p.TableAction = 'K' AND p.TableName <> 'worldcities') OR p.TableName IN (
        'product', 'product_variant', 'product_info', 'product_info_warehouse',
        'product_config', 'product_search', 'product_picture', 'brand', 'category', 'app_file'
    )) AND t.table_name IS NULL;
    IF v_TableName IS NOT NULL THEN
        SET v_Message = CONCAT('Falta tabla base requerida: ', v_TableName);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage
               WHERE referenced_table_name IS NOT NULL
                 AND ((table_schema = DATABASE() AND referenced_table_schema <> DATABASE())
                   OR (table_schema <> DATABASE() AND referenced_table_schema = DATABASE()))) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Hay FK entre esquemas: aísle completamente la copia antes de limpiar';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.events
               WHERE event_schema = DATABASE() AND status = 'ENABLED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Deshabilite los eventos de db_store_lts antes de limpiar';
    END IF;
    SELECT COUNT(*) INTO v_TableCount FROM information_schema.triggers
    WHERE trigger_schema = DATABASE()
      AND trigger_name IN ('trg_after_insert_store', 'trg_after_insert_user_store_cash_register');
    IF v_TableCount <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Faltan triggers de inicializacion de tienda/caja; importe el respaldo completo';
    END IF;
    SELECT COUNT(*) INTO v_TableCount FROM information_schema.routines
    WHERE routine_schema = DATABASE() AND routine_type = 'PROCEDURE'
      AND routine_name IN ('get_cod_seq', 'get_cod_trx', 'get_cod_seq_sin_commit',
                           'sp_initalize_store_automation', 'sp_initialize_user_store_automation');
    IF v_TableCount <> 5 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Faltan procedimientos de negocio; importe el respaldo completo';
    END IF;

    IF (SELECT COUNT(*) FROM app_user WHERE UserCod IN ('ROOT', 'USER_WEB')) <> 2
       OR (SELECT COUNT(*) FROM app_user u JOIN person p ON p.PersonCod = u.PersonCod
           WHERE u.UserCod IN ('ROOT', 'USER_WEB') AND u.Status = 'A' AND p.Status = 'A') <> 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Se requieren ROOT y USER_WEB activos con sus personas; no se inventan contrasenas';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM user_profile up
                   JOIN app_profile ap ON ap.ProfileCod = up.ProfileCod AND ap.Status = 'A'
                   JOIN profile_menu pm ON pm.ProfileCod = ap.ProfileCod AND pm.Status = 'A'
                   JOIN app_menu am ON am.MenuCod = pm.MenuCod AND am.Status = 'A'
                   WHERE up.UserCod = 'ROOT' AND up.Status = 'A') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ROOT no tiene un perfil activo con menus; corrija la copia antes de limpiar';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM country WHERE CountryCod = 'PER' AND Status = 'A') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Falta el pais PER activo en el catalogo';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM table_sequence WHERE SequenceTableType = 'cash_register') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Falta la secuencia global cash_register';
    END IF;

    -- No inventar ni alterar el producto delivery: exigir sus datos originales antes de limpiar.
    IF NOT EXISTS (SELECT 1 FROM product WHERE ProductCod = 'D0000001' AND Status = 'A')
       OR NOT EXISTS (SELECT 1 FROM product_variant WHERE ProductCod = 'D0000001' AND Status = 'A')
       OR NOT EXISTS (SELECT 1 FROM product_config
                      WHERE ProductCod = 'D0000001' AND StoreCod = 'T001' AND Status = 'A') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Falta delivery D0000001 activo con variantes y configuracion T001; no se ha limpiado';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM product p JOIN brand b ON b.BrandCod = p.BrandCod
                   JOIN category c ON c.CategoryCod = p.CategoryCod
                   WHERE p.ProductCod = 'D0000001' AND b.Status = 'A' AND c.Status = 'A')
       OR (SELECT COUNT(*) FROM currency WHERE IsMonedaSystem = 'S') <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Delivery requiere su marca/categoria activas y una unica moneda del sistema';
    END IF;
    IF EXISTS (SELECT 1 FROM payment_method pm LEFT JOIN app_file f ON f.FileCod = pm.FileCod
               WHERE pm.FileCod IS NOT NULL AND pm.FileCod <> '' AND f.FileCod IS NULL) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Un medio de pago referencia un archivo inexistente; restaure sus imagenes en la copia';
    END IF;
    CREATE TEMPORARY TABLE `_initialization_protected_categories` (
        `CategoryCod` varchar(10) NOT NULL COMMENT 'Categorias GENERICO y descendientes, delivery y ancestros necesarios',
        PRIMARY KEY (`CategoryCod`)
    );
    INSERT INTO `_initialization_protected_categories`
    WITH RECURSIVE generic_categories AS (
        SELECT CategoryCod, CategoryDadCod FROM category
        WHERE UPPER(TRIM(CategoryName)) = 'GENERICO'
          AND (IsCategoryDad = 'S' OR CategoryDadCod IS NULL OR CategoryDadCod = '')
        UNION DISTINCT
        SELECT child.CategoryCod, child.CategoryDadCod FROM category child
        JOIN generic_categories parent ON child.CategoryDadCod = parent.CategoryCod
    ), protected_categories AS (
        SELECT c.CategoryCod, c.CategoryDadCod FROM category c
        JOIN product p ON p.CategoryCod = c.CategoryCod WHERE p.ProductCod = 'D0000001'
        UNION DISTINCT
        SELECT CategoryCod, CategoryDadCod FROM generic_categories
        UNION DISTINCT
        SELECT parent.CategoryCod, parent.CategoryDadCod FROM category parent
        JOIN protected_categories child ON parent.CategoryCod = child.CategoryDadCod
    ) SELECT CategoryCod FROM protected_categories;

    -- Antes de borrar, comprobar que los correlativos globales tienen destinos conocidos.
    -- El limite de 18 digitos corresponde al numero, sin contar el prefijo.
    -- LT/ES con longitud total 20 son formatos validos: prefijo de 2 + 18 digitos.
    SELECT MIN(s.SequenceTableType) INTO v_TableName
    FROM table_sequence s
    LEFT JOIN `_initialization_table_plan` p ON p.TableName = s.SequenceTableType
    LEFT JOIN information_schema.tables t
      ON t.table_schema = DATABASE() AND t.table_name = s.SequenceTableType AND t.table_type = 'BASE TABLE'
    WHERE p.TableName IS NULL OR t.table_name IS NULL OR s.UsePrefix NOT IN ('S', 'N')
       OR s.`length` <= IF(s.UsePrefix = 'S', CHAR_LENGTH(s.Prefix), 0)
       OR s.`length` - IF(s.UsePrefix = 'S', CHAR_LENGTH(s.Prefix), 0) > 18
       OR (p.TableAction <> 'C' AND
           (SELECT COUNT(*) FROM information_schema.key_column_usage k
            WHERE k.table_schema = DATABASE() AND k.table_name = s.SequenceTableType
              AND k.constraint_name = 'PRIMARY') <> 1);
    IF v_TableName IS NOT NULL THEN
        SET v_Message = CONCAT('Revise el destino/formato de la secuencia global: ', v_TableName);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
    END IF;

    CREATE TEMPORARY TABLE `_initialization_users` AS
        SELECT * FROM app_user WHERE UserCod IN ('ROOT', 'USER_WEB');
    CREATE TEMPORARY TABLE `_initialization_people` AS
        SELECT DISTINCT p.* FROM person p JOIN app_user u ON u.PersonCod = p.PersonCod
        WHERE u.UserCod IN ('ROOT', 'USER_WEB');
    CREATE TEMPORARY TABLE `_initialization_user_profiles` AS
        SELECT * FROM user_profile WHERE UserCod IN ('ROOT', 'USER_WEB');

    -- Una definicion por tipo, prefiriendo T001 y el periodo activo original.
    -- Se conservan prefijos/longitudes, no numeros ni tiendas/periodos historicos.
    CREATE TEMPORARY TABLE `_initialization_store_sequences` AS
        SELECT SequenceTableType, Prefix, SequenceLength FROM (
            SELECT ss.SequenceTableType, ss.Prefix, ss.SequenceLength,
                   ROW_NUMBER() OVER (PARTITION BY ss.SequenceTableType
                       ORDER BY (ss.StoreCod = 'T001') DESC, (p.Status = 'A') DESC,
                                ss.PeriodId DESC, ss.StoreCod, ss.Prefix, ss.SequenceLength) AS TemplateOrder
            FROM store_sequence ss JOIN period p ON p.PeriodId = ss.PeriodId
        ) templates WHERE TemplateOrder = 1;
    -- transfer_request_head no exige una definicion propia: conservarla solo si existe.
    -- Mantener las ocho definiciones operativas sin inventar prefijos adicionales.
    SELECT MIN(required_sequences.SequenceTableType) INTO v_TableName
    FROM (
        SELECT 'pucharse_request_head' AS SequenceTableType
        UNION ALL SELECT 'pucharse_head'
        UNION ALL SELECT 'stock_entry_head'
        UNION ALL SELECT 'stock_exit_head'
        UNION ALL SELECT 'transfer_head'
        UNION ALL SELECT 'presale_head'
        UNION ALL SELECT 'sale_head'
        UNION ALL SELECT 'credit_note_head'
    ) required_sequences
    LEFT JOIN `_initialization_store_sequences` templates
        ON templates.SequenceTableType = required_sequences.SequenceTableType
    WHERE templates.SequenceTableType IS NULL;
    IF v_TableName IS NOT NULL THEN
        SET v_Message = CONCAT('Falta la secuencia operativa: ', v_TableName);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
    END IF;
    IF EXISTS (SELECT 1 FROM `_initialization_store_sequences`
               WHERE SequenceLength < 1 OR SequenceLength > 18) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Faltan definiciones de secuencias operativas o tienen longitudes invalidas';
    END IF;

    -- Preparar las semillas antes del primer TRUNCATE detecta incompatibilidades de columnas.
    CREATE TEMPORARY TABLE `_initialization_company` LIKE company;
    INSERT INTO `_initialization_company` (
        CompanyCod, TaxId, LegalName, TradeName, FiscalAddress, Address, UbigeoCod,
        Department, Province, District, CountryCode, Phone, Email, Website, LogoPath,
        Status, CreationUser, CreationDate, ModifyUser, ModifyDate
    ) VALUES (
        'C001', '20000000001', 'COMPANY_DEFAULT', NULL, 'Av. Principal 123', NULL, '140101',
        NULL, NULL, NULL, 'PE', NULL, NULL, NULL, NULL,
        'A', 'SYSTEM', CURRENT_TIMESTAMP, NULL, NULL
    );
    CREATE TEMPORARY TABLE `_initialization_store` LIKE store;
    INSERT INTO `_initialization_store` (
        StoreCod, Name, Description, Address, UbigeoCod, SunatAddressTypeCode,
        IsVirtualStoreEnabled, Latitude, Longitude, CreationUser, CreationDate,
        ModifyUser, ModifyDate, Status, CompanyCod, CountryCod
    ) VALUES (
        'T001', 'STORE_DEFAULT', NULL, NULL, NULL, '0000', 'N', NULL, NULL,
        'SYSTEM', CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 'A', 'C001', 'PER'
    );
    CREATE TEMPORARY TABLE `_initialization_period` LIKE period;
    INSERT INTO `_initialization_period` (
        PeriodId, PeriodCod, PeriodDesc, StartDate, EndDate, CreationUser, CreationDate,
        ModifyUser, ModifyDate, Status
    ) VALUES (1, 'INICIAL', 'Periodo inicial de instalacion', NULL, NULL,
              'SYSTEM', CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, 'A');

    -- Semillas exactas de los dos archivos de talonarios suministrados por el usuario.
    CREATE TEMPORARY TABLE `_initialization_counterfoil` LIKE counterfoil;
    INSERT INTO `_initialization_counterfoil` (
        CounterfoilCod, DocumentType, Series, Correlative, IsAutomatic,
        CreationUser, CreationDate, ModifyUser, ModifyDate, Status, GroupDocument
    ) VALUES
        ('01F001','01','F001',0,'S','ROOT','2026-08-24 17:01:40','','2026-08-24 16:57:24','A','F'),
        ('03B001','03','B001',0,'S','ROOT','2026-08-24 17:01:56','','2026-08-24 17:01:45','A','B'),
        ('07B001','07','B001',0,'S','ROOT','2026-08-24 17:18:07','','2026-08-24 17:17:55','A','B'),
        ('07F001','07','F001',0,'S','ROOT','2026-08-24 17:17:46','','2026-08-24 17:17:27','A','F'),
        ('09T001','09','T001',0,'S','ROOT','2026-08-24 17:05:02','','2026-08-24 17:02:06','A','G');
    CREATE TEMPORARY TABLE `_initialization_counterfoil_store` LIKE counterfoil_store;
    INSERT INTO `_initialization_counterfoil_store` (
        CounterfoilCod, StoreCod, CreationUser, CreationDate, ModifyUser, ModifyDate, Status
    ) VALUES
        ('01F001','T001','ROOT','2026-08-24 17:01:40','','2026-08-24 16:57:24','A'),
        ('03B001','T001','ROOT','2026-08-24 17:01:56','','2026-08-24 17:01:45','A'),
        ('07B001','T001','ROOT','2026-08-24 17:18:07','','2026-08-24 17:17:55','A'),
        ('07F001','T001','ROOT','2026-08-24 17:17:46','','2026-08-24 17:17:27','A'),
        ('09T001','T001','ROOT','2026-08-24 17:05:02','','2026-08-24 17:02:06','A');

    -- Solo en esta conexion. TRUNCATE no ejecuta triggers DELETE y reinicia AUTO_INCREMENT.
    SET SESSION FOREIGN_KEY_CHECKS = 0;
    BEGIN
        DECLARE v_Done boolean DEFAULT FALSE;
        DECLARE v_Action char(1);
        DECLARE v_KeepCondition text;
        DECLARE table_cursor CURSOR FOR
            SELECT p.TableName, p.TableAction, p.KeepCondition FROM `_initialization_table_plan` p
            JOIN information_schema.tables t ON t.table_schema = DATABASE()
              AND t.table_name = p.TableName AND t.table_type = 'BASE TABLE'
            WHERE p.TableAction IN ('C', 'S')
               OR (p.TableAction = 'R' AND p.TableName <> 'table_sequence')
            ORDER BY p.TableName;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = TRUE;
        OPEN table_cursor;
        table_loop: LOOP
            FETCH table_cursor INTO v_TableName, v_Action, v_KeepCondition;
            IF v_Done THEN LEAVE table_loop; END IF;
            IF v_Action = 'S' THEN
                IF v_KeepCondition IS NULL THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Falta predicado para una tabla protegida';
                END IF;
                SET @InitializationSql = CONCAT('DELETE FROM `db_store_lts`.`',
                    REPLACE(v_TableName, '`', '``'), '` WHERE NOT (COALESCE((', v_KeepCondition, '), FALSE))');
            ELSE
                SET @InitializationSql = CONCAT('TRUNCATE TABLE `db_store_lts`.`',
                    REPLACE(v_TableName, '`', '``'), '`');
            END IF;
            PREPARE InitializationStatement FROM @InitializationSql;
            SET v_PreparedStatementActive = TRUE;
            EXECUTE InitializationStatement;
            DEALLOCATE PREPARE InitializationStatement;
            SET v_PreparedStatementActive = FALSE;
        END LOOP;
        CLOSE table_cursor;
    END;

    -- table_sequence es configuracion: no truncar sus definiciones.
    UPDATE table_sequence SET SequenceTrx = 0;
    INSERT INTO person SELECT * FROM `_initialization_people`;
    INSERT INTO app_user SELECT * FROM `_initialization_users`;
    UPDATE app_user SET RecoveryCod = NULL, PasswordDecoded = NULL,
        ModifyUser = 'SYSTEM', ModifyDate = CURRENT_TIMESTAMP;
    INSERT INTO user_profile SELECT * FROM `_initialization_user_profiles`;
    INSERT INTO company SELECT * FROM `_initialization_company`;
    INSERT INTO period SELECT * FROM `_initialization_period`;

    -- El trigger original delega en sp_initalize_store_automation: crea T0010001.
    INSERT INTO store SELECT * FROM `_initialization_store`;
    INSERT INTO counterfoil SELECT * FROM `_initialization_counterfoil`;
    INSERT INTO counterfoil_store SELECT * FROM `_initialization_counterfoil_store`;
    -- Sustituir las dos secuencias de stock creadas por el trigger por las plantillas completas.
    TRUNCATE TABLE store_sequence;
    INSERT INTO store_sequence (StoreCod, PeriodId, SequenceTrx, Prefix, SequenceTableType, SequenceLength)
        SELECT 'T001', 1, 0, Prefix, SequenceTableType, SequenceLength
        FROM `_initialization_store_sequences`;

    -- El trigger original crea las cajas y consume la secuencia cash_register.
    INSERT INTO user_store (UserCod, StoreCod, IsMainStore, CreationUser, CreationDate, Status)
        SELECT UserCod, 'T001', 'S', 'SYSTEM', CURRENT_TIMESTAMP, 'A'
        FROM app_user ORDER BY UserCod;

    -- payment_method queda intacta. En business_config solo vaciar el Access Token
    -- privado en Str3Config; conservar Public Key, estado, modo y todos los demas campos.
    UPDATE business_config
    SET Str3Config = '', ModifyDate = ModifyDate
    WHERE GroupCod = 'MercadoPagoEcommerce' AND ConfigCod = 'MercadoPagoCheckout';

    -- Recalcular el minimo seguro de los maestros conservados/recreados.
    -- No poner company/store/cash_register/perfiles/etc. en cero si sus codigos ya existen.
    BEGIN
        DECLARE v_Done boolean DEFAULT FALSE;
        DECLARE v_ColumnName varchar(64);
        DECLARE v_DataType varchar(64);
        DECLARE v_Prefix varchar(2);
        DECLARE v_CodeLength int;
        DECLARE v_NumericLength int;
        DECLARE v_UsePrefix char(1);
        DECLARE sequence_cursor CURSOR FOR
            SELECT s.SequenceTableType, k.column_name, c.data_type,
                   s.Prefix, s.`length`, s.UsePrefix
            FROM table_sequence s
            JOIN `_initialization_table_plan` p ON p.TableName = s.SequenceTableType
            JOIN information_schema.key_column_usage k ON k.table_schema = DATABASE()
              AND k.table_name = s.SequenceTableType AND k.constraint_name = 'PRIMARY'
            JOIN information_schema.columns c ON c.table_schema = k.table_schema
              AND c.table_name = k.table_name AND c.column_name = k.column_name
            WHERE p.TableAction <> 'C' ORDER BY s.SequenceTableType;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = TRUE;
        OPEN sequence_cursor;
        sequence_loop: LOOP
            FETCH sequence_cursor INTO v_TableName, v_ColumnName, v_DataType,
                                       v_Prefix, v_CodeLength, v_UsePrefix;
            IF v_Done THEN LEAVE sequence_loop; END IF;
            SET v_NumericLength = v_CodeLength - IF(v_UsePrefix = 'S', CHAR_LENGTH(v_Prefix), 0);
            SET @InitializationPrefix = IF(v_UsePrefix = 'S', v_Prefix, '');
            SET @InitializationPrefixLength = CHAR_LENGTH(@InitializationPrefix);
            SET @InitializationCodeLength = v_CodeLength;
            SET @InitializationSql = CONCAT(
                'SELECT COALESCE(MAX(CAST(SUBSTRING(CAST(`', REPLACE(v_ColumnName, '`', '``'),
                '` AS CHAR), @InitializationPrefixLength + 1) AS UNSIGNED)), 0) ',
                'INTO @InitializationSequenceMinimum FROM `db_store_lts`.`', REPLACE(v_TableName, '`', '``'),
                '` WHERE LEFT(CAST(`', REPLACE(v_ColumnName, '`', '``'),
                '` AS CHAR), @InitializationPrefixLength) = @InitializationPrefix ',
                'AND CHAR_LENGTH(CAST(`', REPLACE(v_ColumnName, '`', '``'), '` AS CHAR)) ',
                IF(v_DataType IN ('tinyint','smallint','mediumint','int','bigint','decimal'), '<=', '='),
                ' @InitializationCodeLength AND SUBSTRING(CAST(`', REPLACE(v_ColumnName, '`', '``'),
                '` AS CHAR), @InitializationPrefixLength + 1) REGEXP ''^[0-9]+$''');
            PREPARE InitializationStatement FROM @InitializationSql;
            SET v_PreparedStatementActive = TRUE;
            EXECUTE InitializationStatement;
            DEALLOCATE PREPARE InitializationStatement;
            SET v_PreparedStatementActive = FALSE;
            IF @InitializationSequenceMinimum >= POW(10, v_NumericLength) - 1 THEN
                SET v_Message = CONCAT('Secuencia sin espacio disponible: ', v_TableName);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
            END IF;
            UPDATE table_sequence SET SequenceTrx = @InitializationSequenceMinimum
            WHERE SequenceTableType = v_TableName;
        END LOOP;
        CLOSE sequence_cursor;
    END;

    -- DELETE conserva identidades existentes; ajustar el siguiente AUTO_INCREMENT
    -- al minimo seguro en tablas S, sin cambiar las filas protegidas.
    BEGIN
        DECLARE v_Done boolean DEFAULT FALSE;
        DECLARE v_ColumnName varchar(64);
        DECLARE auto_increment_cursor CURSOR FOR
            SELECT c.table_name, c.column_name FROM information_schema.columns c
            JOIN `_initialization_table_plan` p ON p.TableName = c.table_name
            WHERE c.table_schema = DATABASE() AND c.extra LIKE '%auto_increment%'
              AND p.TableAction = 'S' ORDER BY c.table_name;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = TRUE;
        OPEN auto_increment_cursor;
        auto_increment_loop: LOOP
            FETCH auto_increment_cursor INTO v_TableName, v_ColumnName;
            IF v_Done THEN LEAVE auto_increment_loop; END IF;
            SET @InitializationSql = CONCAT('SELECT COALESCE(MAX(`', REPLACE(v_ColumnName, '`', '``'),
                '`), 0) + 1 INTO @InitializationNextIdentity FROM `db_store_lts`.`',
                REPLACE(v_TableName, '`', '``'), '`');
            PREPARE InitializationStatement FROM @InitializationSql;
            SET v_PreparedStatementActive = TRUE;
            EXECUTE InitializationStatement;
            DEALLOCATE PREPARE InitializationStatement;
            SET v_PreparedStatementActive = FALSE;
            SET @InitializationSql = CONCAT('ALTER TABLE `db_store_lts`.`',
                REPLACE(v_TableName, '`', '``'), '` AUTO_INCREMENT = ', @InitializationNextIdentity);
            PREPARE InitializationStatement FROM @InitializationSql;
            SET v_PreparedStatementActive = TRUE;
            EXECUTE InitializationStatement;
            DEALLOCATE PREPARE InitializationStatement;
            SET v_PreparedStatementActive = FALSE;
        END LOOP;
        CLOSE auto_increment_cursor;
    END;

    SET SESSION FOREIGN_KEY_CHECKS = v_PreviousForeignKeyChecks;
    SET SESSION group_concat_max_len = 65535;

    -- Activar FK no revalida datos existentes: comprobar todas, incluidas las compuestas.
    CREATE TEMPORARY TABLE `_initialization_fk_checks` (
        `TableName` varchar(64) NOT NULL COMMENT 'Tabla hija validada',
        `ConstraintName` varchar(64) NOT NULL COMMENT 'Clave foranea comprobada',
        `OrphanRows` bigint NOT NULL COMMENT 'Filas sin padre; debe ser cero'
    );
    BEGIN
        DECLARE v_Done boolean DEFAULT FALSE;
        DECLARE v_ConstraintName varchar(64);
        DECLARE v_ParentTable varchar(64);
        DECLARE v_JoinCondition text;
        DECLARE v_NotNullCondition text;
        DECLARE foreign_key_cursor CURSOR FOR
            SELECT table_name, constraint_name, referenced_table_name,
                GROUP_CONCAT(CONCAT('parent.`', REPLACE(referenced_column_name, '`', '``'),
                    '` = child.`', REPLACE(column_name, '`', '``'), '`')
                    ORDER BY ordinal_position SEPARATOR ' AND '),
                GROUP_CONCAT(CONCAT('child.`', REPLACE(column_name, '`', '``'), '` IS NOT NULL')
                    ORDER BY ordinal_position SEPARATOR ' AND ')
            FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE() AND referenced_table_schema = DATABASE()
              AND referenced_table_name IS NOT NULL
            GROUP BY table_name, constraint_name, referenced_table_name
            ORDER BY table_name, constraint_name;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = TRUE;
        OPEN foreign_key_cursor;
        foreign_key_loop: LOOP
            FETCH foreign_key_cursor INTO v_TableName, v_ConstraintName, v_ParentTable,
                                          v_JoinCondition, v_NotNullCondition;
            IF v_Done THEN LEAVE foreign_key_loop; END IF;
            SET @InitializationSql = CONCAT(
                'SELECT COUNT(*) INTO @InitializationOrphanRows FROM `db_store_lts`.`',
                REPLACE(v_TableName, '`', '``'), '` child WHERE ', v_NotNullCondition,
                ' AND NOT EXISTS (SELECT 1 FROM `db_store_lts`.`', REPLACE(v_ParentTable, '`', '``'),
                '` parent WHERE ', v_JoinCondition, ')');
            PREPARE InitializationStatement FROM @InitializationSql;
            SET v_PreparedStatementActive = TRUE;
            EXECUTE InitializationStatement;
            DEALLOCATE PREPARE InitializationStatement;
            SET v_PreparedStatementActive = FALSE;
            INSERT INTO `_initialization_fk_checks` VALUES (v_TableName, v_ConstraintName, @InitializationOrphanRows);
            IF @InitializationOrphanRows <> 0 THEN
                SET v_Message = CONCAT('Hay filas huerfanas en ', v_TableName, ': ', v_ConstraintName);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
            END IF;
        END LOOP;
        CLOSE foreign_key_cursor;
    END;

    CREATE TEMPORARY TABLE `_initialization_table_counts` (
        `TableName` varchar(64) NOT NULL COMMENT 'Tabla comprobada',
        `TableAction` char(1) NOT NULL COMMENT 'C: vaciar; K: conservar; R: reinicializar; S: eliminar selectivamente',
        `RemainingRows` bigint NOT NULL COMMENT 'Filas finales; C debe tener cero'
    );
    BEGIN
        DECLARE v_Done boolean DEFAULT FALSE;
        DECLARE v_Action char(1);
        DECLARE v_KeepCondition text;
        DECLARE count_cursor CURSOR FOR
            SELECT p.TableName, p.TableAction, p.KeepCondition FROM `_initialization_table_plan` p
            JOIN information_schema.tables t ON t.table_schema = DATABASE()
              AND t.table_name = p.TableName AND t.table_type = 'BASE TABLE'
            ORDER BY p.TableName;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = TRUE;
        OPEN count_cursor;
        count_loop: LOOP
            FETCH count_cursor INTO v_TableName, v_Action, v_KeepCondition;
            IF v_Done THEN LEAVE count_loop; END IF;
            SET @InitializationSql = CONCAT('SELECT COUNT(*) INTO @InitializationRemainingRows ',
                'FROM `db_store_lts`.`', REPLACE(v_TableName, '`', '``'), '`');
            PREPARE InitializationStatement FROM @InitializationSql;
            SET v_PreparedStatementActive = TRUE;
            EXECUTE InitializationStatement;
            DEALLOCATE PREPARE InitializationStatement;
            SET v_PreparedStatementActive = FALSE;
            INSERT INTO `_initialization_table_counts` VALUES (v_TableName, v_Action, @InitializationRemainingRows);
            IF v_Action = 'C' AND @InitializationRemainingRows <> 0 THEN
                SET v_Message = CONCAT('Una tabla operativa no quedo vacia: ', v_TableName);
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
            END IF;
            IF v_Action = 'S' THEN
                SET @InitializationSql = CONCAT('SELECT COUNT(*) INTO @InitializationUnexpectedRows ',
                    'FROM `db_store_lts`.`', REPLACE(v_TableName, '`', '``'),
                    '` WHERE NOT (COALESCE((', v_KeepCondition, '), FALSE))');
                PREPARE InitializationStatement FROM @InitializationSql;
                SET v_PreparedStatementActive = TRUE;
                EXECUTE InitializationStatement;
                DEALLOCATE PREPARE InitializationStatement;
                SET v_PreparedStatementActive = FALSE;
                IF @InitializationUnexpectedRows <> 0 THEN
                    SET v_Message = CONCAT('Hay filas adicionales en la tabla protegida: ', v_TableName);
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_Message;
                END IF;
            END IF;
        END LOOP;
        CLOSE count_cursor;
    END;

    IF (SELECT COUNT(*) FROM app_user) <> 2
       OR (SELECT COUNT(*) FROM company) <> 1 OR (SELECT COUNT(*) FROM store) <> 1
       OR (SELECT COUNT(*) FROM period WHERE Status = 'A') <> 1
       OR (SELECT COUNT(*) FROM warehouse WHERE WarehouseCod = 'T0010001' AND StoreCod = 'T001') <> 1
       OR (SELECT COUNT(*) FROM warehouse) <> 1
       OR (SELECT COUNT(*) FROM user_store WHERE StoreCod = 'T001' AND IsMainStore = 'S') <> 2
       OR (SELECT COUNT(*) FROM cash_register WHERE StoreCod = 'T001' AND UserCod IN ('ROOT','USER_WEB')) <> 2
       OR (SELECT COUNT(*) FROM cash_register) <> 2
       OR (SELECT COUNT(*) FROM product WHERE ProductCod = 'D0000001') <> 1
       OR (SELECT COUNT(*) FROM product) <> 1
       OR (SELECT COUNT(*) FROM product_config WHERE ProductCod = 'D0000001' AND StoreCod = 'T001') <> 1
       OR (SELECT COUNT(*) FROM product_search WHERE ProductCod = 'D0000001' AND StoreCod = 'T001') <> 1
       OR EXISTS (SELECT 1 FROM product_info WHERE ProductCod <> 'D0000001' OR StoreCod <> 'T001'
                   OR NumDigitalStock <> 0 OR NumPhysicalStock <> 0 OR NumUnavailableStock <> 0
                   OR NumReservedStock <> 0 OR NumTotalStock <> 0)
       OR EXISTS (SELECT 1 FROM product_info_warehouse WHERE ProductCod <> 'D0000001' OR WarehouseCod <> 'T0010001'
                   OR NumDigitalStock <> 0 OR NumPhysicalStock <> 0 OR NumUnavailableStock <> 0
                   OR NumReservedStock <> 0 OR NumTotalStock <> 0)
       OR (SELECT COUNT(*) FROM counterfoil) <> 5
       OR EXISTS (SELECT 1 FROM counterfoil WHERE Correlative <> 0)
       OR (SELECT COUNT(*) FROM counterfoil_store WHERE StoreCod = 'T001') <> 5
       OR (SELECT COUNT(*) FROM counterfoil_store) <> 5
       OR EXISTS (SELECT 1 FROM store_sequence WHERE SequenceTrx <> 0 OR StoreCod <> 'T001' OR PeriodId <> 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estado inicial incompleto; no exporte esta copia';
    END IF;

    SET SESSION group_concat_max_len = v_PreviousGroupConcatMaxLen;
    SELECT DATABASE() AS InitializedDatabase, 'OK' AS Result,
           @@SESSION.FOREIGN_KEY_CHECKS AS ForeignKeyChecks,
           (SELECT COUNT(*) FROM `_initialization_table_counts`) AS TablesReviewed,
           (SELECT COUNT(*) FROM `_initialization_fk_checks`) AS ForeignKeysReviewed;
    SELECT * FROM `_initialization_table_counts` ORDER BY TableAction, TableName;
    SELECT * FROM `_initialization_fk_checks` ORDER BY TableName, ConstraintName;
    SELECT SequenceTableType, SequenceTrx, Prefix, `length`, UsePrefix
    FROM table_sequence ORDER BY SequenceTableType;

    DROP TEMPORARY TABLE `_initialization_table_plan`, `_initialization_users`,
        `_initialization_people`, `_initialization_user_profiles`, `_initialization_store_sequences`,
        `_initialization_company`, `_initialization_store`, `_initialization_period`,
        `_initialization_table_counts`, `_initialization_fk_checks`,
        `_initialization_protected_categories`, `_initialization_counterfoil`, `_initialization_counterfoil_store`;
END $$
DELIMITER ;

CALL `db_store_lts`.`p_initialize_clean_store`();
DROP PROCEDURE `db_store_lts`.`p_initialize_clean_store`;
SET @InitializationSelectedDatabase = NULL;
SET @InitializationSql = NULL;
SET @InitializationPrefix = NULL;
SET @InitializationPrefixLength = NULL;
SET @InitializationCodeLength = NULL;
SET @InitializationSequenceMinimum = NULL;
SET @InitializationOrphanRows = NULL;
SET @InitializationRemainingRows = NULL;
SET @InitializationUnexpectedRows = NULL;
SET @InitializationNextIdentity = NULL;
