package com.ccadmin.app.user.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.user.model.entity.AppMenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppMenuRepository extends JpaRepository<AppMenuEntity,String>, CcAdminRepository<AppMenuEntity,String> {

    @Query( value = """
            select count(1) from app_menu am 
            where ( am.MenuCod = :id or am.Name like %:query% )
            """, nativeQuery = true)
    @Override
    public int countByQueryText(
            @Param("id") String id
            ,@Param("query") String query
    );

    @Query(value = """
            select * from app_menu am 
            where ( am.MenuCod = :id or am.Name like %:query% )
            order by am.ModifyDate desc
            limit :init,:limit   
            """ , nativeQuery = true)
    @Override
    public List<AppMenuEntity> findByQueryText(
             @Param("id") String id
            ,@Param("query") String query
            ,@Param("init") int init
            ,@Param("limit") int limit
    );

    @Query( value = """
            select * from app_menu am where am.Status = 'A' and am.IsMenuDad = 'S'
            """, nativeQuery = true)
    public List<AppMenuEntity> findMenuDad();

    @Query( value = """
            select * from app_menu am where am.Status = 'A' and am.IsMenuDad = 'N' and am.MenuDadCod = :MenuDadCod
            """, nativeQuery = true)
    public List<AppMenuEntity> findMenuChild(@Param("MenuDadCod") String MenuDadCod);

    @Query( value = """
            select am.* from app_menu am
            where am.MenuCod in (
            	select pm.MenuCod from user_profile up
            	inner join profile_menu pm on pm.ProfileCod = up.ProfileCod
            	where up.UserCod  = :UserCod
            	and up.Status = 'A'
            	and pm.Status = 'A'
            )
            """, nativeQuery = true)
    public List<AppMenuEntity> findByUser(@Param("UserCod") String UserCod);


}
