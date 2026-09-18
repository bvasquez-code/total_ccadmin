package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.report.model.idto.IPaymentMethodsReportDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentMethodsReportRepository extends JpaRepository<SaleHeadEntity, String>, CcAdminRepository<IPaymentMethodsReportDto, String> {

    @Override
    @Query(value = """
            SELECT COUNT(*) FROM (
            WITH payment_store AS (
                SELECT sp.TrxPaymentId, MIN(sh.StoreCod) AS StoreCod
                FROM sale_payments sp JOIN sale_head sh ON sh.SaleCod = sp.SaleCod
                WHERE sp.Status = 'A' AND sh.Status = 'A'
                GROUP BY sp.TrxPaymentId HAVING COUNT(DISTINCT sh.StoreCod) = 1
            ), movements AS (
                SELECT t.TrxPaymentId, t.CreationDate, t.PaymentMethodCod,
                       t.PaymentStatus, t.CurrencyCod, t.TypeMovement,
                       COALESCE(t.AmountPaid, 0) - COALESCE(t.AmountReturned, 0) AS NetAmount,
                       COALESCE(ps.StoreCod, original_store.StoreCod, cs.StoreCod) AS StoreCod
                FROM trx_payments t
                LEFT JOIN payment_store ps ON ps.TrxPaymentId = t.TrxPaymentId
                LEFT JOIN payment_store original_store ON original_store.TrxPaymentId = t.ReversalOfTrxPaymentId
                LEFT JOIN cash_session cs ON cs.CashSessionID = t.CashSessionID
                WHERE t.Status = 'A'
            )
            SELECT m.StoreCod AS StoreCod, st.Name AS StoreName, m.CurrencyCod AS CurrencyCod,
                   m.PaymentMethodCod AS PaymentMethodCod,
                   COALESCE(pm.Name, m.PaymentMethodCod) AS PaymentMethodName,
                   m.PaymentStatus AS PaymentStatus, COUNT(*) AS PaymentCount,
                   SUM(CASE WHEN m.NetAmount >= 0 THEN m.NetAmount ELSE 0 END) AS Income,
                   SUM(CASE WHEN m.NetAmount < 0 THEN -m.NetAmount ELSE 0 END) AS Reversals,
                   SUM(m.NetAmount) AS Amount
            FROM movements m
            JOIN store st ON st.StoreCod = m.StoreCod
            LEFT JOIN payment_method pm ON pm.PaymentMethodCod = m.PaymentMethodCod
            WHERE (:#{#search.State} = '' OR m.PaymentStatus = :#{#search.State})
             AND m.CreationDate >= :#{#search.DateFrom} AND m.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR m.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = m.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR m.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', m.PaymentMethodCod, pm.Name) LIKE :#{#search.Query} ESCAPE '!' GROUP BY m.StoreCod, st.Name, m.CurrencyCod, m.PaymentMethodCod, pm.Name, m.PaymentStatus

            ) report_count
            """, nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = """
            WITH payment_store AS (
                SELECT sp.TrxPaymentId, MIN(sh.StoreCod) AS StoreCod
                FROM sale_payments sp JOIN sale_head sh ON sh.SaleCod = sp.SaleCod
                WHERE sp.Status = 'A' AND sh.Status = 'A'
                GROUP BY sp.TrxPaymentId HAVING COUNT(DISTINCT sh.StoreCod) = 1
            ), movements AS (
                SELECT t.TrxPaymentId, t.CreationDate, t.PaymentMethodCod,
                       t.PaymentStatus, t.CurrencyCod, t.TypeMovement,
                       COALESCE(t.AmountPaid, 0) - COALESCE(t.AmountReturned, 0) AS NetAmount,
                       COALESCE(ps.StoreCod, original_store.StoreCod, cs.StoreCod) AS StoreCod
                FROM trx_payments t
                LEFT JOIN payment_store ps ON ps.TrxPaymentId = t.TrxPaymentId
                LEFT JOIN payment_store original_store ON original_store.TrxPaymentId = t.ReversalOfTrxPaymentId
                LEFT JOIN cash_session cs ON cs.CashSessionID = t.CashSessionID
                WHERE t.Status = 'A'
            )
            SELECT m.StoreCod AS StoreCod, st.Name AS StoreName, m.CurrencyCod AS CurrencyCod,
                   m.PaymentMethodCod AS PaymentMethodCod,
                   COALESCE(pm.Name, m.PaymentMethodCod) AS PaymentMethodName,
                   m.PaymentStatus AS PaymentStatus, COUNT(*) AS PaymentCount,
                   SUM(CASE WHEN m.NetAmount >= 0 THEN m.NetAmount ELSE 0 END) AS Income,
                   SUM(CASE WHEN m.NetAmount < 0 THEN -m.NetAmount ELSE 0 END) AS Reversals,
                   SUM(m.NetAmount) AS Amount
            FROM movements m
            JOIN store st ON st.StoreCod = m.StoreCod
            LEFT JOIN payment_method pm ON pm.PaymentMethodCod = m.PaymentMethodCod
            WHERE (:#{#search.State} = '' OR m.PaymentStatus = :#{#search.State})
             AND m.CreationDate >= :#{#search.DateFrom} AND m.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR m.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = m.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR m.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', m.PaymentMethodCod, pm.Name) LIKE :#{#search.Query} ESCAPE '!' GROUP BY m.StoreCod, st.Name, m.CurrencyCod, m.PaymentMethodCod, pm.Name, m.PaymentStatus

            ORDER BY StoreCod, CurrencyCod, PaymentMethodCod, PaymentStatus
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<IPaymentMethodsReportDto> findByQueryText(@Param("search") SearchDto search);
}
