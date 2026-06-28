package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.StockZoneMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockZoneMovementRepository extends JpaRepository<StockZoneMovementEntity, Long> {

    @Query(value = """
            select count(1) from stock_zone_movement
            where SourceTable = :SourceTable
            and OperationCod = :OperationCod
            and SourceZone = :SourceZone
            and TargetZone = :TargetZone
            """, nativeQuery = true)
    int countBySource(
            @Param("SourceTable") String sourceTable,
            @Param("OperationCod") String operationCod,
            @Param("SourceZone") String sourceZone,
            @Param("TargetZone") String targetZone
    );
}
