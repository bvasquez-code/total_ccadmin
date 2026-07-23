-- Ejecutar antes los scripts nuevos de tables en este orden:
-- table_stock_entry_head.sql, table_stock_entry_det.sql,
-- table_stock_exit_head.sql, table_stock_exit_det.sql.
-- Si ya se uso la version que creaba cabeceras de resolucion, ejecutar despues:
-- regularize_exceptional_stock_resolution_heads.sql

INSERT INTO store_sequence (StoreCod, PeriodId, SequenceTrx, Prefix, SequenceTableType, SequenceLength)
SELECT s.StoreCod, p.PeriodId, 0, q.Prefix, q.SequenceTableType, 6
FROM store s
CROSS JOIN period p
CROSS JOIN (
    SELECT 'IE' Prefix, 'stock_entry_head' SequenceTableType
    UNION ALL SELECT 'IS', 'stock_exit_head'
) q
WHERE s.Status = 'A' AND p.Status = 'A'
  AND NOT EXISTS (
      SELECT 1 FROM store_sequence ss
      WHERE ss.StoreCod = s.StoreCod
        AND ss.PeriodId = p.PeriodId
        AND ss.SequenceTableType = q.SequenceTableType
  );

INSERT INTO app_menu (MenuCod, Name, Description, IsMenuDad, MenuDadCod, CreationUser, CreationDate, Status)
VALUES
('AT000005', 'Entradas de stock', 'Movimientos excepcionales de entrada de stock', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
('AT000006', 'Crear o editar entrada', 'Formulario de entrada excepcional', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
('AT000007', 'Ver entrada de stock', 'Consulta de entrada excepcional', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
('AT000008', 'Resolver entrada', 'Resolucion de stock no disponible de una entrada', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
('AT000009', 'Retiros de stock', 'Movimientos excepcionales de retiro de stock', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
('AT000010', 'Crear o editar retiro', 'Formulario de retiro excepcional', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
('AT000011', 'Ver retiro de stock', 'Consulta de retiro excepcional', 'N', 'AT000000', 'SYSTEM', NOW(), 'A'),
('AT000012', 'Resolver retiro', 'Resolucion de stock no disponible de un retiro', 'N', 'AT000000', 'SYSTEM', NOW(), 'A')
ON DUPLICATE KEY UPDATE
    Name = VALUES(Name), Description = VALUES(Description),
    IsMenuDad = VALUES(IsMenuDad), MenuDadCod = VALUES(MenuDadCod),
    ModifyUser = 'SYSTEM', ModifyDate = NOW(), Status = 'A';

INSERT INTO profile_menu (ProfileCod, MenuCod, CreationUser, CreationDate, Status)
SELECT parent.ProfileCod, child.MenuCod, 'SYSTEM', NOW(), 'A'
FROM profile_menu parent
JOIN app_menu child ON child.MenuCod BETWEEN 'AT000005' AND 'AT000012'
WHERE parent.MenuCod = 'AT000000' AND parent.Status = 'A'
ON DUPLICATE KEY UPDATE ModifyUser = 'SYSTEM', ModifyDate = NOW(), Status = 'A';
