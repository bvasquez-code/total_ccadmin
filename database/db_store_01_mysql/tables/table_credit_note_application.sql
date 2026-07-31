DROP PROCEDURE IF EXISTS `p_manage_credit_note_application`;

DELIMITER $$

CREATE PROCEDURE `p_manage_credit_note_application`()
BEGIN
    DECLARE v_table_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'credit_note_application';

    IF v_table_exists = 0 THEN
        CREATE TABLE `credit_note_application` (
          `ApplicationId` bigint NOT NULL AUTO_INCREMENT COMMENT 'Identificador de la aplicacion del saldo',
          `CreditNoteCod` varchar(16) NOT NULL COMMENT 'Nota de credito que origina el saldo',
          `SaleCod` varchar(16) NOT NULL COMMENT 'Venta en la que se aplica el saldo',
          `TrxPaymentId` bigint NOT NULL COMMENT 'Transaccion interna NC001 asociada a la aplicacion',
          `AmountApplied` decimal(16,2) NOT NULL COMMENT 'Monto del saldo aplicado en la moneda de la nota',
          `CreationUser` varchar(16) NOT NULL,
          `CreationDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `ModifyUser` varchar(16) DEFAULT NULL,
          `ModifyDate` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          `Status` char(1) NOT NULL DEFAULT 'A' COMMENT 'Estado del registro: A=Activo, I=Inactivo',
          PRIMARY KEY (`ApplicationId`),
          UNIQUE KEY `uq_credit_note_application_trx_payment` (`TrxPaymentId`),
          KEY `idx_credit_note_application_credit_note` (`CreditNoteCod`),
          KEY `idx_credit_note_application_sale` (`SaleCod`),
          CONSTRAINT `fk_credit_note_application_credit_note`
            FOREIGN KEY (`CreditNoteCod`) REFERENCES `credit_note_head` (`CreditNoteCod`),
          CONSTRAINT `fk_credit_note_application_sale`
            FOREIGN KEY (`SaleCod`) REFERENCES `sale_head` (`SaleCod`),
          CONSTRAINT `fk_credit_note_application_trx_payment`
            FOREIGN KEY (`TrxPaymentId`) REFERENCES `trx_payments` (`TrxPaymentId`),
          CONSTRAINT `chk_credit_note_application_amount`
            CHECK (`AmountApplied` > 0),
          CONSTRAINT `chk_credit_note_application_status`
            CHECK (`Status` IN ('A', 'I'))
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

        SELECT 'Tabla credit_note_application creada desde cero.' AS Mensaje;
    ELSE
        SELECT 'Tabla credit_note_application ya existe. No se realizaron cambios estructurales.' AS Mensaje;
    END IF;
END $$

DELIMITER ;

CALL `p_manage_credit_note_application`();
DROP PROCEDURE `p_manage_credit_note_application`;
