package com.ccadmin.app.producttraceability.repository;

import com.ccadmin.app.producttraceability.model.entity.ProductTraceabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductTraceabilityRepository
        extends JpaRepository<ProductTraceabilityEntity, Long> {

    @Query(value = """
            select count(1)
            from product_traceability
            where KardexID = :kardexId
            """, nativeQuery = true)
    int countByKardexId(@Param("kardexId") long kardexId);

    @Query(value = """
            select pt.*
            from product_traceability pt
            where pt.StoreCod = :storeCode
              and pt.WarehouseCod = :warehouseCode
              and pt.ProductCod = :productCode
              and pt.Variant = :variant
              and pt.TypeOperation = 'S'
              and pt.AvailabilityStatus = 'A'
              and pt.NumUnitAvailable > 0
              and (:lotNumber is null or pt.LotNumber = :lotNumber)
              and (:expirationDate is null or pt.ExpirationDate = :expirationDate)
            order by pt.ProductTraceabilityID
            for update
            """, nativeQuery = true)
    List<ProductTraceabilityEntity> findAvailableForUpdate(
            @Param("storeCode") String storeCode,
            @Param("warehouseCode") String warehouseCode,
            @Param("productCode") String productCode,
            @Param("variant") String variant,
            @Param("lotNumber") String lotNumber,
            @Param("expirationDate") Date expirationDate
    );

    @Query(value = """
            select pt.*
            from product_traceability pt
            where pt.SourceTable = :sourceTable
              and pt.OperationCod = :operationCode
              and pt.ItemNumber <=> :itemNumber
              and pt.ProductCod = :productCode
              and pt.Variant = :variant
              and pt.TypeOperation = 'R'
            order by pt.ProductTraceabilityID
            for update
            """, nativeQuery = true)
    List<ProductTraceabilityEntity> findOutboundAllocationsForUpdate(
            @Param("sourceTable") String sourceTable,
            @Param("operationCode") String operationCode,
            @Param("itemNumber") Integer itemNumber,
            @Param("productCode") String productCode,
            @Param("variant") String variant
    );

    @Query(value = """
            select pt.*
            from product_traceability pt
            where pt.SourceTable = :sourceTable
              and pt.OperationCod = :operationCode
              and pt.ItemNumber <=> :itemNumber
              and pt.StoreCod = :storeCode
              and pt.WarehouseCod = :warehouseCode
              and pt.ProductCod = :productCode
              and pt.Variant = :variant
              and pt.TypeOperation = 'S'
              and pt.AvailabilityStatus = 'A'
              and pt.NumUnitAvailable > 0
            order by pt.ProductTraceabilityID
            for update
            """, nativeQuery = true)
    List<ProductTraceabilityEntity> findAvailableFromOperationForUpdate(
            @Param("sourceTable") String sourceTable,
            @Param("operationCode") String operationCode,
            @Param("itemNumber") Integer itemNumber,
            @Param("storeCode") String storeCode,
            @Param("warehouseCode") String warehouseCode,
            @Param("productCode") String productCode,
            @Param("variant") String variant
    );

    @Query(value = """
            select coalesce(sum(pt.NumUnit), 0)
            from product_traceability pt
            where pt.OriginProductTraceabilityID = :originId
              and pt.TypeOperation = 'S'
              and pt.Status = 'A'
            """, nativeQuery = true)
    long sumInboundQuantityByOrigin(@Param("originId") long originId);

    @Query(value = """
            select pt.*
            from product_traceability pt
            where pt.ProductCod = :productCode
              and pt.Variant = :variant
              and pt.StoreCod = :storeCode
              and pt.WarehouseCod = :warehouseCode
              and pt.TypeOperation = 'R'
              and pt.Status = 'A'
            order by pt.ProductTraceabilityID desc
            limit 1
            """, nativeQuery = true)
    Optional<ProductTraceabilityEntity> findLastOutbound(
            @Param("productCode") String productCode,
            @Param("variant") String variant,
            @Param("storeCode") String storeCode,
            @Param("warehouseCode") String warehouseCode
    );

    @Query(value = """
            select pt.*
            from product_traceability pt
            where pt.ProductCod = :productCode
              and pt.Variant = :variant
              and pt.StoreCod = :storeCode
              and pt.SourceTable = 'pucharse_head'
              and pt.TypeOperation = 'S'
              and pt.Status = 'A'
            order by pt.ProductTraceabilityID desc
            limit 1
            """, nativeQuery = true)
    Optional<ProductTraceabilityEntity> findLastPurchaseInbound(
            @Param("productCode") String productCode,
            @Param("variant") String variant,
            @Param("storeCode") String storeCode
    );
}
