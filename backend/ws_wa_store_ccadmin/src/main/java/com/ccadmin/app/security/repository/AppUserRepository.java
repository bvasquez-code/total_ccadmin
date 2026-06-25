package com.ccadmin.app.security.repository;

import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUserEntity,String>, CcAdminRepository<AppUserEntity,String> {


    @Override
    @Query( value = """
            select COUNT(1) from app_user au
            inner join person p on au.PersonCod = p.PersonCod
            where
            au.UserCod = :id or CONCAT(p.Names,p.LastNames) like %:query%
            """, nativeQuery = true)
    public int countByQueryText(
            @Param("id") String id,
            @Param("query") String query
    );

    @Override
    @Query( value = """
            select au.* from app_user au
            inner join person p on au.PersonCod = p.PersonCod
            where
            au.UserCod = :id or CONCAT(p.Names,p.LastNames) like %:query%
            limit :init,:limit
            """, nativeQuery = true)
    public List<AppUserEntity> findByQueryText(
            @Param("id")String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
