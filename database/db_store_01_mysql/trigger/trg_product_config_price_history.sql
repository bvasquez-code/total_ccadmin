DROP TRIGGER IF EXISTS `trg_product_config_price_history`;

DELIMITER $$

CREATE TRIGGER `trg_product_config_price_history`
AFTER UPDATE ON `product_config`
FOR EACH ROW
BEGIN
    IF NOT (OLD.`NumPrice` <=> NEW.`NumPrice`) THEN
        INSERT INTO `product_price_history` (
            `ProductCod`, `StoreCod`, `OldPrice`, `NewPrice`,
            `CreationUser`, `CreationDate`, `Status`
        ) VALUES (
            NEW.`ProductCod`, NEW.`StoreCod`, OLD.`NumPrice`, NEW.`NumPrice`,
            COALESCE(NULLIF(NEW.`ModifyUser`, ''), NULLIF(NEW.`CreationUser`, ''), 'SISTEMA'),
            NOW(), 'A'
        );
    END IF;
END $$

DELIMITER ;
