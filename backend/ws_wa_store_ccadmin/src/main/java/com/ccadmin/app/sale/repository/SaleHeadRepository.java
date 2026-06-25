package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.idto.IExpectedTotalsDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleHeadRepository extends JpaRepository<SaleHeadEntity,String>, CcAdminRepository<SaleHeadEntity,String> {

    @Query(value = """
            CALL db_store_01.get_cod_trx(:storeCod, 'sale_head')
            """,nativeQuery = true)
    public String getSaleCod(@Param("storeCod") String storeCod);

    @Override
    @Query(value = """
            select count(1) from sale_head sh
            left join client c on c.ClientCod = sh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where
            sh.SaleCod = :id or ( ('' != :query and concat(sh.SaleCod,p.DocumentNum,p.Names,p.LastNames) like concat('%',:query,'%') ) or ( '' = :query ) )
            and sh.StoreCod = :storeCod
            """,nativeQuery = true)
    public int countByQueryTextStore(
              @Param("id") String id
            , @Param("query") String query
            , @Param("storeCod") String storeCod
    );

    @Override
    @Query(value = """
            select sh.* from sale_head sh
            left join client c on c.ClientCod = sh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where
            sh.SaleCod = :id or ( ('' != :query and concat(sh.SaleCod,p.DocumentNum,p.Names,p.LastNames) like concat('%',:query,'%') ) or ( '' = :query ) )
            and sh.StoreCod = :storeCod
            order by sh.SaleCod desc
            limit :init,:limit
            """,nativeQuery = true)
    public List<SaleHeadEntity> findByQueryTextStore(
              @Param("id") String id
            , @Param("query") String query
            , @Param("storeCod") String storeCod
            , @Param("init") int init
            , @Param("limit") int limit
    );

    @Modifying
    @Query(value = """
            update sale_head set
                HasCreditNote = :HasCreditNote
            where
                SaleCod = :SaleCod
           """,nativeQuery = true)
    void updateHasCreditNote(
            @Param("SaleCod") String SaleCod,
            @Param("HasCreditNote") String HasCreditNote
    );

    @Query(value = """
            select
              coalesce(sum(case when pm.PaymentMethodType = '1001' then sp.NumAmountPaid  else 0 end),0) as Cash,
              coalesce(sum(case when pm.PaymentMethodType != '1001' then sp.NumAmountPaid else 0 end),0) as Other,
              coalesce(sum(sp.NumAmountPaid),0) as Total
            from sale_head sh
            inner join sale_payments sp on sp.SaleCod = sh.SaleCod
            inner join trx_payments tp on tp.TrxPaymentId = sp.TrxPaymentId
            inner join payment_method pm on pm.PaymentMethodCod  = tp.PaymentMethodCod
            where 
            CashSessionID = :sessionId
            and sh.Status = 'A'
        """, nativeQuery = true)
    IExpectedTotalsDto getExpectedTotalsForSession(@Param("sessionId") Long sessionId);

}
