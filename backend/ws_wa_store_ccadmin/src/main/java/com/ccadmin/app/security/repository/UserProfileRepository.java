package com.ccadmin.app.security.repository;

import com.ccadmin.app.security.model.entity.UserProfileEntity;
import com.ccadmin.app.security.model.entity.id.UserProfileID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UserProfileID> {


    @Modifying
    @Query( value = """
            update user_profile set Status = :Status where UserCod = :UserCod                
            """, nativeQuery = true)
    public void updateAllStatus(
            @Param("Status")String Status,
            @Param("UserCod") String UserCod
    );

    @Query( value = """
            select * from user_profile up where up.Status = 'A' and up.UserCod = :UserCod
            """,nativeQuery = true)
    public List<UserProfileEntity> findAllByUser(@Param("UserCod")String UserCod);
}
