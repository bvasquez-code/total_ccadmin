package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KardexZoneRepository extends JpaRepository<KardexZoneEntity, Long> {

    @Query(value = """
            select count(1) from kardex_zone
            where SourceTable = :SourceTable
              and OperationCod = :OperationCod
              and ItemNumber = :ItemNumber
              and MovementEvent = :MovementEvent
            """, nativeQuery = true)
    int countByEvent(
            @Param("SourceTable") String sourceTable,
            @Param("OperationCod") String operationCod,
            @Param("ItemNumber") int itemNumber,
            @Param("MovementEvent") String movementEvent
    );
}
