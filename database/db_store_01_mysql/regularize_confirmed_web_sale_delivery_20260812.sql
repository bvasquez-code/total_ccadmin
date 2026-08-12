-- Regulariza pedidos WEB creados antes de la incorporacion del flujo operativo.
-- Solo cambia estados iniciales para no retroceder pedidos ya listos,
-- despachados, entregados, cancelados o fallidos.

UPDATE `sale_delivery` sd
INNER JOIN `sale_head` sh
    ON sh.`SaleCod` = sd.`SaleCod`
    AND sh.`Status` = 'A'
INNER JOIN `sale_channel` sc
    ON sc.`SaleCod` = sh.`SaleCod`
    AND sc.`ChannelCod` = 'WEB'
    AND sc.`Status` = 'A'
SET
    sd.`DeliveryStatus` = 'R',
    sd.`ModifyUser` = 'REGULARIZATION',
    sd.`ModifyDate` = CURRENT_TIMESTAMP
WHERE sh.`SaleStatus` = 'C'
  AND sd.`Status` = 'A'
  AND sd.`DeliveryStatus` IN ('P', 'S');

SELECT ROW_COUNT() AS `PedidosWebRegularizados`;
