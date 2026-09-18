package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.report.model.idto.ICreditNotesReportDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CreditNotesReportRepository extends JpaRepository<CreditNoteHeadEntity, String>, CcAdminRepository<ICreditNotesReportDto, String> {

    @Override
    @Query(value = """
            SELECT COUNT(*) FROM (
            SELECT s.CreditNoteCod AS CreditNoteCod, s.SaleCod AS SaleCod,
                   s.CreationDate AS ReportDate, s.StoreCod AS StoreCod, st.Name AS StoreName,
                   s.ClientCod AS ClientCod,
            COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
             AS ClientName, s.CurrencyCod AS CurrencyCod,
                   s.TypeCreditNote AS TypeCreditNote, s.CreditNoteStatus AS CreditNoteStatus,
                   s.NumTotalPriceNoTax AS AmountNoTax, s.NumTotalTax AS Tax, s.NumTotalPrice AS Amount,
                   s.IsPaid AS IsPaid, s.IsStockReturned AS IsStockReturned,
                   s.Commenter AS Reason, COALESCE(detail.Quantity, 0) AS Quantity,
                   COALESCE(detail.ReturnedQuantity, 0) AS ReturnedQuantity
            FROM credit_note_head s
            JOIN store st ON st.StoreCod = s.StoreCod
             LEFT JOIN client c ON c.ClientCod = s.ClientCod
             LEFT JOIN person p ON p.PersonCod = c.PersonCod
            LEFT JOIN (
                SELECT CreditNoteCod, SUM(NumUnit) AS Quantity,
                       SUM(COALESCE(NumUnitStockReturned, 0)) AS ReturnedQuantity
                FROM credit_note_det WHERE Status = 'A' GROUP BY CreditNoteCod
            ) detail ON detail.CreditNoteCod = s.CreditNoteCod
            WHERE s.Status = 'A' AND (:#{#search.State} = '' OR s.CreditNoteStatus = :#{#search.State})
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', s.CreditNoteCod, s.SaleCod, s.ClientCod, s.Commenter, COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
            ) LIKE :#{#search.Query} ESCAPE '!'
            ) report_count
            """, nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = """
            SELECT s.CreditNoteCod AS CreditNoteCod, s.SaleCod AS SaleCod,
                   s.CreationDate AS ReportDate, s.StoreCod AS StoreCod, st.Name AS StoreName,
                   s.ClientCod AS ClientCod,
            COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
             AS ClientName, s.CurrencyCod AS CurrencyCod,
                   s.TypeCreditNote AS TypeCreditNote, s.CreditNoteStatus AS CreditNoteStatus,
                   s.NumTotalPriceNoTax AS AmountNoTax, s.NumTotalTax AS Tax, s.NumTotalPrice AS Amount,
                   s.IsPaid AS IsPaid, s.IsStockReturned AS IsStockReturned,
                   s.Commenter AS Reason, COALESCE(detail.Quantity, 0) AS Quantity,
                   COALESCE(detail.ReturnedQuantity, 0) AS ReturnedQuantity
            FROM credit_note_head s
            JOIN store st ON st.StoreCod = s.StoreCod
             LEFT JOIN client c ON c.ClientCod = s.ClientCod
             LEFT JOIN person p ON p.PersonCod = c.PersonCod
            LEFT JOIN (
                SELECT CreditNoteCod, SUM(NumUnit) AS Quantity,
                       SUM(COALESCE(NumUnitStockReturned, 0)) AS ReturnedQuantity
                FROM credit_note_det WHERE Status = 'A' GROUP BY CreditNoteCod
            ) detail ON detail.CreditNoteCod = s.CreditNoteCod
            WHERE s.Status = 'A' AND (:#{#search.State} = '' OR s.CreditNoteStatus = :#{#search.State})
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', s.CreditNoteCod, s.SaleCod, s.ClientCod, s.Commenter, COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
            ) LIKE :#{#search.Query} ESCAPE '!'
            ORDER BY ReportDate DESC, CreditNoteCod DESC
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<ICreditNotesReportDto> findByQueryText(@Param("search") SearchDto search);
}
