-- Solo consultas. Seleccionar db_store_lts antes de ejecutar.
-- Tambien puede utilizarse en una base de verificacion donde se importe el dump final.
SELECT DATABASE() AS BaseSeleccionada, @@SESSION.foreign_key_checks AS ValidacionFK;

-- Deben aparecer exactamente ROOT y USER_WEB. No mostrar contrasenas.
SELECT UserCod, PersonCod, Status, DateExpire,
       (RecoveryCod IS NULL) AS SinRecuperacionPendiente,
       (PasswordDecoded IS NULL) AS SinContrasenaDecodificada
FROM app_user ORDER BY UserCod;
SELECT up.UserCod, up.ProfileCod, up.Status, ap.Name, ap.Status AS EstadoPerfil
FROM user_profile up JOIN app_profile ap ON ap.ProfileCod = up.ProfileCod
ORDER BY up.UserCod, up.ProfileCod;

-- Una compania C001/COMPANY_DEFAULT y una tienda T001/STORE_DEFAULT.
SELECT CompanyCod, TaxId, LegalName, FiscalAddress, UbigeoCod, Status FROM company;
SELECT StoreCod, Name, CompanyCod, CountryCod, IsVirtualStoreEnabled, Status FROM store;
SELECT * FROM period;
SELECT WarehouseCod, StoreCod, WarehouseName, Status FROM warehouse;
SELECT UserCod, StoreCod, IsMainStore, Status FROM user_store ORDER BY UserCod;
SELECT RegisterCod, StoreCod, UserCod, Status FROM cash_register ORDER BY UserCod;

-- El unico producto es delivery. Estas definiciones se conservan, no se normalizan.
SELECT * FROM product;
SELECT * FROM product_variant WHERE ProductCod = 'D0000001';
SELECT * FROM product_config WHERE ProductCod = 'D0000001';
SELECT * FROM product_picture WHERE ProductCod = 'D0000001';
-- Incluyen la marca GENERICO y el padre GENERICO con sus descendientes originales.
SELECT * FROM brand ORDER BY BrandCod;
SELECT * FROM category ORDER BY CategoryDadCod, CategoryCod;
SELECT * FROM counterfoil ORDER BY CounterfoilCod;
SELECT * FROM counterfoil_store ORDER BY CounterfoilCod, StoreCod;
-- Medios de pago e imagenes: deben coincidir con los registros de la copia original.
SELECT pm.*, f.Route AS RutaArchivoReferenciado
FROM payment_method pm LEFT JOIN app_file f ON f.FileCod = pm.FileCod
ORDER BY pm.PaymentMethodCod;

-- Todos estos resultados deben ser cero.
SELECT 'productos_no_delivery' AS Dato, COUNT(*) AS Filas FROM product WHERE ProductCod <> 'D0000001'
UNION ALL SELECT 'variantes_no_delivery', COUNT(*) FROM product_variant WHERE ProductCod <> 'D0000001'
UNION ALL SELECT 'stock_tienda_con_saldo', COUNT(*) FROM product_info
    WHERE NumDigitalStock <> 0 OR NumPhysicalStock <> 0 OR NumUnavailableStock <> 0 OR NumReservedStock <> 0 OR NumTotalStock <> 0
UNION ALL SELECT 'stock_almacen_con_saldo', COUNT(*) FROM product_info_warehouse
    WHERE NumDigitalStock <> 0 OR NumPhysicalStock <> 0 OR NumUnavailableStock <> 0 OR NumReservedStock <> 0 OR NumTotalStock <> 0
UNION ALL SELECT 'ventas', COUNT(*) FROM sale_head
UNION ALL SELECT 'preventas', COUNT(*) FROM presale_head
UNION ALL SELECT 'compras', COUNT(*) FROM pucharse_head
UNION ALL SELECT 'transferencias', COUNT(*) FROM transfer_head
UNION ALL SELECT 'notas_credito', COUNT(*) FROM credit_note_head
UNION ALL SELECT 'pagos', COUNT(*) FROM trx_payments
UNION ALL SELECT 'sesiones_usuario', COUNT(*) FROM app_session
UNION ALL SELECT 'historial_sesiones', COUNT(*) FROM app_session_history
UNION ALL SELECT 'sesiones_caja', COUNT(*) FROM cash_session
UNION ALL SELECT 'kardex', COUNT(*) FROM kardex
UNION ALL SELECT 'clientes', COUNT(*) FROM client
UNION ALL SELECT 'proveedores', COUNT(*) FROM supplier
UNION ALL SELECT 'promociones', COUNT(*) FROM promotion
UNION ALL SELECT 'talonarios_con_correlativo', COUNT(*) FROM counterfoil WHERE Correlative <> 0
UNION ALL SELECT 'imagenes_pago_sin_archivo', COUNT(*) FROM payment_method pm
    LEFT JOIN app_file f ON f.FileCod = pm.FileCod WHERE pm.FileCod IS NOT NULL AND pm.FileCod <> '' AND f.FileCod IS NULL
-- SUNAT: 0 tras limpiar, o 2 tras cargar las plantillas; ninguna debe estar activa.
UNION ALL SELECT 'configuracion_sunat', COUNT(*) FROM sunat_config
UNION ALL SELECT 'configuracion_sunat_activa', COUNT(*) FROM sunat_config WHERE ActiveConfig = 'S' AND Status = 'A'
UNION ALL SELECT 'documentos_sunat', COUNT(*) FROM sunat_document;

-- Todos los correlativos por tienda deben ser 0 y del periodo 1/T001.
SELECT StoreCod, PeriodId, SequenceTableType, Prefix, SequenceLength, SequenceTrx
FROM store_sequence ORDER BY SequenceTableType;
-- Los globales de maestros existentes tienen el minimo seguro; los vaciados, 0.
SELECT SequenceTableType, Prefix, `length`, UsePrefix, SequenceTrx
FROM table_sequence ORDER BY SequenceTableType;

-- AUTO_INCREMENT: 1 en tablas vaciadas; period: 2; impuestos delivery: MAX(ID)+1.
SELECT table_name, auto_increment FROM information_schema.tables
WHERE table_schema = DATABASE() AND auto_increment IS NOT NULL ORDER BY table_name;

-- Mercado Pago sin token privado. Clave publica, estado, modo, tarifas y horarios se conservan.
SELECT GroupCod, ConfigCod, Status, Sta1Config AS Habilitado, Sta2Config AS Pruebas,
       ConfigVal AS ClavePublicaConservada,
       (COALESCE(Str3Config, '') = '') AS SinTokenPrivado
FROM business_config
WHERE GroupCod = 'MercadoPagoEcommerce' AND ConfigCod = 'MercadoPagoCheckout';
SELECT GroupCod, ConfigCod, ConfigVal, ConfigName, Num1Config, Num2Config,
       Status, Sta1Config, Sta2Config, ModifyUser, ModifyDate
FROM business_config WHERE GroupCod IN ('ShippingConfig', 'ShippingScheduleConfig')
ORDER BY GroupCod, ConfigCorr;

-- Se mantienen procedimientos, funciones, triggers y vistas de la aplicacion.
SELECT routine_type, routine_name, definer FROM information_schema.routines
WHERE routine_schema = DATABASE() ORDER BY routine_type, routine_name;
SELECT trigger_name, event_object_table, definer FROM information_schema.triggers
WHERE trigger_schema = DATABASE() ORDER BY trigger_name;
SELECT table_name FROM information_schema.views WHERE table_schema = DATABASE();
SELECT event_name, status, definer FROM information_schema.events WHERE event_schema = DATABASE();
