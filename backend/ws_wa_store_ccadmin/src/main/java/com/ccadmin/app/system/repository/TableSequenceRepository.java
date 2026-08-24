package com.ccadmin.app.system.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.system.model.entity.TableSequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TableSequenceRepository extends JpaRepository<TableSequenceEntity, String>,
        CcAdminRepository<TableSequenceEntity, String> {

    @Query(value = "call get_cod_seq(:type)", nativeQuery = true)
    String getNextCode(@Param("type") String sequenceTableType);

    @Query(value = """
            select distinct ts.SequenceTableType
            from table_sequence ts
            order by ts.SequenceTableType
            """, nativeQuery = true)
    List<String> findSequenceTableTypes();

    @Query(value = """
            select ts.*
            from table_sequence ts
            where ts.SequenceTableType = :SequenceTableType
            limit 1
            """, nativeQuery = true)
    TableSequenceEntity findBySequenceTableType(
            @Param("SequenceTableType") String sequenceTableType
    );

    @Override
    @Query(value = """
            select count(1)
            from table_sequence ts
            where ts.SequenceTrx = :id
               or ts.SequenceTableType like %:query%
               or ts.Prefix like %:query%
               or ts.UsePrefix = :query
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select ts.*
            from table_sequence ts
            where ts.SequenceTrx = :id
               or ts.SequenceTableType like %:query%
               or ts.Prefix like %:query%
               or ts.UsePrefix = :query
            order by ts.SequenceTableType, ts.SequenceTrx
            limit :init, :limit
            """, nativeQuery = true)
    List<TableSequenceEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
