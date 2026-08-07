-- Inicializa los recursos automaticos correspondientes a una asociacion usuario-tienda.
-- Este procedimiento se ejecuta dentro de la transaccion que inserta user_store;
-- por ello utiliza get_cod_seq_sin_commit y no debe invocarse como generador manual.

DELIMITER $$

DROP PROCEDURE IF EXISTS `sp_initialize_user_store_automation` $$

CREATE PROCEDURE `sp_initialize_user_store_automation`(
    IN p_UserCod varchar(16),
    IN p_StoreCod varchar(4),
    IN p_CreationUser varchar(16)
)
BEGIN
    DECLARE v_RegisterCod varchar(8);

    IF NOT EXISTS (
        SELECT 1
        FROM `cash_register`
        WHERE `UserCod` = p_UserCod
          AND `StoreCod` = p_StoreCod
    ) THEN
        CALL `get_cod_seq_sin_commit`('cash_register', v_RegisterCod);

        INSERT INTO `cash_register` (
            `RegisterCod`, `StoreCod`, `UserCod`, `Name`, `Description`,
            `SerialNumber`, `CreationUser`, `CreationDate`, `Status`
        ) VALUES (
            v_RegisterCod,
            p_StoreCod,
            p_UserCod,
            concat('Caja ', p_UserCod),
            concat('Caja creada automaticamente para el usuario ', p_UserCod),
            concat(p_StoreCod, v_RegisterCod),
            p_CreationUser,
            CURRENT_TIMESTAMP,
            'A'
        );
    END IF;
END $$

DELIMITER ;
