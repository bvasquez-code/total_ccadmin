DROP PROCEDURE IF EXISTS `p_manage_cash_session_item`;

DELIMITER $$

CREATE PROCEDURE `p_manage_cash_session_item`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'cash_session_item'
    ) THEN
        CREATE TABLE `cash_session_item` (
          `ItemID` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador unico del item de la sesion',
          `CashSessionID` bigint NOT NULL COMMENT 'Sesion de caja a la que pertenece el item',
          `ItemType` char(1) NOT NULL COMMENT 'Tipo de item: D=Denominacion, P=Medio de pago, M=Movimiento',
          `Denomination` decimal(10,2) DEFAULT NULL COMMENT 'Valor de la denominacion cuando el tipo es D',
          `Qty` int DEFAULT NULL COMMENT 'Cantidad de unidades de la denominacion cuando el tipo es D',
          `PaymentMethodCod` varchar(8) DEFAULT NULL COMMENT 'Medio de pago cuando el tipo es P',
          `MovementType` char(2) DEFAULT NULL COMMENT 'Tipo de movimiento: IN=Ingreso, OU=Salida',
          `ReferenceCod` varchar(32) DEFAULT NULL COMMENT 'Codigo opcional del documento u operacion de referencia',
          `Amount` decimal(16,2) NOT NULL DEFAULT '0.00' COMMENT 'Importe del item expresado en la moneda indicada',
          `CurrencyCod` varchar(5) NOT NULL COMMENT 'Moneda utilizada para el importe del item',
          `Commenter` varchar(128) DEFAULT NULL COMMENT 'Comentario opcional del item',
          `CreationUser` varchar(16) NOT NULL COMMENT 'Usuario que creo el registro',
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de creacion del registro',
          `ModifyUser` varchar(16) DEFAULT NULL COMMENT 'Usuario que realizo la ultima modificacion',
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha y hora de la ultima modificacion',
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado logico del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`ItemID`),
          KEY `idx_cash_item_session` (`CashSessionID`),
          KEY `idx_cash_item_type` (`ItemType`),
          KEY `idx_cash_item_paymethod` (`PaymentMethodCod`),
          KEY `fk_cash_item_currency` (`CurrencyCod`),
          CONSTRAINT `fk_cash_item_currency`
            FOREIGN KEY (`CurrencyCod`) REFERENCES `currency` (`CurrencyCod`),
          CONSTRAINT `fk_cash_item_paymethod`
            FOREIGN KEY (`PaymentMethodCod`) REFERENCES `payment_method` (`PaymentMethodCod`),
          CONSTRAINT `fk_cash_item_session`
            FOREIGN KEY (`CashSessionID`) REFERENCES `cash_session` (`CashSessionID`),
          CONSTRAINT `cash_session_item_chk_1`
            CHECK (`ItemType` in (_utf8mb4'D', _utf8mb4'P', _utf8mb4'M')),
          CONSTRAINT `cash_session_item_chk_2`
            CHECK (`ItemType` <> _utf8mb4'D' OR (`Denomination` is not null AND `Qty` is not null)),
          CONSTRAINT `cash_session_item_chk_3`
            CHECK (`ItemType` <> _utf8mb4'P' OR `PaymentMethodCod` is not null),
          CONSTRAINT `cash_session_item_chk_4`
            CHECK (`ItemType` <> _utf8mb4'M' OR `MovementType` in (_utf8mb4'IN', _utf8mb4'OU'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
          COMMENT='Denominaciones, pagos y movimientos asociados a una sesion de caja';

        SELECT 'Tabla cash_session_item creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla cash_session_item ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_cash_session_item`();
DROP PROCEDURE `p_manage_cash_session_item`;
