package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.report.model.idto.IDocumentsReportDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentsReportRepository extends JpaRepository<SaleHeadEntity, String>, CcAdminRepository<IDocumentsReportDto, String> {

    @Override
    @Query(value = """
            SELECT COUNT(*) FROM (
            SELECT r.* FROM (
                SELECT 'SALE' AS SourceType, d.DocumentCod AS DocumentCod,
                       d.CounterfoilCod AS CounterfoilCod, d.DocumentType AS DocumentType,
                       d.DocumentRole AS DocumentRole, s.SaleCod AS OperationCod,
                       COALESCE(d.IssueDate, d.CreationDate) AS ReportDate,
                       s.StoreCod AS StoreCod, st.Name AS StoreName,
                       COALESCE(d.ClientCod, s.ClientCod) AS ClientCod,
                       s.CurrencyCod AS CurrencyCod, s.NumTotalPrice AS Amount,
                       s.SaleStatus AS DocumentStatus
                FROM sale_document d JOIN sale_head s ON s.SaleCod = d.SaleCod
                JOIN store st ON st.StoreCod = s.StoreCod
                WHERE d.Status = 'A' AND s.Status = 'A'
             AND COALESCE(d.IssueDate, d.CreationDate) >= :#{#search.DateFrom} AND COALESCE(d.IssueDate, d.CreationDate) < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})     UNION ALL
                SELECT 'CREDIT_NOTE', d.DocumentCod, d.CounterfoilCod, '07', 'F',
                       s.CreditNoteCod, d.CreationDate, s.StoreCod, st.Name, s.ClientCod,
                       s.CurrencyCod, -s.NumTotalPrice, s.CreditNoteStatus
                FROM credit_note_document d JOIN credit_note_head s ON s.CreditNoteCod = d.CreditNoteCod
                JOIN store st ON st.StoreCod = s.StoreCod
                WHERE d.Status = 'A' AND s.Status = 'A'
             AND d.CreationDate >= :#{#search.DateFrom} AND d.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod}) ) r
            WHERE (:#{#search.State} = '' OR r.DocumentType = :#{#search.State})

              AND CONCAT_WS(' ', r.DocumentCod, r.CounterfoilCod, r.OperationCod, r.ClientCod) LIKE :#{#search.Query} ESCAPE '!'
            ) report_count
            """, nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = """
            SELECT r.* FROM (
                SELECT 'SALE' AS SourceType, d.DocumentCod AS DocumentCod,
                       d.CounterfoilCod AS CounterfoilCod, d.DocumentType AS DocumentType,
                       d.DocumentRole AS DocumentRole, s.SaleCod AS OperationCod,
                       COALESCE(d.IssueDate, d.CreationDate) AS ReportDate,
                       s.StoreCod AS StoreCod, st.Name AS StoreName,
                       COALESCE(d.ClientCod, s.ClientCod) AS ClientCod,
                       s.CurrencyCod AS CurrencyCod, s.NumTotalPrice AS Amount,
                       s.SaleStatus AS DocumentStatus
                FROM sale_document d JOIN sale_head s ON s.SaleCod = d.SaleCod
                JOIN store st ON st.StoreCod = s.StoreCod
                WHERE d.Status = 'A' AND s.Status = 'A'
             AND COALESCE(d.IssueDate, d.CreationDate) >= :#{#search.DateFrom} AND COALESCE(d.IssueDate, d.CreationDate) < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})     UNION ALL
                SELECT 'CREDIT_NOTE', d.DocumentCod, d.CounterfoilCod, '07', 'F',
                       s.CreditNoteCod, d.CreationDate, s.StoreCod, st.Name, s.ClientCod,
                       s.CurrencyCod, -s.NumTotalPrice, s.CreditNoteStatus
                FROM credit_note_document d JOIN credit_note_head s ON s.CreditNoteCod = d.CreditNoteCod
                JOIN store st ON st.StoreCod = s.StoreCod
                WHERE d.Status = 'A' AND s.Status = 'A'
             AND d.CreationDate >= :#{#search.DateFrom} AND d.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod}) ) r
            WHERE (:#{#search.State} = '' OR r.DocumentType = :#{#search.State})

              AND CONCAT_WS(' ', r.DocumentCod, r.CounterfoilCod, r.OperationCod, r.ClientCod) LIKE :#{#search.Query} ESCAPE '!'
            ORDER BY ReportDate DESC, SourceType, OperationCod, DocumentCod, CounterfoilCod
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<IDocumentsReportDto> findByQueryText(@Param("search") SearchDto search);
}
