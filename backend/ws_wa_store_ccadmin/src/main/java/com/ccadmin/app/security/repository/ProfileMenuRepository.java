package com.ccadmin.app.security.repository;

import com.ccadmin.app.security.model.entity.ProfileMenuEntity;
import com.ccadmin.app.security.model.entity.id.ProfileMenuID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfileMenuRepository extends JpaRepository<ProfileMenuEntity, ProfileMenuID> {


    @Modifying
    @Query( value = """
            update profile_menu set Status = :Status
            where ProfileCod = :ProfileCod                   
            """, nativeQuery = true)
    public void updateAllStatus(
            @Param("Status")String Status,
            @Param("ProfileCod") String ProfileCod
    );

    @Query( value = """
            select * from profile_menu
            where ProfileCod = :ProfileCod and Status = 'A'                
            """, nativeQuery = true)
    public List<ProfileMenuEntity> findAllByProfile(@Param("ProfileCod") String ProfileCod);


    @Query( value = """
            select pm.* from user_profile up
            inner join profile_menu pm on pm.ProfileCod = up.ProfileCod
            where up.UserCod  = :UserCod
            and up.Status = 'A'
            and pm.Status = 'A'     
            """, nativeQuery = true)
    public List<ProfileMenuEntity> findAllByUser(@Param("UserCod") String UserCod);
}
