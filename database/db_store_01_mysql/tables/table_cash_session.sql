DROP PROCEDURE IF EXISTS `p_manage_cash_session`;

DELIMITER $$

CREATE PROCEDURE `p_manage_cash_session`()
BEGIN
    DECLARE v_column_exists INT DEFAULT 0;
    DECLARE v_index_exists INT DEFAULT 0;
    DECLARE v_old_index_exists INT DEFAULT 0;
    DECLARE v_register_index_exists INT DEFAULT 0;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_session'
    ) THEN
        CREATE TABLE `cash_session` (
          `CashSessionID` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador unico de la sesion de caja',
          `RegisterCod` varchar(8) NOT NULL COMMENT 'Codigo de la caja utilizada en la sesion',
          `StoreCod` varchar(4) NOT NULL COMMENT 'Codigo de la tienda de la sesion',
          `UserCod` varchar(16) NOT NULL COMMENT 'Usuario responsable de la sesion de caja',
          `CurrencyCod` varchar(5) NOT NULL COMMENT 'Moneda utilizada para los importes de la sesion',
          `OpenDate` datetime NOT NULL COMMENT 'Fecha y hora de apertura de la sesion',
          `CloseDate` datetime DEFAULT NULL COMMENT 'Fecha y hora de cierre de la sesion',
          `OpeningFloatAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Fondo inicial de la caja',
          `ExpectedCashAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Importe esperado correspondiente a efectivo',
          `ExpectedOtherAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Importe esperado correspondiente a otros medios de pago',
          `ExpectedTotalAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Importe total esperado al cierre',
          `CountedCashAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Importe de efectivo contado al cierre',
          `CountedOtherAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Importe contado de otros medios de pago',
          `CountedTotalAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Importe total contado al cierre',
          `DifferenceAmount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Diferencia entre el importe contado y el esperado',
          `SessionStatus` char(1) NOT NULL DEFAULT 'O' COMMENT 'Estado de la sesion: O=Abierta, C=Cerrada, X=Anulada',
          `IsOpen` tinyint NOT NULL DEFAULT '1' COMMENT 'Indicador de apertura: 1=Abierta, 0=No abierta',
          `OpenRegisterCod` varchar(8)
            GENERATED ALWAYS AS (CASE WHEN `IsOpen` = 1 THEN `RegisterCod` ELSE NULL END) STORED
            COMMENT 'Codigo generado solo mientras la sesion permanece abierta',
          `Commenter` varchar(128) DEFAULT NULL COMMENT 'Comentario opcional de la sesion de caja',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`CashSessionID`),
          UNIQUE KEY `uq_cash_session_register_open` (`OpenRegisterCod`),
          KEY `idx_cash_session_register` (`RegisterCod`),
          KEY `idx_cash_session_store` (`StoreCod`),
          KEY `idx_cash_session_user` (`UserCod`),
          KEY `idx_cash_session_status` (`SessionStatus`),
          KEY `fk_cash_session_currency` (`CurrencyCod`),
          CONSTRAINT `fk_cash_session_currency`
            FOREIGN KEY (`CurrencyCod`) REFERENCES `currency` (`CurrencyCod`),
          CONSTRAINT `fk_cash_session_register`
            FOREIGN KEY (`RegisterCod`) REFERENCES `cash_register` (`RegisterCod`),
          CONSTRAINT `fk_cash_session_store`
            FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `fk_cash_session_user`
            FOREIGN KEY (`UserCod`) REFERENCES `app_user` (`UserCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Sesiones de apertura, operacion y cierre de caja';

        SELECT 'Tabla cash_session creada desde cero.' AS Mensaje;
    ELSE
        SELECT COUNT(*) INTO v_old_index_exists
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_session'
          AND index_name = 'uq_cash_session_register_open'
          AND column_name = 'RegisterCod';

        IF v_old_index_exists > 0 THEN
            SELECT COUNT(*) INTO v_register_index_exists
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'cash_session'
              AND index_name = 'idx_cash_session_register';

            IF v_register_index_exists = 0 THEN
                ALTER TABLE `cash_session`
                    ADD KEY `idx_cash_session_register` (`RegisterCod`);
            END IF;

            ALTER TABLE `cash_session`
                DROP INDEX `uq_cash_session_register_open`;
        END IF;

        SELECT COUNT(*) INTO v_column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_session'
          AND column_name = 'OpenRegisterCod';

        IF v_column_exists = 0 THEN
            ALTER TABLE `cash_session`
                ADD COLUMN `OpenRegisterCod` varchar(8)
                    GENERATED ALWAYS AS (
                        CASE WHEN `IsOpen` = 1 THEN `RegisterCod` ELSE NULL END
                    ) STORED
                    COMMENT 'Codigo generado solo mientras la sesion permanece abierta'
                    AFTER `IsOpen`;
        END IF;

        SELECT COUNT(*) INTO v_index_exists
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_session'
          AND index_name = 'uq_cash_session_register_open';

        IF v_index_exists = 0 THEN
            ALTER TABLE `cash_session`
                ADD UNIQUE KEY `uq_cash_session_register_open` (`OpenRegisterCod`);
        END IF;

        SELECT 'Tabla cash_session regularizada para conservar multiples sesiones cerradas.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_cash_session`();
DROP PROCEDURE `p_manage_cash_session`;
