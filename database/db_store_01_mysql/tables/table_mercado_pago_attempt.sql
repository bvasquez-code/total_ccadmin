-- Ejecutar antes de desplegar el backend de Mercado Pago. MySQL 8, idempotente.
DROP PROCEDURE IF EXISTS p_manage_mercado_pago_attempt;
DELIMITER $$
CREATE PROCEDURE p_manage_mercado_pago_attempt()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'mercado_pago_attempt') THEN
        CREATE TABLE mercado_pago_attempt (
            AttemptId varchar(36) NOT NULL COMMENT 'UUID e idempotency key enviados a Mercado Pago',
            SaleCod varchar(16) NOT NULL COMMENT 'Pedido web propietario del intento',
            PaymentMethodCod varchar(16) NOT NULL COMMENT 'TC001 credito o TD001 debito solicitado',
            RequestHash char(64) NOT NULL COMMENT 'SHA256 del formulario tokenizado; no almacena token ni datos de tarjeta',
            CredentialHash char(64) NOT NULL COMMENT 'SHA256 de la credencial; impide reintentar con otra cuenta',
            Amount decimal(16,2) NOT NULL COMMENT 'Importe autorizado por backend en unidades de CurrencyCod',
            CurrencyCod varchar(5) NOT NULL COMMENT 'Codigo ISO de moneda del pedido',
            TestMode char(1) NOT NULL COMMENT 'S prueba; N produccion; debe coincidir con live_mode del proveedor',
            PaymentId varchar(32) DEFAULT NULL COMMENT 'Identificador de pago real de Mercado Pago',
            PaymentState char(1) NOT NULL COMMENT 'P enviado o en verificacion; C aprobado y registrado; F rechazado o cancelado',
            ProviderStatus varchar(64) DEFAULT NULL COMMENT 'Estado recibido desde la API de Mercado Pago',
            ProviderStatusDetail varchar(64) DEFAULT NULL COMMENT 'Codigo status_detail del proveedor; motivo de rechazo o detalle del estado',
            CreationUser varchar(16) NOT NULL COMMENT 'Usuario de auditoria que crea el intento',
            CreationDate datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Fecha de creacion del intento',
            ModifyUser varchar(16) DEFAULT NULL COMMENT 'Usuario de ultima actualizacion',
            ModifyDate datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Fecha de ultima conciliacion',
            Status char(1) NOT NULL DEFAULT 'A' COMMENT 'A activo; I inactivo',
            PRIMARY KEY (AttemptId),
            UNIQUE KEY uk_mercado_pago_attempt_payment (PaymentId),
            KEY idx_mercado_pago_attempt_sale (SaleCod, CreationDate),
            KEY idx_mercado_pago_attempt_pending (PaymentState, ModifyDate),
            CONSTRAINT fk_mercado_pago_attempt_sale FOREIGN KEY (SaleCod) REFERENCES sale_head (SaleCod),
            CONSTRAINT chk_mercado_pago_attempt_state CHECK (PaymentState IN ('P','C','F')),
            CONSTRAINT chk_mercado_pago_attempt_test CHECK (TestMode IN ('S','N'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'mercado_pago_attempt'
                     AND column_name = 'ProviderStatusDetail') THEN
        ALTER TABLE mercado_pago_attempt
            ADD COLUMN ProviderStatusDetail varchar(64) DEFAULT NULL
            COMMENT 'Codigo status_detail del proveedor; motivo de rechazo o detalle del estado'
            AFTER ProviderStatus;
    END IF;
END$$
DELIMITER ;
CALL p_manage_mercado_pago_attempt();
DROP PROCEDURE IF EXISTS p_manage_mercado_pago_attempt;
