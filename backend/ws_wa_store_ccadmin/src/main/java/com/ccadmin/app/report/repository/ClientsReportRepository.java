package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.report.model.idto.IClientsReportDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ClientsReportRepository extends JpaRepository<SaleHeadEntity, String>, CcAdminRepository<IClientsReportDto, String> {

    @Override
    @Query(value = """
            SELECT COUNT(*) FROM (
            SELECT s.ClientCod AS ClientCod,
            COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
             AS ClientName, p.DocumentNum AS DocumentNum,
                   s.StoreCod AS StoreCod, st.Name AS StoreName, s.CurrencyCod AS CurrencyCod,
                   COUNT(*) AS SaleCount, MIN(s.CreationDate) AS FirstSaleDate,
                   MAX(s.CreationDate) AS LastSaleDate, SUM(s.NumTotalPrice) AS Amount,
                   ROUND(AVG(s.NumTotalPrice), 2) AS AverageTicket
            FROM sale_head s
            JOIN store st ON st.StoreCod = s.StoreCod
             LEFT JOIN client c ON c.ClientCod = s.ClientCod
             LEFT JOIN person p ON p.PersonCod = c.PersonCod
            WHERE s.Status = 'A' AND s.SaleStatus = 'C'
              AND s.ClientCod IS NOT NULL AND s.ClientCod <> '' AND :#{#search.State} = ''
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', s.ClientCod, p.DocumentNum, COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
            ) LIKE :#{#search.Query} ESCAPE '!' GROUP BY s.ClientCod, p.BusinessName, p.Names, p.LastNames, p.DocumentNum,
                     s.StoreCod, st.Name, s.CurrencyCod

            ) report_count
            """, nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = """
            SELECT s.ClientCod AS ClientCod,
            COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
             AS ClientName, p.DocumentNum AS DocumentNum,
                   s.StoreCod AS StoreCod, st.Name AS StoreName, s.CurrencyCod AS CurrencyCod,
                   COUNT(*) AS SaleCount, MIN(s.CreationDate) AS FirstSaleDate,
                   MAX(s.CreationDate) AS LastSaleDate, SUM(s.NumTotalPrice) AS Amount,
                   ROUND(AVG(s.NumTotalPrice), 2) AS AverageTicket
            FROM sale_head s
            JOIN store st ON st.StoreCod = s.StoreCod
             LEFT JOIN client c ON c.ClientCod = s.ClientCod
             LEFT JOIN person p ON p.PersonCod = c.PersonCod
            WHERE s.Status = 'A' AND s.SaleStatus = 'C'
              AND s.ClientCod IS NOT NULL AND s.ClientCod <> '' AND :#{#search.State} = ''
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', s.ClientCod, p.DocumentNum, COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
            ) LIKE :#{#search.Query} ESCAPE '!' GROUP BY s.ClientCod, p.BusinessName, p.Names, p.LastNames, p.DocumentNum,
                     s.StoreCod, st.Name, s.CurrencyCod

            ORDER BY Amount DESC, StoreCod, CurrencyCod, ClientCod
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<IClientsReportDto> findByQueryText(@Param("search") SearchDto search);
}
