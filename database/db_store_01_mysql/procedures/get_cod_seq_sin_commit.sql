-- SOLO PARA PROCESOS AUTOMATICOS EJECUTADOS DENTRO DE UNA TRANSACCION O TRIGGER.
-- No modifica autocommit y no devuelve resultsets; entrega el codigo mediante OUT.
-- El incremento forma parte de la transaccion llamadora. Si esta se revierte,
-- la secuencia tambien se revierte y el numero puede reutilizarse posteriormente.
-- Por eso no debe exponerse ni reservarse el codigo antes de confirmar el proceso:
-- una referencia externa podria interpretar su reutilizacion como sobreescritura.
-- El bloqueo de la fila de table_sequence evita sobreescrituras entre transacciones
-- concurrentes. Para operaciones normales continuar utilizando get_cod_seq.

DELIMITER $$

DROP PROCEDURE IF EXISTS `get_cod_seq_sin_commit` $$

CREATE PROCEDURE `get_cod_seq_sin_commit`(
    IN p_SequenceTableType varchar(32),
    OUT p_CodTrx varchar(32)
)
BEGIN
    UPDATE `table_sequence`
    SET `SequenceTrx` = `SequenceTrx` + 1
    WHERE `SequenceTableType` = p_SequenceTableType;

    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No existe configuracion para la secuencia automatica solicitada';
    END IF;

    SELECT CASE
               WHEN `UsePrefix` = 'S'
                   THEN concat(`Prefix`, lpad(`SequenceTrx`, `length` - length(`Prefix`), '0'))
               ELSE lpad(`SequenceTrx`, `length`, '0')
           END
    INTO p_CodTrx
    FROM `table_sequence`
    WHERE `SequenceTableType` = p_SequenceTableType;
END $$

DELIMITER ;
