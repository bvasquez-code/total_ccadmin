package com.ccadmin.app.bulkload.repository;

import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BulkLoadHeadRepository extends JpaRepository<BulkLoadHeadEntity, String> {
    @Query(value = "CALL get_cod_seq('bulk_load_head')", nativeQuery = true)
    String createCode();

    @Query(value = "select * from bulk_load_head where BulkLoadCod=:code for update", nativeQuery = true)
    BulkLoadHeadEntity findForUpdate(@Param("code") String code);

    @Query(value = """
        select count(1)
        from bulk_load_head h
        where h.Status='A'
          and (:query='' or h.BulkLoadCod like concat('%',:query,'%')
               or h.OriginalFileName like concat('%',:query,'%')
               or h.CreationUser like concat('%',:query,'%'))
          and (:bulkLoadType='' or h.BulkLoadType=:bulkLoadType)
          and (:processStatus='' or h.ProcessStatus=:processStatus)
          and (:dateStart is null or date(h.CreationDate)>=:dateStart)
          and (:dateEnd is null or date(h.CreationDate)<=:dateEnd)
        """, nativeQuery = true)
    int countSearch(@Param("query") String query,
                    @Param("bulkLoadType") String bulkLoadType,
                    @Param("processStatus") String processStatus,
                    @Param("dateStart") String dateStart,
                    @Param("dateEnd") String dateEnd);

    @Query(value = """
        select *
        from bulk_load_head h
        where h.Status='A'
          and (:query='' or h.BulkLoadCod like concat('%',:query,'%')
               or h.OriginalFileName like concat('%',:query,'%')
               or h.CreationUser like concat('%',:query,'%'))
          and (:bulkLoadType='' or h.BulkLoadType=:bulkLoadType)
          and (:processStatus='' or h.ProcessStatus=:processStatus)
          and (:dateStart is null or date(h.CreationDate)>=:dateStart)
          and (:dateEnd is null or date(h.CreationDate)<=:dateEnd)
        order by h.CreationDate desc
        limit :init,:limit
        """, nativeQuery = true)
    List<BulkLoadHeadEntity> search(@Param("query") String query,
                                    @Param("bulkLoadType") String bulkLoadType,
                                    @Param("processStatus") String processStatus,
                                    @Param("dateStart") String dateStart,
                                    @Param("dateEnd") String dateEnd,
                                    @Param("init") int init,
                                    @Param("limit") int limit);

    @Query(value = """
        select *
        from bulk_load_head
        where Status='A'
          and ProcessStatus in ('Q','W')
        order by CreationDate
        """, nativeQuery = true)
    List<BulkLoadHeadEntity> findRecoverable();
}
