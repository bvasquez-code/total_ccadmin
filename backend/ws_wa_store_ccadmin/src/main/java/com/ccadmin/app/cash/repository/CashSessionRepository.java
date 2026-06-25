package com.ccadmin.app.cash.repository;

import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface CashSessionRepository extends JpaRepository<CashSessionEntity, Long> {

    @Query(value = """
        select s.*
        from cash_session s
        where s.RegisterCod = :registerCod
          and s.IsOpen = 1
          and s.Status = 'A'
        limit 1
        """, nativeQuery = true)
    Optional<CashSessionEntity> findOpenByRegister(@Param("registerCod") String registerCod);

    @Query(value = """
        select s.*
        from cash_session s
        where s.StoreCod = :storeCod
          and s.OpenDate between :from and :to
        order by s.OpenDate desc
        """, nativeQuery = true)
    List<CashSessionEntity> findByStoreAndDate(
            @Param("storeCod") String storeCod,
            @Param("from") Date from,
            @Param("to") Date to
    );
}
