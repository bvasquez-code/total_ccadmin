package com.ccadmin.app.system.repository;

import com.ccadmin.app.system.model.entity.CounterfoilStoreEntity;
import com.ccadmin.app.system.model.entity.id.CounterfoilStoreID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CounterfoilStoreRepository extends JpaRepository<CounterfoilStoreEntity, CounterfoilStoreID> {

    @Query(value = """
        select cs.*
        from counterfoil_store cs
        where cs.CounterfoilCod = :cod
          and cs.Status = 'A'
        order by cs.StoreCod
        """, nativeQuery = true)
    List<CounterfoilStoreEntity> findStoresByCounterfoil(@Param("cod") String counterfoilCod);

    @Query(value = """
        select cs.*
        from counterfoil_store cs
        where cs.StoreCod = :storeCod
          and cs.Status = 'A'
        order by cs.CounterfoilCod
        """, nativeQuery = true)
    List<CounterfoilStoreEntity> findCounterfoilsByStore(@Param("storeCod") String storeCod);

    @Modifying
    @Query(value = """
        update counterfoil_store
        set Status = 'I', ModifyDate = now()
        where CounterfoilCod = :cod and StoreCod = :storeCod and Status = 'A'
        """, nativeQuery = true)
    int softDelete(@Param("cod") String counterfoilCod, @Param("storeCod") String storeCod);
}