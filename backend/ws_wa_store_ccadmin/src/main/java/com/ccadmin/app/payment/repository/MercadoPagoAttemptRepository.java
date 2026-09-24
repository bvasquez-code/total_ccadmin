package com.ccadmin.app.payment.repository;

import com.ccadmin.app.payment.model.entity.MercadoPagoAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MercadoPagoAttemptRepository extends JpaRepository<MercadoPagoAttemptEntity, String> {
    // A locking read sees current rows even if a caller already opened a REPEATABLE READ snapshot.
    @Query(value = "select AttemptId from mercado_pago_attempt where SaleCod = :saleCod and PaymentState = 'P' limit 1 for update", nativeQuery = true)
    Optional<String> findPendingAttemptForUpdate(@Param("saleCod") String saleCod);

    default boolean hasPendingPayment(String saleCod) {
        return findPendingAttemptForUpdate(saleCod).isPresent();
    }

    @Query(value = "select * from mercado_pago_attempt where SaleCod = :saleCod order by CreationDate desc, AttemptId desc limit 1", nativeQuery = true)
    Optional<MercadoPagoAttemptEntity> findLatest(@Param("saleCod") String saleCod);

    @Query(value = "select * from mercado_pago_attempt where PaymentState = 'P' order by ModifyDate, CreationDate limit 50", nativeQuery = true)
    List<MercadoPagoAttemptEntity> findPending();
}
