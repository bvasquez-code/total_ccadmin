package com.ccadmin.app.sunat.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SunatConfigRepository extends JpaRepository<SunatConfigEntity, String>,
        CcAdminRepository<SunatConfigEntity, String> {

    @Query(value = """
            select sc.* from sunat_config sc
            where sc.Status = 'A' and sc.ActiveConfig = 'S'
            order by sc.ModifyDate desc
            limit 1
            """, nativeQuery = true)
    Optional<SunatConfigEntity> findActiveConfig();

    @Modifying
    @Query(value = """
            update sunat_config
            set ActiveConfig = 'N', ModifyUser = :userCod, ModifyDate = now()
            where ActiveConfig = 'S'
            """, nativeQuery = true)
    void deactivateActiveConfigs(@Param("userCod") String userCod);

    @Override
    @Query(value = """
            select count(1)
            from sunat_config sc
            where sc.SunatConfigCod = :id
               or sc.IssuerRuc like %:query%
               or sc.Environment like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select sc.*
            from sunat_config sc
            where sc.SunatConfigCod = :id
               or sc.IssuerRuc like %:query%
               or sc.Environment like %:query%
            order by sc.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<SunatConfigEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
