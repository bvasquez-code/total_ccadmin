package com.ccadmin.app.bulkload.repository;

import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.id.BulkLoadDetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BulkLoadDetRepository extends JpaRepository<BulkLoadDetEntity, BulkLoadDetId> {
    @Query(value = """
        select count(1)
        from bulk_load_det
        where BulkLoadCod=:code and Status='A'
          and (:storeCod='' or StoreCod=:storeCod)
          and (:processStatus='' or ProcessStatus=:processStatus)
        """, nativeQuery = true)
    int countSearch(@Param("code") String code,
                    @Param("storeCod") String storeCod,
                    @Param("processStatus") String processStatus);

    @Query(value = """
        select *
        from bulk_load_det
        where BulkLoadCod=:code and Status='A'
          and (:storeCod='' or StoreCod=:storeCod)
          and (:processStatus='' or ProcessStatus=:processStatus)
        order by ItemNumber
        limit :init,:limit
        """, nativeQuery = true)
    List<BulkLoadDetEntity> search(@Param("code") String code,
                                   @Param("storeCod") String storeCod,
                                   @Param("processStatus") String processStatus,
                                   @Param("init") int init,
                                   @Param("limit") int limit);

    @Query(value = """
        select *
        from bulk_load_det
        where BulkLoadCod=:code and Status='A' and ProcessStatus='P'
        order by ItemNumber
        limit 20
        for update
        """, nativeQuery = true)
    List<BulkLoadDetEntity> findNextPendingForUpdate(@Param("code") String code);

    @Query(value = """
        select count(1)
        from bulk_load_det
        where BulkLoadCod=:code and Status='A' and ProcessStatus=:processStatus
        """, nativeQuery = true)
    int countByProcessStatus(@Param("code") String code,
                             @Param("processStatus") String processStatus);

    @Query(value = """
        select count(1)
        from bulk_load_det
        where BulkLoadCod=:code and StoreCod=:storeCod and Status='A'
          and ProcessStatus=:processStatus
        """, nativeQuery = true)
    int countByStoreAndProcessStatus(@Param("code") String code,
                                     @Param("storeCod") String storeCod,
                                     @Param("processStatus") String processStatus);

    @Modifying
    @Query(value = """
        update bulk_load_det
        set ProcessStatus='X', ModifyUser=:userCod, ModifyDate=now()
        where BulkLoadCod=:code and Status='A' and ProcessStatus='P'
        """, nativeQuery = true)
    void cancelPending(@Param("code") String code, @Param("userCod") String userCod);

    @Query(value = """
        select StoreCod
        from bulk_load_det
        where BulkLoadCod=:code and Status='A' and ProcessStatus='P'
        order by ItemNumber
        limit 1
        """, nativeQuery = true)
    String findNextPendingStore(@Param("code") String code);
}
