fixture_user_store AS (
    SELECT 'U001' AS UserCod, 'T001' AS StoreCod, 'A' AS Status
    UNION ALL SELECT 'U001', 'T002', 'I'
    UNION ALL SELECT 'U002', 'T002', 'A'
),
fixture_kardex AS (
    SELECT 'sale_head' AS SourceTable, 'S1' AS OperationCod, 'T001' AS StoreCod,
           'R' AS TypeOperation, '2026-09-01 00:00:00' AS CreationDate, 'A' AS Status
),
fixture_store AS (
    SELECT 'T001' AS `StoreCod`, 'Local uno' AS `Name`
    UNION ALL
    SELECT 'T002' AS `StoreCod`, 'Local dos' AS `Name`
),
fixture_person AS (
    SELECT 'PERSON1' AS `PersonCod`, '' AS `BusinessName`, 'Ada' AS `Names`, 'Perez' AS `LastNames`, '12345678' AS `DocumentNum`
),
fixture_client AS (
    SELECT 'C1' AS `ClientCod`, 'PERSON1' AS `PersonCod`
),
fixture_product AS (
    SELECT 'P1' AS `ProductCod`, 'Producto uno' AS `ProductName`
    UNION ALL
    SELECT 'P2' AS `ProductCod`, 'Producto dos' AS `ProductName`
),
fixture_sale_head AS (
    SELECT 'S1' AS `SaleCod`, 'O1' AS `PresaleCod`, '2026-09-01 00:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'C' AS `SaleStatus`, 'S' AS `IsPaid`, 240.00 AS `NumPriceSubTotal`, 4.00 AS `NumDiscount`, 200.00 AS `NumTotalPriceNoTax`, 36.00 AS `NumTotalTax`, 236.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S2' AS `SaleCod`, 'O2' AS `PresaleCod`, '2026-09-01 00:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'USD' AS `CurrencyCod`, 'C' AS `SaleStatus`, 'S' AS `IsPaid`, 118.00 AS `NumPriceSubTotal`, 0.00 AS `NumDiscount`, 100.00 AS `NumTotalPriceNoTax`, 18.00 AS `NumTotalTax`, 118.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S3' AS `SaleCod`, NULL AS `PresaleCod`, '2026-09-01 00:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'X' AS `SaleStatus`, 'N' AS `IsPaid`, 999.00 AS `NumPriceSubTotal`, 0.00 AS `NumDiscount`, 999.00 AS `NumTotalPriceNoTax`, 0.00 AS `NumTotalTax`, 999.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S4' AS `SaleCod`, NULL AS `PresaleCod`, '2026-09-01 00:00:00' AS `CreationDate`, 'T002' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'C' AS `SaleStatus`, 'S' AS `IsPaid`, 999.00 AS `NumPriceSubTotal`, 0.00 AS `NumDiscount`, 999.00 AS `NumTotalPriceNoTax`, 0.00 AS `NumTotalTax`, 999.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S5' AS `SaleCod`, NULL AS `PresaleCod`, '2026-10-01 00:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'C' AS `SaleStatus`, 'S' AS `IsPaid`, 999.00 AS `NumPriceSubTotal`, 0.00 AS `NumDiscount`, 999.00 AS `NumTotalPriceNoTax`, 0.00 AS `NumTotalTax`, 999.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S6' AS `SaleCod`, NULL AS `PresaleCod`, '2026-09-30 23:59:59' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'C' AS `SaleStatus`, 'S' AS `IsPaid`, 59.00 AS `NumPriceSubTotal`, 0.00 AS `NumDiscount`, 50.00 AS `NumTotalPriceNoTax`, 9.00 AS `NumTotalTax`, 59.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S7' AS `SaleCod`, 'O1' AS `PresaleCod`, '2026-09-02 00:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'P' AS `SaleStatus`, 'N' AS `IsPaid`, 0.00 AS `NumPriceSubTotal`, 0.00 AS `NumDiscount`, 0.00 AS `NumTotalPriceNoTax`, 0.00 AS `NumTotalTax`, 0.00 AS `NumTotalPrice`, 'A' AS `Status`
),
fixture_sale_det AS (
    SELECT 'S1' AS `SaleCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 2.00 AS `NumUnit`, 100.00 AS `NumPriceSubTotal`, 18.00 AS `NumTotalTax`, 118.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S2' AS `SaleCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 100.00 AS `NumPriceSubTotal`, 18.00 AS `NumTotalTax`, 118.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S3' AS `SaleCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 100.00 AS `NumPriceSubTotal`, 18.00 AS `NumTotalTax`, 118.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S4' AS `SaleCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 100.00 AS `NumPriceSubTotal`, 18.00 AS `NumTotalTax`, 118.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S5' AS `SaleCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 100.00 AS `NumPriceSubTotal`, 18.00 AS `NumTotalTax`, 118.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S6' AS `SaleCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 50.00 AS `NumPriceSubTotal`, 9.00 AS `NumTotalTax`, 59.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'S1' AS `SaleCod`, 2.00 AS `ItemNumber`, 'P2' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 100.00 AS `NumPriceSubTotal`, 18.00 AS `NumTotalTax`, 118.00 AS `NumTotalPrice`, 'A' AS `Status`
),
fixture_product_info AS (
    SELECT 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 2.00 AS `NumPhysicalStock`, 1.00 AS `NumReservedStock`, 1.00 AS `NumUnavailableStock`, 4.00 AS `NumTotalStock`, 'A' AS `Status`
    UNION ALL
    SELECT 'P2' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 0.00 AS `NumPhysicalStock`, 0.00 AS `NumReservedStock`, 0.00 AS `NumUnavailableStock`, 0.00 AS `NumTotalStock`, 'A' AS `Status`
    UNION ALL
    SELECT 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T002' AS `StoreCod`, 90.00 AS `NumPhysicalStock`, 0.00 AS `NumReservedStock`, 0.00 AS `NumUnavailableStock`, 90.00 AS `NumTotalStock`, 'A' AS `Status`
),
fixture_product_config AS (
    SELECT 'P1' AS `ProductCod`, 'T001' AS `StoreCod`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 5.00 AS `NumMinStock`, 'A' AS `Status`
    UNION ALL
    SELECT 'P2' AS `ProductCod`, 'T001' AS `StoreCod`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumMinStock`, 'A' AS `Status`
),
fixture_payment_method AS (
    SELECT 'CASH' AS `PaymentMethodCod`, 'Efectivo' AS `Name`
),
fixture_cash_session AS (
    SELECT 1.00 AS `CashSessionID`, 'T001' AS `StoreCod`
    UNION ALL
    SELECT 2.00 AS `CashSessionID`, 'T002' AS `StoreCod`
),
fixture_sale_payments AS (
    SELECT 1.00 AS `TrxPaymentId`, 'S1' AS `SaleCod`, 'A' AS `Status`
    UNION ALL
    SELECT 1.00 AS `TrxPaymentId`, 'S1' AS `SaleCod`, 'A' AS `Status`
),
fixture_trx_payments AS (
    SELECT 1.00 AS `TrxPaymentId`, '2026-09-01 00:00:00' AS `CreationDate`, 'CASH' AS `PaymentMethodCod`, 'OK' AS `PaymentStatus`, 'PEN' AS `CurrencyCod`, 'I' AS `TypeMovement`, 120.00 AS `AmountPaid`, 2.00 AS `AmountReturned`, NULL AS `ReversalOfTrxPaymentId`, NULL AS `CashSessionID`, 'A' AS `Status`
    UNION ALL
    SELECT 2.00 AS `TrxPaymentId`, '2026-09-01 00:00:00' AS `CreationDate`, 'CASH' AS `PaymentMethodCod`, 'OK' AS `PaymentStatus`, 'PEN' AS `CurrencyCod`, 'E' AS `TypeMovement`, -59.00 AS `AmountPaid`, 0.00 AS `AmountReturned`, 1.00 AS `ReversalOfTrxPaymentId`, NULL AS `CashSessionID`, 'A' AS `Status`
    UNION ALL
    SELECT 3.00 AS `TrxPaymentId`, '2026-09-01 00:00:00' AS `CreationDate`, 'CASH' AS `PaymentMethodCod`, 'OK' AS `PaymentStatus`, 'USD' AS `CurrencyCod`, 'I' AS `TypeMovement`, 10.00 AS `AmountPaid`, 0.00 AS `AmountReturned`, NULL AS `ReversalOfTrxPaymentId`, 1.00 AS `CashSessionID`, 'A' AS `Status`
    UNION ALL
    SELECT 4.00 AS `TrxPaymentId`, '2026-09-01 00:00:00' AS `CreationDate`, 'CASH' AS `PaymentMethodCod`, 'PE' AS `PaymentStatus`, 'PEN' AS `CurrencyCod`, 'I' AS `TypeMovement`, 100.00 AS `AmountPaid`, 0.00 AS `AmountReturned`, NULL AS `ReversalOfTrxPaymentId`, 1.00 AS `CashSessionID`, 'A' AS `Status`
    UNION ALL
    SELECT 5.00 AS `TrxPaymentId`, '2026-09-01 00:00:00' AS `CreationDate`, 'CASH' AS `PaymentMethodCod`, 'OK' AS `PaymentStatus`, 'PEN' AS `CurrencyCod`, 'I' AS `TypeMovement`, 999.00 AS `AmountPaid`, 0.00 AS `AmountReturned`, NULL AS `ReversalOfTrxPaymentId`, 2.00 AS `CashSessionID`, 'A' AS `Status`
),
fixture_sale_document AS (
    SELECT 'B001-1' AS `DocumentCod`, 'B001' AS `CounterfoilCod`, '03' AS `DocumentType`, 'F' AS `DocumentRole`, 'S1' AS `SaleCod`, 'C1' AS `ClientCod`, '2026-09-01 00:00:00' AS `IssueDate`, '2026-09-01 00:00:00' AS `CreationDate`, 'A' AS `Status`
    UNION ALL
    SELECT 'P001-1' AS `DocumentCod`, 'P001' AS `CounterfoilCod`, '99' AS `DocumentType`, 'I' AS `DocumentRole`, 'S1' AS `SaleCod`, 'C1' AS `ClientCod`, '2026-09-01 00:00:00' AS `IssueDate`, '2026-09-01 00:00:00' AS `CreationDate`, 'A' AS `Status`
),
fixture_credit_note_head AS (
    SELECT 'N1' AS `CreditNoteCod`, 'S1' AS `SaleCod`, '2026-09-15 12:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'C' AS `CreditNoteStatus`, 'S' AS `IsPaid`, 'S' AS `IsStockReturned`, 'RETURN' AS `TypeCreditNote`, 'Devolucion' AS `Commenter`, 50.00 AS `NumTotalPriceNoTax`, 9.00 AS `NumTotalTax`, 59.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'N2' AS `CreditNoteCod`, 'S1' AS `SaleCod`, '2026-09-01 00:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'P' AS `CreditNoteStatus`, 'N' AS `IsPaid`, 'N' AS `IsStockReturned`, 'RETURN' AS `TypeCreditNote`, 'Pendiente' AS `Commenter`, 50.00 AS `NumTotalPriceNoTax`, 9.00 AS `NumTotalTax`, 59.00 AS `NumTotalPrice`, 'A' AS `Status`
),
fixture_credit_note_det AS (
    SELECT 'N1' AS `CreditNoteCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 1.00 AS `NumUnitStockReturned`, 50.00 AS `NumPriceSubTotal`, 59.00 AS `NumTotalPrice`, 'A' AS `Status`
    UNION ALL
    SELECT 'N2' AS `CreditNoteCod`, 1.00 AS `ItemNumber`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'NIU' AS `ProductUnitName`, 1.00 AS `ProductUnitFactor`, 1.00 AS `NumUnit`, 0.00 AS `NumUnitStockReturned`, 50.00 AS `NumPriceSubTotal`, 59.00 AS `NumTotalPrice`, 'A' AS `Status`
),
fixture_credit_note_document AS (
    SELECT 'N1' AS `CreditNoteCod`, 'BC01-1' AS `DocumentCod`, 'BC01' AS `CounterfoilCod`, '2026-09-15 12:00:00' AS `CreationDate`, 'A' AS `Status`
),
fixture_presale_head AS (
    SELECT 'O1' AS `PresaleCod`, '2026-09-01 00:00:00' AS `CreationDate`, 'T001' AS `StoreCod`, 'C1' AS `ClientCod`, 'PEN' AS `CurrencyCod`, 'C' AS `SaleStatus`, 'S' AS `IsPaid`, 236.00 AS `NumTotalPrice`, 'A' AS `Status`
),
fixture_presale_channel AS (
    SELECT 'O1' AS `PresaleCod`, 'WEB' AS `ChannelCod`, 'A' AS `Status`
),
fixture_sale_delivery AS (
    SELECT 'S7' AS `SaleCod`, 'P' AS `DeliveryStatus`, 'TRACK1' AS `TrackingNumber`, 'A' AS `Status`
),
fixture_pucharse_head AS (
    SELECT 'B1' AS PucharseCod, 'PEN' AS CurrencyCod, 'T001' AS StoreCod, 'D1' AS DealerCod,
           'FACT001' AS ExternalCod, '2026-08-01 00:00:00' AS CreationDate,
           'F' AS PurchaseStatus, 200.00 AS NumTotalPrice, 'A' AS Status
    UNION ALL SELECT 'B2', 'PEN', 'T001', 'D1', 'FACT002', '2026-09-10 12:00:00', 'F', 500.00, 'A'
    UNION ALL SELECT 'B3', 'PEN', 'T001', 'D1', 'FACT003', '2026-09-11 12:00:00', 'P', 900.00, 'A'
    UNION ALL SELECT 'B4', 'PEN', 'T002', 'D1', 'FACT004', '2026-09-12 12:00:00', 'F', 999.00, 'A'
    UNION ALL SELECT 'B5', 'USD', 'T001', 'D1', 'FACT005', '2026-09-13 12:00:00', 'F', 100.00, 'A'
),
fixture_currency AS (
    SELECT 'PEN' AS CurrencyCod, 'S' AS IsMonedaSystem
    UNION ALL SELECT 'USD', 'N'
),
fixture_stock_entry_head AS (
    SELECT 'E1' AS StockEntryCod, 'OPENING' AS ReasonCode
    UNION ALL SELECT 'E2', 'ADJUSTMENT'
),
fixture_stock_exit_head AS (
    SELECT 'X1' AS StockExitCod, 'ADJUSTMENT' AS ReasonCode
    UNION ALL SELECT 'X2', 'ADJUSTMENT'
),
fixture_transfer_head AS (
    SELECT 'TR1' AS TransferCod, 'T002' AS StoreCodOrigin, 'T001' AS StoreCodDest
    UNION ALL SELECT 'TR2', 'T001', 'T002'
),
fixture_product_traceability AS (
    SELECT 1.00 AS `ProductTraceabilityID`, 'LT1' AS `TechnicalLot`, NULL AS `OriginProductTraceabilityID`, 'B1' AS `OperationCod`, 1.00 AS `ItemNumber`, 'pucharse_head' AS `SourceTable`, 'S' AS `TypeOperation`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 10.00 AS `NumUnit`, 200.00 AS `NumTotalPriceCost`, 'A' AS `Status`, 0.00 AS `NumTotalPriceSale`, '2026-08-01 00:00:00' AS `OperationDate`
    UNION ALL
    SELECT 2.00 AS `ProductTraceabilityID`, 'LT1' AS `TechnicalLot`, 1.00 AS `OriginProductTraceabilityID`, 'S1' AS `OperationCod`, 1.00 AS `ItemNumber`, 'sale_head' AS `SourceTable`, 'R' AS `TypeOperation`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 1.00 AS `NumUnit`, 20.00 AS `NumTotalPriceCost`, 'A' AS `Status`, 59.00 AS `NumTotalPriceSale`, '2026-09-01 00:00:00' AS `OperationDate`
    UNION ALL
    SELECT 3.00 AS `ProductTraceabilityID`, 'LT1' AS `TechnicalLot`, 1.00 AS `OriginProductTraceabilityID`, 'S1' AS `OperationCod`, 1.00 AS `ItemNumber`, 'sale_head' AS `SourceTable`, 'R' AS `TypeOperation`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 1.00 AS `NumUnit`, 20.00 AS `NumTotalPriceCost`, 'A' AS `Status`, 59.00 AS `NumTotalPriceSale`, '2026-09-01 00:00:00' AS `OperationDate`
    UNION ALL
    SELECT 4.00 AS `ProductTraceabilityID`, 'LT1' AS `TechnicalLot`, 2.00 AS `OriginProductTraceabilityID`, 'N1' AS `OperationCod`, 1.00 AS `ItemNumber`, 'credit_note_head' AS `SourceTable`, 'S' AS `TypeOperation`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 1.00 AS `NumUnit`, 20.00 AS `NumTotalPriceCost`, 'A' AS `Status`, 59.00 AS `NumTotalPriceSale`, '2026-09-15 12:00:00' AS `OperationDate`
    UNION ALL
    SELECT 5.00 AS `ProductTraceabilityID`, 'LT1' AS `TechnicalLot`, 1.00 AS `OriginProductTraceabilityID`, 'S2' AS `OperationCod`, 1.00 AS `ItemNumber`, 'sale_head' AS `SourceTable`, 'R' AS `TypeOperation`, 'P1' AS `ProductCod`, '0000' AS `Variant`, 'T001' AS `StoreCod`, 1.00 AS `NumUnit`, 20.00 AS `NumTotalPriceCost`, 'A' AS `Status`, 118.00 AS `NumTotalPriceSale`, '2026-09-01 00:00:00' AS `OperationDate`
    UNION ALL SELECT 6, 'LT2', NULL, 'B2', 1, 'pucharse_head', 'S', 'P1', '0000', 'T001', 5, 200, 'A', 0, '2026-09-10 12:00:00'
    UNION ALL SELECT 7, 'LT3', NULL, 'B2', 2, 'pucharse_head', 'S', 'P2', '0000', 'T001', 5, 300, 'A', 0, '2026-09-10 12:00:03'
    UNION ALL SELECT 8, 'LT4', NULL, 'B5', 1, 'pucharse_head', 'S', 'P1', '0000', 'T001', 10, 100, 'A', 0, '2026-09-13 12:00:00'
    UNION ALL SELECT 9, 'LT1', 1, 'TR1', 1, 'transfer_head', 'R', 'P1', '0000', 'T002', 2, 40, 'A', 0, '2026-09-16 09:00:00'
    UNION ALL SELECT 10, 'LT1', 9, 'TR1', 1, 'transfer_head', 'S', 'P1', '0000', 'T001', 2, 40, 'A', 0, '2026-09-16 10:00:00'
    UNION ALL SELECT 11, 'LT1', 10, 'TR2', 1, 'transfer_head', 'R', 'P1', '0000', 'T001', 1, 20, 'A', 0, '2026-09-30 23:59:59'
    UNION ALL SELECT 12, 'LT1', 11, 'TR2', 1, 'transfer_head', 'S', 'P1', '0000', 'T002', 1, 20, 'A', 0, '2026-10-01 00:00:00'
    UNION ALL SELECT 13, 'LTE', NULL, 'E1', 1, 'stock_entry_head', 'S', 'P1', '0000', 'T001', 4, 440, 'A', 0, '2026-09-17 11:26:51'
    UNION ALL SELECT 14, 'LTE', 13, 'X1', 1, 'stock_exit_head', 'R', 'P1', '0000', 'T001', 4, 440, 'A', 0, '2026-09-17 11:28:26'
    UNION ALL SELECT 15, 'LTE', 13, 'X2', 1, 'stock_exit_head', 'R', 'P1', '0000', 'T001', 1, 110, 'I', 0, '2026-09-17 11:29:00'
    UNION ALL SELECT 16, 'LTE2', NULL, 'E2', 1, 'stock_entry_head', 'S', 'P1', '0000', 'T001', 1, 110, 'A', 0, '2026-10-01 00:00:00'
)
