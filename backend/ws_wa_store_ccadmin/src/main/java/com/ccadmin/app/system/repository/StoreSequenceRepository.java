package com.ccadmin.app.system.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.system.model.entity.StoreSequenceEntity;
import com.ccadmin.app.system.model.entity.id.StoreSequenceID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoreSequenceRepository extends JpaRepository<StoreSequenceEntity, StoreSequenceID>,
        CcAdminRepository<StoreSequenceEntity, StoreSequenceID> {

    @Query(value = """
            select distinct ss.SequenceTableType
            from store_sequence ss
            order by ss.SequenceTableType
            """, nativeQuery = true)
    List<String> findSequenceTableTypes();

    @Override
    @Query(value = """
            select count(1)
            from store_sequence ss
            where ss.StoreCod = :id
               or ss.SequenceTableType like %:query%
               or ss.Prefix like %:query%
               or cast(ss.PeriodId as char) = :query
               or cast(ss.SequenceTrx as char) = :query
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select ss.*
            from store_sequence ss
            where ss.StoreCod = :id
               or ss.SequenceTableType like %:query%
               or ss.Prefix like %:query%
               or cast(ss.PeriodId as char) = :query
               or cast(ss.SequenceTrx as char) = :query
            order by ss.StoreCod, ss.SequenceTableType, ss.PeriodId
            limit :init, :limit
            """, nativeQuery = true)
    List<StoreSequenceEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Override
    @Query(value = """
            select count(1)
            from store_sequence ss
            where ss.StoreCod = :storeCod
              and (
                   ss.StoreCod = :id
                or ss.SequenceTableType like %:query%
                or ss.Prefix like %:query%
                or cast(ss.PeriodId as char) = :query
                or cast(ss.SequenceTrx as char) = :query
              )
            """, nativeQuery = true)
    int countByQueryTextStore(@Param("id") String id, @Param("query") String query, @Param("storeCod") String storeCod);

    @Override
    @Query(value = """
            select ss.*
            from store_sequence ss
            where ss.StoreCod = :storeCod
              and (
                   ss.StoreCod = :id
                or ss.SequenceTableType like %:query%
                or ss.Prefix like %:query%
                or cast(ss.PeriodId as char) = :query
                or cast(ss.SequenceTrx as char) = :query
              )
            order by ss.StoreCod, ss.SequenceTableType, ss.PeriodId
            limit :init, :limit
            """, nativeQuery = true)
    List<StoreSequenceEntity> findByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
