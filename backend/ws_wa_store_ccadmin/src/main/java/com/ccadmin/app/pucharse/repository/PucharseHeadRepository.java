package com.ccadmin.app.pucharse.repository;

import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PucharseHeadRepository extends JpaRepository<PucharseHeadEntity,String>, CcAdminRepository<PucharseHeadEntity,String> {

    @Query(value = """
            select * from pucharse_head
            where PucharseCod = :PucharseCod
            for update
            """, nativeQuery = true)
    Optional<PucharseHeadEntity> findByIdForUpdate(@Param("PucharseCod") String pucharseCod);

    @Query(value = """
            CALL get_cod_trx(:storeCod, 'pucharse_head')
            """,nativeQuery = true)
    public String getPucharseCod(@Param("storeCod") String storeCod);

    @Query(value = """
            SELECT ph.* FROM pucharse_head ph WHERE ph.PucharseReqCod = :PucharseReqCod
            """, nativeQuery = true)
    public PucharseHeadEntity findByPucharseReqCod(@Param("PucharseReqCod") String PucharseReqCod);

    @Override
    @Query(value = """
            SELECT count(1) FROM pucharse_head ph
            where ph.StoreCod = :storeCod
            and (ph.PucharseCod = :id or concat(ph.PucharseCod,ph.ExternalCod,ph.DealerCod) like %:query%)
            """, nativeQuery = true)
    public int countByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod
    );

    @Override
    @Query(value = """
            SELECT ph.* FROM pucharse_head ph
            where ph.StoreCod = :storeCod
            and (ph.PucharseCod = :id or concat(ph.PucharseCod,ph.ExternalCod,ph.DealerCod) like %:query%)
            order by ph.PucharseCod desc
            limit :init,:limit
            """, nativeQuery = true)
    public List<PucharseHeadEntity> findByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
