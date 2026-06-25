package com.ccadmin.app.cash.repository;

import com.ccadmin.app.cash.model.entity.CashSessionItemEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface CashSessionItemRepository extends JpaRepository<CashSessionItemEntity, Long> {

    @Query(value = """
        select sum(i.Denomination * i.Qty)
        from cash_session_item i
        where i.CashSessionID = :sessionId
          and i.ItemType = 'D'
          and i.Status = 'A'
        """, nativeQuery = true)
    BigDecimal sumDenominations(@Param("sessionId") Long sessionId);

    @Query(value = """
        select sum(case when i.MovementType='IN' then i.Amount else -i.Amount end)
        from cash_session_item i
        where i.CashSessionID = :sessionId
          and i.ItemType = 'M'
          and i.Status = 'A'
        """, nativeQuery = true)
    BigDecimal sumNetMovements(@Param("sessionId") Long sessionId);

    @Query(value = """
        select coalesce(sum(i.Amount),0)
        from cash_session_item i
        join payment_method pm on pm.PaymentMethodCod = i.PaymentMethodCod
        where i.CashSessionID = :sessionId
          and i.ItemType = 'P'
          and i.Status = 'A'
          and pm.IsCash = 0
        """, nativeQuery = true)
    BigDecimal sumCountedOther(@Param("sessionId") Long sessionId);

    @Query(value = """
        select coalesce(sum(i.Amount),0)
        from cash_session_item i
        join payment_method pm on pm.PaymentMethodCod = i.PaymentMethodCod
        where i.CashSessionID = :sessionId
          and i.ItemType = 'P'
          and i.Status = 'A'
          and pm.IsCash = 1
        """, nativeQuery = true)
    BigDecimal sumCountedCashFromPayments(@Param("sessionId") Long sessionId);

    @Query(value = """
        select i.*
        from cash_session_item i
        where i.CashSessionID = :sessionId
          and i.Status = 'A'
        order by i.CreationDate asc, i.ItemID asc
        """, nativeQuery = true)
    List<CashSessionItemEntity> findByCashSessionID(@Param("sessionId") Long sessionId);
}

