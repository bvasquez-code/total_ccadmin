package com.ccadmin.app.payment.repository;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrxPaymentRepository extends JpaRepository<TrxPaymentEntity, Long>, CcAdminRepository<TrxPaymentEntity, Long> {


    @Query( value = """
            select * from trx_payments tp where tp.TransactionId = :TransactionId
            """,
            nativeQuery = true)
    public TrxPaymentEntity findByTransactionId(@Param("TransactionId") String TransactionId);

    @Override
    @Query(value = """
            select count(1)
            from trx_payments tp
            where tp.TrxPaymentId = :id
               or tp.TransactionId like %:query%
               or tp.PaymentMethodCod like %:query%
               or tp.PaymentPlatform like %:query%
               or tp.PaymentStatus like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select *
            from trx_payments tp
            where tp.TrxPaymentId = :id
               or tp.TransactionId like %:query%
               or tp.PaymentMethodCod like %:query%
               or tp.PaymentPlatform like %:query%
               or tp.PaymentStatus like %:query%
            order by tp.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<TrxPaymentEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
