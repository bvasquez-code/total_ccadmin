package com.ccadmin.app.system.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CounterfoilRepository extends JpaRepository<CounterfoilEntity,String>, CcAdminRepository<CounterfoilEntity, String> {

    @Query( value = """
            select
            	c.*
            from counterfoil c
                inner join counterfoil_store cs on c.CounterfoilCod = cs.CounterfoilCod
            where
                c.DocumentType = :DocumentType
                and c.IsAutomatic = 'S'
                and cs.StoreCod = :StoreCod
                and cs.Status = 'A'
                and c.Status = 'A'
                limit 1
            """, nativeQuery = true)
    public CounterfoilEntity findByStoreDefault(
             @Param("DocumentType") String DocumentType
            ,@Param("StoreCod") String StoreCod
    );

    @Query( value = """
            select
            	c.*
            from counterfoil c
                inner join counterfoil_store cs on c.CounterfoilCod = cs.CounterfoilCod
            where
                c.DocumentType = :DocumentType
                and c.IsAutomatic = 'S'
                and cs.StoreCod = :StoreCod
                and c.GroupDocument = :GroupDocument
                and cs.Status = 'A'
                and c.Status = 'A'
            """, nativeQuery = true)
    public CounterfoilEntity findByStoreDefault(
             @Param("DocumentType") String DocumentType
            ,@Param("StoreCod") String StoreCod
            ,@Param("GroupDocument") String GroupDocument
    );


    @Query(value = """
        select c.*
        from counterfoil c
        where c.DocumentType = :docType and c.Series = :series
        limit 1
        """, nativeQuery = true)
    Optional<CounterfoilEntity> findByDocTypeSeries(@Param("docType") String docType,
                                                    @Param("series") String series);

    @Query(value = """
        select c.*
        from counterfoil c
        where c.Status = 'A'
        order by c.DocumentType, c.Series
        """, nativeQuery = true)
    List<CounterfoilEntity> findActives();

    // ====== Soporte SearchService (CcAdminRepository) ======
    @Override
    @Query(value = """
        select count(1)
        from counterfoil c
        where c.CounterfoilCod = :id
           or c.DocumentType like %:query%
           or c.Series like %:query%
        """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
        select c.*
        from counterfoil c
        where c.CounterfoilCod = :id
           or c.DocumentType like %:query%
           or c.Series like %:query%
        order by c.CreationDate desc
        limit :init, :limit
        """, nativeQuery = true)
    List<CounterfoilEntity> findByQueryText(@Param("id") String id,
                                            @Param("query") String query,
                                            @Param("init") int init,
                                            @Param("limit") int limit);


    @Override
    @Query(value = """
        select count(1)
        from counterfoil c
        inner join counterfoil_store cs on c.CounterfoilCod = cs.CounterfoilCod
        where cs.StoreCod = :storeCod
           and (c.CounterfoilCod = :id
           or c.DocumentType like %:query%
           or c.Series like %:query%)
        """, nativeQuery = true)
    int countByQueryTextStore(@Param("id") String id,
                              @Param("query") String query,
                              @Param("storeCod") String storeCod);

    @Override
    @Query(value = """
        select c.*
        from counterfoil c
        inner join counterfoil_store cs on c.CounterfoilCod = cs.CounterfoilCod
        where cs.StoreCod = :storeCod
           and (c.CounterfoilCod = :id
           or c.DocumentType like %:query%
           or c.Series like %:query%)
        order by c.CreationDate desc
        limit :init, :limit
        """, nativeQuery = true)
    List<CounterfoilEntity> findByQueryTextStore(@Param("id") String id,
                                                 @Param("query") String query,
                                                 @Param("storeCod") String storeCod,
                                                 @Param("init") int init,
                                                 @Param("limit") int limit);

    // ====== Reserva de correlativo (update + select) ======
    @Modifying
    @Query(value = """
        update counterfoil
        set Correlative = Correlative + 1, ModifyDate = now()
        where CounterfoilCod = :cod and Status = 'A'
        """, nativeQuery = true)
    int incCorrelative(@Param("cod") String counterfoilCod);

    @Query(value = """
        select c.Correlative
        from counterfoil c
        where c.CounterfoilCod = :cod
        """, nativeQuery = true)
    Integer getCorrelative(@Param("cod") String counterfoilCod);


    @Query(value = """
       select count(1) from counterfoil c
       where c.Series = :Series
       """, nativeQuery = true)
    int existBySeries(@Param("Series") String Series);
}
