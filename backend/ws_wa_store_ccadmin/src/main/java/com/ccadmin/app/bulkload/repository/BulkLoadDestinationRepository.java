package com.ccadmin.app.bulkload.repository;

import com.ccadmin.app.bulkload.model.entity.BulkLoadDestinationEntity;
import com.ccadmin.app.bulkload.model.entity.id.BulkLoadDestinationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BulkLoadDestinationRepository
        extends JpaRepository<BulkLoadDestinationEntity, BulkLoadDestinationId> {

    @Query(value = """
        select *
        from bulk_load_destination
        where BulkLoadCod=:code and Status='A'
        order by StoreCod
        """, nativeQuery = true)
    List<BulkLoadDestinationEntity> findByCode(@Param("code") String code);
}
