package com.ccadmin.app.report.repository;

import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.report.model.idto.IStockReportDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StockReportRepository extends JpaRepository<ProductInfoEntity, ProductInfoId>, CcAdminRepository<IStockReportDto, ProductInfoId> {

    @Override
    @Query(value = """
            SELECT COUNT(*) FROM (
            SELECT i.ProductCod AS ProductCod, pr.ProductName AS ProductName,
                   i.Variant AS Variant, i.StoreCod AS StoreCod, st.Name AS StoreName,
                   COALESCE(pc.ProductUnitName, 'NIU') AS ProductUnitName,
                   COALESCE(pc.ProductUnitFactor, 1) AS ProductUnitFactor,
                   i.NumPhysicalStock AS PhysicalStock, i.NumReservedStock AS ReservedStock,
                   i.NumUnavailableStock AS UnavailableStock, i.NumTotalStock AS TotalStock,
                   COALESCE(pc.NumMinStock, 0) AS MinStock,
                   CASE WHEN i.NumPhysicalStock = 0 THEN 'EMPTY'
                        WHEN pc.NumMinStock IS NOT NULL AND i.NumPhysicalStock <= pc.NumMinStock THEN 'LOW'
                        ELSE 'AVAILABLE' END AS StockStatus
            FROM product_info i
            JOIN store st ON st.StoreCod = i.StoreCod
            LEFT JOIN product pr ON pr.ProductCod = i.ProductCod
            LEFT JOIN product_config pc ON pc.ProductCod = i.ProductCod AND pc.StoreCod = i.StoreCod
                                       AND pc.Status = 'A'
            WHERE i.Status = 'A'
              AND (:#{#search.StoreCod} = '' OR i.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = i.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.State} = ''
                OR (:#{#search.State} = 'EMPTY' AND i.NumPhysicalStock = 0)
                OR (:#{#search.State} = 'LOW' AND pc.NumMinStock IS NOT NULL AND i.NumPhysicalStock <= pc.NumMinStock)
                OR (:#{#search.State} = 'AVAILABLE' AND i.NumPhysicalStock > 0))

              AND CONCAT_WS(' ', i.ProductCod, pr.ProductName, i.Variant) LIKE :#{#search.Query} ESCAPE '!'
            ) report_count
            """, nativeQuery = true)
    int countByQueryText(@Param("search") SearchDto search);

    @Override
    @Query(value = """
            SELECT i.ProductCod AS ProductCod, pr.ProductName AS ProductName,
                   i.Variant AS Variant, i.StoreCod AS StoreCod, st.Name AS StoreName,
                   COALESCE(pc.ProductUnitName, 'NIU') AS ProductUnitName,
                   COALESCE(pc.ProductUnitFactor, 1) AS ProductUnitFactor,
                   i.NumPhysicalStock AS PhysicalStock, i.NumReservedStock AS ReservedStock,
                   i.NumUnavailableStock AS UnavailableStock, i.NumTotalStock AS TotalStock,
                   COALESCE(pc.NumMinStock, 0) AS MinStock,
                   CASE WHEN i.NumPhysicalStock = 0 THEN 'EMPTY'
                        WHEN pc.NumMinStock IS NOT NULL AND i.NumPhysicalStock <= pc.NumMinStock THEN 'LOW'
                        ELSE 'AVAILABLE' END AS StockStatus
            FROM product_info i
            JOIN store st ON st.StoreCod = i.StoreCod
            LEFT JOIN product pr ON pr.ProductCod = i.ProductCod
            LEFT JOIN product_config pc ON pc.ProductCod = i.ProductCod AND pc.StoreCod = i.StoreCod
                                       AND pc.Status = 'A'
            WHERE i.Status = 'A'
              AND (:#{#search.StoreCod} = '' OR i.StoreCod = :#{#search.StoreCod})
              AND EXISTS (SELECT 1 FROM user_store report_user_store
                          WHERE report_user_store.StoreCod = i.StoreCod
                            AND report_user_store.UserCod = :#{#search.UserCod} AND report_user_store.Status = 'A')
              AND (:#{#search.State} = ''
                OR (:#{#search.State} = 'EMPTY' AND i.NumPhysicalStock = 0)
                OR (:#{#search.State} = 'LOW' AND pc.NumMinStock IS NOT NULL AND i.NumPhysicalStock <= pc.NumMinStock)
                OR (:#{#search.State} = 'AVAILABLE' AND i.NumPhysicalStock > 0))

              AND CONCAT_WS(' ', i.ProductCod, pr.ProductName, i.Variant) LIKE :#{#search.Query} ESCAPE '!'
            ORDER BY StoreCod, ProductCod, Variant
            LIMIT :#{#search.Init}, :#{#search.Limit}
            """, nativeQuery = true)
    List<IStockReportDto> findByQueryText(@Param("search") SearchDto search);
}
