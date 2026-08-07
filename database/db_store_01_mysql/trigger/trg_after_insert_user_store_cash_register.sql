-- Crea una caja automatica por cada combinacion usuario-tienda.
-- Los usuarios asociados a varias tiendas reciben una caja independiente en cada una.

DROP TRIGGER IF EXISTS `trg_after_insert_user_store_cash_register`;

DELIMITER $$

CREATE TRIGGER `trg_after_insert_user_store_cash_register`
AFTER INSERT ON `user_store`
FOR EACH ROW
BEGIN
    CALL `sp_initialize_user_store_automation`(
        NEW.`UserCod`,
        NEW.`StoreCod`,
        NEW.`CreationUser`
    );
END $$

DELIMITER ;
