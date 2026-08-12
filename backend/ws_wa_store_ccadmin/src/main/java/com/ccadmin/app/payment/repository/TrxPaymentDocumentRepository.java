package com.ccadmin.app.payment.repository;

import com.ccadmin.app.payment.model.entity.TrxPaymentDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrxPaymentDocumentRepository extends JpaRepository<TrxPaymentDocumentEntity, Long> {

    @Query(value = """
            select tpd.*
            from trx_payments_document tpd
            where tpd.TrxPaymentId = :trxPaymentId
              and tpd.Status = 'A'
            order by tpd.CreationDate asc, tpd.TrxPaymentDocumentId asc
            """, nativeQuery = true)
    List<TrxPaymentDocumentEntity> findActiveByTrxPaymentId(
            @Param("trxPaymentId") Long trxPaymentId
    );
}
