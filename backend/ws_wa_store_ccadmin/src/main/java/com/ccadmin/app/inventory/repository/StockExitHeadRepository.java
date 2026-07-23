package com.ccadmin.app.inventory.repository;

import com.ccadmin.app.inventory.model.entity.StockExitHeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockExitHeadRepository extends JpaRepository<StockExitHeadEntity, String> {
    @Query(value = "CALL db_store_01.get_cod_trx(:storeCod, 'stock_exit_head')", nativeQuery = true)
    String createCode(@Param("storeCod") String storeCod);

    @Query(value = "select * from stock_exit_head where StockExitCod=:code for update", nativeQuery = true)
    StockExitHeadEntity findForUpdate(@Param("code") String code);

    @Query(value = """
        select count(1) from stock_exit_head h
        where h.StoreCod=:storeCod and h.Status='A'
          and (:query='' or h.StockExitCod like concat('%',:query,'%') or h.Observation like concat('%',:query,'%'))
          and (:processStatus='' or h.ProcessStatus=:processStatus)
          and (:processType='' or h.ProcessType=:processType)
          and (:dateStart is null or date(h.CreationDate)>=:dateStart)
          and (:dateEnd is null or date(h.CreationDate)<=:dateEnd)
        """, nativeQuery = true)
    int countSearch(@Param("storeCod") String storeCod, @Param("query") String query,
                    @Param("processStatus") String processStatus, @Param("processType") String processType,
                    @Param("dateStart") String dateStart, @Param("dateEnd") String dateEnd);

    @Query(value = """
        select * from stock_exit_head h
        where h.StoreCod=:storeCod and h.Status='A'
          and (:query='' or h.StockExitCod like concat('%',:query,'%') or h.Observation like concat('%',:query,'%'))
          and (:processStatus='' or h.ProcessStatus=:processStatus)
          and (:processType='' or h.ProcessType=:processType)
          and (:dateStart is null or date(h.CreationDate)>=:dateStart)
          and (:dateEnd is null or date(h.CreationDate)<=:dateEnd)
        order by h.CreationDate desc limit :init,:limit
        """, nativeQuery = true)
    List<StockExitHeadEntity> search(@Param("storeCod") String storeCod, @Param("query") String query,
                    @Param("processStatus") String processStatus, @Param("processType") String processType,
                    @Param("dateStart") String dateStart, @Param("dateEnd") String dateEnd,
                    @Param("init") int init, @Param("limit") int limit);
}
