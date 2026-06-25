package com.ccadmin.app.system.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.system.model.entity.AppFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppFileRepository extends JpaRepository<AppFileEntity,String>, CcAdminRepository<AppFileEntity,String> {

    @Override
    @Query(value = """
            select count(1) from app_file af
            where af.FileCod = :id
            or '' = :query
            """, nativeQuery = true)
    public int countByQueryText(
            @Param("id") String id,
            @Param("query") String query
    );

    @Override
    @Query(value = """
            select af.* from app_file af
            where af.FileCod = :id
            or '' = :query
            limit :init,:limit
            """, nativeQuery = true)
    public List<AppFileEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
