package com.ccadmin.app.cash.repository;

import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CashRegisterRepository extends JpaRepository<CashRegisterEntity, String>,
        CcAdminRepository<CashRegisterEntity, String> {

    @Override
    @Query(value = """
        select count(1)
        from cash_register r
        where r.RegisterCod = :id
           or r.StoreCod = :query
           or r.Name like %:query%
        """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
        select r.*
        from cash_register r
        where r.RegisterCod = :id
           or r.StoreCod = :query
           or r.Name like %:query%
        order by r.CreationDate desc
        limit :init, :limit
        """, nativeQuery = true)
    List<CashRegisterEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Query(value = """
        select r.*
        from cash_register r
        where r.Status = 'A'
        order by r.Name asc
        """, nativeQuery = true)
    List<CashRegisterEntity> findActives();

    @Query(value = """
        select r.*
        from cash_register r
        where r.StoreCod = :storeCod and r.Status = 'A'
        order by r.Name asc
        """, nativeQuery = true)
    List<CashRegisterEntity> findActivesByStore(@Param("storeCod") String storeCod);

    @Query(value = """
        select r.*
        from cash_register r
        where r.RegisterCod = :registerCod
          and r.Status = 'A'
        limit 1
        """, nativeQuery = true)
    Optional<CashRegisterEntity> findActiveByRegisterCod(@Param("registerCod") String registerCod);

    @Query(value = """
        select r.*
        from cash_register r
        where r.UserCod = :userCod
          and r.StoreCod = :storeCod
          and r.Status = 'A'
        order by r.CreationDate asc, r.RegisterCod asc
        limit 1
        """, nativeQuery = true)
    Optional<CashRegisterEntity> findActiveByUserAndStore(
            @Param("userCod") String userCod,
            @Param("storeCod") String storeCod
    );
}

