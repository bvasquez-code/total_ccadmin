package com.ccadmin.app.security.repository;

import com.ccadmin.app.security.model.entity.AppSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AppSessionRepository extends JpaRepository<AppSessionEntity,Long> {

    @Modifying
    @Query(value = """
            INSERT INTO app_session_history
            (SessionID, CashSessionID, UserCod, Token, SessionOjb, DeleteDate, CreationUser, CreationDate, ModifyUser, ModifyDate, Status)
            select
            SessionID, CashSessionID, UserCod, Token, SessionOjb, now(), CreationUser, CreationDate, ModifyUser, ModifyDate, Status
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

    @Query(value = """
            select *
            from app_session s
            where s.Token = :token
              and s.Status = 'A'
              and s.DeleteDate is null
            limit 1
            """, nativeQuery = true)
    Optional<AppSessionEntity> findActiveByToken(@Param("token") String token);

    @Query(value = """
            select *
            from app_session s
            where s.SessionID = :sessionId
              and s.Status = 'A'
              and s.DeleteDate is null
            limit 1
            """, nativeQuery = true)
    Optional<AppSessionEntity> findActiveBySessionId(@Param("sessionId") Long sessionId);

    @Modifying
    @Transactional
    @Query(value = """
            update app_session
            set CashSessionID = :cashSessionId,
                ModifyUser = :userCod,
                ModifyDate = now()
            where SessionID = :sessionId
              and UserCod = :userCod
              and Status = 'A'
              and DeleteDate is null
            """, nativeQuery = true)
    int updateCashSessionId(
            @Param("sessionId") Long sessionId,
            @Param("userCod") String userCod,
            @Param("cashSessionId") Long cashSessionId
    );

    @Modifying
    @Transactional
    @Query(value = """
            update app_session
            set CashSessionID = null,
                ModifyUser = :userCod,
                ModifyDate = now()
            where CashSessionID = :cashSessionId
              and UserCod = :userCod
              and Status = 'A'
              and DeleteDate is null
            """, nativeQuery = true)
    int clearCashSessionId(
            @Param("cashSessionId") Long cashSessionId,
            @Param("userCod") String userCod
    );
}
