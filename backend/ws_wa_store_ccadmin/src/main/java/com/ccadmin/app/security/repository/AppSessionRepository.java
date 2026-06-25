package com.ccadmin.app.security.repository;

import com.ccadmin.app.security.model.entity.AppSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppSessionRepository extends JpaRepository<AppSessionEntity,Long> {

    @Modifying
    @Query(value = """
            INSERT INTO app_session_history
            (SessionID, UserCod, Token, SessionOjb, DeleteDate, CreationUser, CreationDate, ModifyUser, ModifyDate, Status)
            select
            SessionID, UserCod, Token, SessionOjb, now(), CreationUser, CreationDate, ModifyUser, ModifyDate, Status
            from
            app_session
            where UserCod = :UserCod and SessionID <> :SessionID
            """,nativeQuery = true)
    void saveHistory(@Param("UserCod")String UserCod,
                     @Param("SessionID")long SessionID
    );

    @Modifying
    @Query(value = """
            delete from app_session where UserCod = :UserCod and SessionID <> :SessionID
            """,nativeQuery = true)
    void cleanSession(
            @Param("UserCod")String UserCod
            ,@Param("SessionID")long SessionID
    );

    @Query( value = """
            select * from app_session s where s.UserCod = :UserCod order by s.SessionID desc limit 1
            """, nativeQuery = true)
    AppSessionEntity findSessionEnd(@Param("UserCod")String UserCod);
}
