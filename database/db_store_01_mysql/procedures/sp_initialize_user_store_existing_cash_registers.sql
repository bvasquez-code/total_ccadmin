-- Regulariza las asociaciones usuario-tienda creadas antes de instalar el trigger.
-- Delega la creacion en sp_initialize_user_store_automation para mantener un solo
-- nucleo de inicializacion. Puede ejecutarse nuevamente sin duplicar cajas.

DELIMITER $$

DROP PROCEDURE IF EXISTS `sp_initialize_user_store_existing_cash_registers` $$

CREATE PROCEDURE `sp_initialize_user_store_existing_cash_registers`()
BEGIN
    DECLARE v_Done tinyint DEFAULT 0;
    DECLARE v_UserCod varchar(16);
    DECLARE v_StoreCod varchar(4);
    DECLARE v_CreationUser varchar(16);

    DECLARE cur_UserStore CURSOR FOR
        SELECT us.`UserCod`, us.`StoreCod`, us.`CreationUser`
        FROM `user_store` us
        WHERE NOT EXISTS (
            SELECT 1
            FROM `cash_register` cr
            WHERE cr.`UserCod` = us.`UserCod`
              AND cr.`StoreCod` = us.`StoreCod`
        )
        ORDER BY us.`UserCod`, us.`StoreCod`;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_Done = 1;

    OPEN cur_UserStore;

    read_loop: LOOP
        FETCH cur_UserStore INTO v_UserCod, v_StoreCod, v_CreationUser;

        IF v_Done = 1 THEN
            LEAVE read_loop;
        END IF;

        CALL `sp_initialize_user_store_automation`(
            v_UserCod,
            v_StoreCod,
            v_CreationUser
        );
    END LOOP;

    CLOSE cur_UserStore;
END $$

DELIMITER ;

CALL `sp_initialize_user_store_existing_cash_registers`();
