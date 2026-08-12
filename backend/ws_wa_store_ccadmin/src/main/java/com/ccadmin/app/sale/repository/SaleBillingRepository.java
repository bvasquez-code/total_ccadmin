package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleBillingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleBillingRepository extends JpaRepository<SaleBillingEntity, String> {

    @Query(value = """
            select sb.*
            from sale_billing sb
            where sb.SaleCod = :saleCod
              and sb.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<SaleBillingEntity> findActiveBySaleCod(@Param("saleCod") String saleCod);

    @Query(value = """
            select sb.*
            from sale_billing sb
            where sb.SaleCod = :saleCod
              and sb.Status = 'A'
            limit 1
            for update
            """, nativeQuery = true)
    Optional<SaleBillingEntity> findActiveBySaleCodForUpdate(@Param("saleCod") String saleCod);
}
