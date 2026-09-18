package com.ccadmin.app.report.repository;

import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.report.model.idto.IOrdersReportDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrdersReportRepository extends JpaRepository<PresaleHeadEntity, String>, CcAdminRepository<IOrdersReportDto, String> {

    @Override
    @Query(value = """
            SELECT COUNT(*) FROM (
            SELECT s.PresaleCod AS PresaleCod, s.CreationDate AS ReportDate,
                   s.StoreCod AS StoreCod, st.Name AS StoreName, s.ClientCod AS ClientCod,
            COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
             AS ClientName, s.CurrencyCod AS CurrencyCod,
                   s.SaleStatus AS OrderStatus, s.IsPaid AS IsPaid, s.NumTotalPrice AS Amount,
                   COALESCE(pc.ChannelCod, 'IN_PERSON') AS ChannelCod,
                   sale.SaleCod AS SaleCod, sale.SaleStatus AS SaleStatus,
                   delivery.DeliveryStatus AS DeliveryStatus, delivery.TrackingNumber AS TrackingNumber
            FROM presale_head s
            JOIN store st ON st.StoreCod = s.StoreCod
             LEFT JOIN client c ON c.ClientCod = s.ClientCod
             LEFT JOIN person p ON p.PersonCod = c.PersonCod
            LEFT JOIN presale_channel pc ON pc.PresaleCod = s.PresaleCod AND pc.Status = 'A'
            LEFT JOIN sale_head sale ON sale.SaleCod = (
                SELECT latest.SaleCod FROM sale_head latest
                WHERE latest.PresaleCod = s.PresaleCod AND latest.Status = 'A'
                ORDER BY latest.CreationDate DESC, latest.SaleCod DESC LIMIT 1
            )
            LEFT JOIN sale_delivery delivery ON delivery.SaleCod = sale.SaleCod AND delivery.Status = 'A'
            WHERE s.Status = 'A' AND (:#{#search.State} = '' OR s.SaleStatus = :#{#search.State})
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', s.PresaleCod, s.ClientCod, sale.SaleCod, delivery.TrackingNumber, COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
            ) LIKE :#{#search.Query} ESCAPE '!'
            ) report_count
            """, nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = """
            SELECT s.PresaleCod AS PresaleCod, s.CreationDate AS ReportDate,
                   s.StoreCod AS StoreCod, st.Name AS StoreName, s.ClientCod AS ClientCod,
            COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
             AS ClientName, s.CurrencyCod AS CurrencyCod,
                   s.SaleStatus AS OrderStatus, s.IsPaid AS IsPaid, s.NumTotalPrice AS Amount,
                   COALESCE(pc.ChannelCod, 'IN_PERSON') AS ChannelCod,
                   sale.SaleCod AS SaleCod, sale.SaleStatus AS SaleStatus,
                   delivery.DeliveryStatus AS DeliveryStatus, delivery.TrackingNumber AS TrackingNumber
            FROM presale_head s
            JOIN store st ON st.StoreCod = s.StoreCod
             LEFT JOIN client c ON c.ClientCod = s.ClientCod
             LEFT JOIN person p ON p.PersonCod = c.PersonCod
            LEFT JOIN presale_channel pc ON pc.PresaleCod = s.PresaleCod AND pc.Status = 'A'
            LEFT JOIN sale_head sale ON sale.SaleCod = (
                SELECT latest.SaleCod FROM sale_head latest
                WHERE latest.PresaleCod = s.PresaleCod AND latest.Status = 'A'
                ORDER BY latest.CreationDate DESC, latest.SaleCod DESC LIMIT 1
            )
            LEFT JOIN sale_delivery delivery ON delivery.SaleCod = sale.SaleCod AND delivery.Status = 'A'
            WHERE s.Status = 'A' AND (:#{#search.State} = '' OR s.SaleStatus = :#{#search.State})
             AND s.CreationDate >= :#{#search.DateFrom} AND s.CreationDate < :#{#search.DateTo}
              AND (:#{#search.StoreCod} = '' OR s.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = s.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.CurrencyCod} = '' OR s.CurrencyCod = :#{#search.CurrencyCod})
              AND CONCAT_WS(' ', s.PresaleCod, s.ClientCod, sale.SaleCod, delivery.TrackingNumber, COALESCE(NULLIF(p.BusinessName, ''), NULLIF(TRIM(CONCAT_WS(' ', p.Names, p.LastNames)), ''),
                     s.ClientCod, 'Sin cliente')
            ) LIKE :#{#search.Query} ESCAPE '!'
            ORDER BY ReportDate DESC, PresaleCod DESC
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<IOrdersReportDto> findByQueryText(@Param("search") SearchDto search);
}
