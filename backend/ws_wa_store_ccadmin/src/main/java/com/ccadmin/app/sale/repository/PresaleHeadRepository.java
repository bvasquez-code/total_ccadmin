package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresaleHeadRepository extends JpaRepository<PresaleHeadEntity,String>, CcAdminRepository<PresaleHeadEntity,String> {

    @Query(value = """
            CALL db_store_01.get_cod_trx(:storeCod, 'presale_head')
            """,nativeQuery = true)
    public String getPresaleCod(@Param("storeCod") String storeCod);

    @Override
    @Query(value = """
            select count(1) from presale_head ph
            where            
            (ph.PresaleCod = :id or ph.PresaleCod like %:query%)
            and ph.StoreCod = :storeCod
            and ph.SaleStatus = 'P'
            """,nativeQuery = true)
    public int countByQueryTextStore(
              @Param("id") String id
            , @Param("query") String query
            , @Param("storeCod") String storeCod
    );

    @Override
    @Query(value = """
            select ph.* from presale_head ph
            where        
            (ph.PresaleCod = :id or ph.PresaleCod like %:query%)
            and ph.StoreCod = :storeCod
            and ph.SaleStatus = 'P'
            order by ph.PresaleCod desc
            limit :init,:limit
            """,nativeQuery = true)
    public List<PresaleHeadEntity> findByQueryTextStore(
              @Param("id") String id
            , @Param("query") String query
            , @Param("storeCod") String storeCod
            , @Param("init") int init
            , @Param("limit") int limit
    );
}
