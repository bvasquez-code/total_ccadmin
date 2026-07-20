package com.ccadmin.app.product.repository;

import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Date;

@Repository
public interface KardexZoneRepository extends JpaRepository<KardexZoneEntity, Long> {

    @Query(value = """
            select count(1)
            from kardex_zone kz
            where kz.SourceTable = 'no_table'
              and kz.MovementEvent in ('INITIALIZATION', 'BALANCE_REGULARIZATION')
              and kz.ProductCod = :ProductCod
              and kz.Variant = :Variant
              and kz.StoreCod = :StoreCod
              and kz.WarehouseCod = :WarehouseCod
              and kz.ZoneStockMoved = 'UNAVAILABLE'
              and kz.TypeOperation = 'S'
              and kz.NumZoneStockAfter >= :RequiredStock
              and kz.CreationDate >= :OperationCreationDate
            """, nativeQuery = true)
    int countLegacyUnavailableBaseline(
            @Param("ProductCod") String productCod,
            @Param("Variant") String variant,
            @Param("StoreCod") String storeCod,
            @Param("WarehouseCod") String warehouseCod,
            @Param("RequiredStock") int requiredStock,
            @Param("OperationCreationDate") Date operationCreationDate
    );

    @Query(value = """
            select count(1)
            from kardex_zone kz
            where kz.StoreCod = :StoreCod
              and kz.Status = 'A'
              and (:ZoneStockMoved = '' or kz.ZoneStockMoved = :ZoneStockMoved)
              and (:TypeOperation = '' or kz.TypeOperation = :TypeOperation)
              and (
                    :Query = ''
                    or concat_ws(' ',
                        kz.OperationCod,
                        kz.ProductCod,
                        kz.Variant,
                        kz.WarehouseCod,
                        kz.SourceTable,
                        kz.MovementEvent,
                        kz.LotNumber
                    ) like concat('%', :Query, '%')
              )
            """, nativeQuery = true)
    int countSearch(
            @Param("Query") String query,
            @Param("StoreCod") String storeCod,
            @Param("ZoneStockMoved") String zoneStockMoved,
            @Param("TypeOperation") String typeOperation
    );

    @Query(value = """
            select kz.*
            from kardex_zone kz
            where kz.StoreCod = :StoreCod
              and kz.Status = 'A'
              and (:ZoneStockMoved = '' or kz.ZoneStockMoved = :ZoneStockMoved)
              and (:TypeOperation = '' or kz.TypeOperation = :TypeOperation)
              and (
                    :Query = ''
                    or concat_ws(' ',
                        kz.OperationCod,
                        kz.ProductCod,
                        kz.Variant,
                        kz.WarehouseCod,
                        kz.SourceTable,
                        kz.MovementEvent,
                        kz.LotNumber
                    ) like concat('%', :Query, '%')
              )
            order by kz.KardexZoneID desc
            limit :Init, :Limit
            """, nativeQuery = true)
    List<KardexZoneEntity> search(
            @Param("Query") String query,
            @Param("StoreCod") String storeCod,
            @Param("ZoneStockMoved") String zoneStockMoved,
            @Param("TypeOperation") String typeOperation,
            @Param("Init") int init,
            @Param("Limit") int limit
    );

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

    @Query(value = """
            select * from kardex_zone
            where SourceTable = :SourceTable
              and OperationCod = :OperationCod
              and ItemNumber = :ItemNumber
              and MovementEvent = :MovementEvent
            order by KardexZoneID
            """, nativeQuery = true)
    List<KardexZoneEntity> findByEvent(
            @Param("SourceTable") String sourceTable,
            @Param("OperationCod") String operationCod,
            @Param("ItemNumber") int itemNumber,
            @Param("MovementEvent") String movementEvent
    );
}
