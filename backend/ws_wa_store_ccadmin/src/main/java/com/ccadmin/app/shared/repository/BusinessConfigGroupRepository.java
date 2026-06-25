package com.ccadmin.app.shared.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.shared.model.entity.BusinessConfigGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessConfigGroupRepository extends JpaRepository<BusinessConfigGroupEntity, String>, CcAdminRepository<BusinessConfigGroupEntity, String> {

    @Query(value = """
            select *
            from business_config_group bcg
            where bcg.GroupId = :GroupId
            limit 1
            """, nativeQuery = true)
    Optional<BusinessConfigGroupEntity> findByGroupId(@Param("GroupId") Integer GroupId);

    @Query(value = """
            select *
            from business_config_group bcg
            where bcg.Status = 'A'
            order by bcg.GroupName, bcg.GroupCod
            """, nativeQuery = true)
    List<BusinessConfigGroupEntity> findActives();

    @Override
    @Query(value = """
            select count(1)
            from business_config_group bcg
            where bcg.GroupCod = :id
               or bcg.GroupId = :id
               or bcg.GroupName like %:query%
               or bcg.GroupDesc like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select *
            from business_config_group bcg
            where bcg.GroupCod = :id
               or bcg.GroupId = :id
               or bcg.GroupName like %:query%
               or bcg.GroupDesc like %:query%
            order by bcg.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<BusinessConfigGroupEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
