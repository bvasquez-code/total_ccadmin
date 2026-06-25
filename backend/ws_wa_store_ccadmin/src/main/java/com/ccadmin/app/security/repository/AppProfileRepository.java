package com.ccadmin.app.security.repository;

import com.ccadmin.app.security.model.entity.AppProfileEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppProfileRepository extends JpaRepository<AppProfileEntity,String>,CcAdminRepository<AppProfileEntity,String> {

    @Override
    @Query( value = """
            select count(1) from app_profile ap
            where ap.ProfileCod = :id or ap.Description like %:query%
            """, nativeQuery = true)
    public int countByQueryText(
            @Param("id") String id,
            @Param("query") String query
    );

    @Override
    @Query( value = """
            select ap.* from app_profile ap
            where ap.ProfileCod = :id or ap.Description like %:query%
            limit :init,:limit
            """, nativeQuery = true)
    public List<AppProfileEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Query(value = """
            select ap.* from app_profile ap where ap.Status = 'A'
            """, nativeQuery = true)
    public List<AppProfileEntity> findAllActive();
}
