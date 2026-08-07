DROP PROCEDURE IF EXISTS `p_manage_cash_register`;

DELIMITER $$

CREATE PROCEDURE `p_manage_cash_register`()
BEGIN
    DECLARE v_column_exists INT DEFAULT 0;
    DECLARE v_index_exists INT DEFAULT 0;
    DECLARE v_constraint_exists INT DEFAULT 0;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_register'
    ) THEN
        CREATE TABLE `cash_register` (
          `RegisterCod` varchar(8) NOT NULL COMMENT 'Codigo unico de la caja o punto de venta',
          `StoreCod` varchar(4) NOT NULL COMMENT 'Codigo de la tienda a la que pertenece la caja',
          `UserCod` varchar(16) DEFAULT NULL COMMENT 'Usuario propietario de la caja automatica; nulo para cajas generales',
          `Name` varchar(32) NOT NULL COMMENT 'Nombre visible de la caja',
          `Description` varchar(128) DEFAULT NULL COMMENT 'Descripcion opcional de la caja',
          `SerialNumber` varchar(64) DEFAULT NULL COMMENT 'Numero de serie opcional del equipo asociado',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`RegisterCod`),
          KEY `idx_cash_register_store` (`StoreCod`),
          UNIQUE KEY `uq_cash_register_user_store` (`UserCod`,`StoreCod`),
          CONSTRAINT `fk_cash_register_store`
            FOREIGN KEY (`StoreCod`) REFERENCES `store` (`StoreCod`),
          CONSTRAINT `fk_cash_register_user`
            FOREIGN KEY (`UserCod`) REFERENCES `app_user` (`UserCod`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Cajas o puntos de venta disponibles por tienda';

        SELECT 'Tabla cash_register creada desde cero.' AS Mensaje;
    ELSE
        SELECT COUNT(*) INTO v_column_exists
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_register'
          AND column_name = 'UserCod';

        IF v_column_exists = 0 THEN
            ALTER TABLE `cash_register`
                ADD COLUMN `UserCod` varchar(16) DEFAULT NULL
                    COMMENT 'Usuario propietario de la caja automatica; nulo para cajas generales'
                    AFTER `StoreCod`;
        END IF;

        SELECT COUNT(*) INTO v_index_exists
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_register'
          AND index_name = 'uq_cash_register_user_store';

        IF v_index_exists = 0 THEN
            ALTER TABLE `cash_register`
                ADD UNIQUE KEY `uq_cash_register_user_store` (`UserCod`,`StoreCod`);
        END IF;

        SELECT COUNT(*) INTO v_constraint_exists
        FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = 'cash_register'
          AND constraint_name = 'fk_cash_register_user';

        IF v_constraint_exists = 0 THEN
            ALTER TABLE `cash_register`
                ADD CONSTRAINT `fk_cash_register_user`
                FOREIGN KEY (`UserCod`) REFERENCES `app_user` (`UserCod`);
        END IF;

        SELECT 'Tabla cash_register regularizada para cajas automaticas por usuario y tienda.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_cash_register`();
DROP PROCEDURE `p_manage_cash_register`;
