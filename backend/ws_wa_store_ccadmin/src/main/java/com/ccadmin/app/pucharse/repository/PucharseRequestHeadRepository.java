package com.ccadmin.app.pucharse.repository;

import com.ccadmin.app.pucharse.model.entity.PucharseRequestHeadEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PucharseRequestHeadRepository extends JpaRepository<PucharseRequestHeadEntity,String>, CcAdminRepository<PucharseRequestHeadEntity,String> {


    @Query(value = """
            CALL db_store_01.get_cod_trx(:StoreCod, 'pucharse_request_head')
            """, nativeQuery = true)
    public String getPucharseReqCod(
            @Param("StoreCod") String StoreCod
    );

    @Override
    @Query(value = """
            select count(1) from pucharse_request_head prh
            where prh.StoreCod = :storeCod
            and (prh.PucharseReqCod = :id or CONCAT(prh.PucharseReqCod,prh.DealerCod,prh.ExternalCod) like %:query%)
            """, nativeQuery = true)
    public int countByQueryTextStore(
             @Param("id") String id
            ,@Param("query") String query
            ,@Param("storeCod") String storeCod
    );

    @Override
    @Query(value = """
            select prh.* from pucharse_request_head prh
            where prh.StoreCod = :storeCod
            and (prh.PucharseReqCod = :id or CONCAT(prh.PucharseReqCod,prh.DealerCod,prh.ExternalCod) like %:query%)
            order by prh.PucharseReqCod desc
            limit :init,:limit
            """, nativeQuery = true)
    public List<PucharseRequestHeadEntity> findByQueryTextStore(
             @Param("id")String id
            ,@Param("query") String query
            ,@Param("storeCod") String storeCod
            ,@Param("init") int init
            ,@Param("limit") int limit
    );
}
