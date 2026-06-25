package com.ccadmin.app.shared.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.entity.id.BusinessConfigEntityID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessConfigRepository extends JpaRepository<BusinessConfigEntity, BusinessConfigEntityID>, CcAdminRepository<BusinessConfigEntity, BusinessConfigEntityID> {


    @Query( value = """
            select bc.* from business_config bc
            where bc.GroupCod = :GroupCod
            and bc.ConfigCod = :ConfigCod      
            """, nativeQuery = true)
    public BusinessConfigEntity findByConfigCod(
             @Param("GroupCod") String GroupCod
            ,@Param("ConfigCod") String ConfigCod
    );

    @Query( value = """
            select * from business_config bc where bc.GroupCod = :GroupCod
            """, nativeQuery = true)
    List<BusinessConfigEntity> findByGroupCod(
            @Param("GroupCod") String GroupCod
    );

    @Query( value = """
            select *
            from business_config bc
            where bc.GroupCod = :GroupCod
              and bc.Status = 'A'
            order by bc.ConfigCorr
            """, nativeQuery = true)
    List<BusinessConfigEntity> findActivesByGroupCod(
            @Param("GroupCod") String GroupCod
    );

    @Query(value = """
            select *
            from business_config bc
            where bc.Status = 'A'
            order by bc.GroupCod, bc.ConfigCorr
            """, nativeQuery = true)
    List<BusinessConfigEntity> findActives();

    @Override
    @Query(value = """
            select count(1)
            from business_config bc
            where bc.GroupCod = :id
               or bc.ConfigCorr = :id
               or bc.ConfigCod like %:query%
               or bc.ConfigVal like %:query%
               or bc.ConfigName like %:query%
               or bc.ConfigDesc like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select *
            from business_config bc
            where bc.GroupCod = :id
               or bc.ConfigCorr = :id
               or bc.ConfigCod like %:query%
               or bc.ConfigVal like %:query%
               or bc.ConfigName like %:query%
               or bc.ConfigDesc like %:query%
            order by bc.GroupCod, bc.ConfigCorr
            limit :init, :limit
            """, nativeQuery = true)
    List<BusinessConfigEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Override
    @Query(value = """
            select count(1)
            from business_config bc
            where bc.GroupCod = :storeCod
              and (bc.GroupCod = :id
                   or bc.ConfigCorr = :id
                   or bc.ConfigCod like %:query%
                   or bc.ConfigVal like %:query%
                   or bc.ConfigName like %:query%
                   or bc.ConfigDesc like %:query%)
            """, nativeQuery = true)
    int countByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod
    );

    @Override
    @Query(value = """
            select *
            from business_config bc
            where bc.GroupCod = :storeCod
              and (bc.GroupCod = :id
                   or bc.ConfigCorr = :id
                   or bc.ConfigCod like %:query%
                   or bc.ConfigVal like %:query%
                   or bc.ConfigName like %:query%
                   or bc.ConfigDesc like %:query%)
            order by bc.ConfigCorr
            limit :init, :limit
            """, nativeQuery = true)
    List<BusinessConfigEntity> findByQueryTextStore(
            @Param("id") String id,
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("init") int init,
            @Param("limit") int limit
    );

}
