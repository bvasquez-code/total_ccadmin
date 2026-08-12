package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.model.idto.IExpectedTotalsDto;
import com.ccadmin.app.sale.model.idto.ISaleDeliveryOrderDto;
import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Date;

public interface SaleHeadRepository extends JpaRepository<SaleHeadEntity,String>, CcAdminRepository<SaleHeadEntity,String> {

    @Query(value = """
            select * from sale_head
            where SaleCod = :SaleCod
            for update
            """, nativeQuery = true)
    Optional<SaleHeadEntity> findByIdForUpdate(@Param("SaleCod") String saleCod);

    @Query(value = """
            select sh.*
            from sale_head sh
            inner join sale_channel sc on sc.SaleCod = sh.SaleCod
            where sh.SaleCod = :SaleCod
              and sh.ClientCod = :ClientCod
              and sc.ChannelCod = 'WEB'
              and sh.Status = 'A'
              and sc.Status = 'A'
            """, nativeQuery = true)
    Optional<SaleHeadEntity> findWebSale(
            @Param("SaleCod") String saleCod,
            @Param("ClientCod") String clientCod
    );

    @Query(value = """
            select sh.*
            from sale_head sh
            inner join sale_channel sc on sc.SaleCod = sh.SaleCod
            where sh.SaleCod = :SaleCod
              and sh.ClientCod = :ClientCod
              and sc.ChannelCod = 'WEB'
              and sh.Status = 'A'
              and sc.Status = 'A'
            for update
            """, nativeQuery = true)
    Optional<SaleHeadEntity> findWebSaleForUpdate(
            @Param("SaleCod") String saleCod,
            @Param("ClientCod") String clientCod
    );

    @Query(value = """
            select sh.*
            from sale_head sh
            inner join sale_channel sc on sc.SaleCod = sh.SaleCod
            where sh.SaleCod = :SaleCod
              and sc.ChannelCod = 'WEB'
              and sh.Status = 'A'
              and sc.Status = 'A'
            for update
            """, nativeQuery = true)
    Optional<SaleHeadEntity> findWebSaleBySaleCodForUpdate(@Param("SaleCod") String saleCod);

    @Query(value = """
            select count(1)
            from sale_head sh
            inner join sale_channel sc on sc.SaleCod = sh.SaleCod
            where sh.ClientCod = :ClientCod
              and sc.ChannelCod = 'WEB'
              and sh.Status = 'A'
              and sc.Status = 'A'
            """, nativeQuery = true)
    int countWebSalesByClientCod(@Param("ClientCod") String clientCod);

    @Query(value = """
            select
                sh.SaleCod as SaleCod,
                sh.PresaleCod as PresaleCod,
                sh.StoreCod as StoreCod,
                s.Name as StoreName,
                sh.CreationDate as CreationDate,
                sh.NumTotalPrice as NumTotalPrice,
                coalesce(ps.NumTotalPaid, 0) as NumTotalPaid,
                coalesce(ps.PaymentCount, 0) as PaymentCount,
                sh.CurrencyCod as CurrencyCod,
                sh.SaleStatus as SaleStatus,
                sh.IsPaid as IsPaid,
                sd.DeliveryTypeCod as DeliveryTypeCod,
                coalesce(dt.Name, sd.DeliveryTypeCod) as DeliveryTypeName,
                sd.DeliveryStatus as DeliveryStatus,
                sd.Address as Address,
                sd.ScheduledFrom as ScheduledFrom,
                sd.ScheduledTo as ScheduledTo,
                sd.TrackingNumber as TrackingNumber
            from sale_head sh
            inner join sale_channel sc on sc.SaleCod = sh.SaleCod
            inner join store s on s.StoreCod = sh.StoreCod
            left join sale_delivery sd
                on sd.SaleCod = sh.SaleCod
                and sd.Status = 'A'
            left join delivery_type dt
                on dt.DeliveryTypeCod = sd.DeliveryTypeCod
                and dt.Status = 'A'
            left join (
                select
                    sp.SaleCod,
                    sum(case when sp.Status = 'A' then sp.NumAmountPaid else 0 end) as NumTotalPaid,
                    count(1) as PaymentCount
                from sale_payments sp
                group by sp.SaleCod
            ) ps on ps.SaleCod = sh.SaleCod
            where sh.ClientCod = :ClientCod
              and sc.ChannelCod = 'WEB'
              and sh.Status = 'A'
              and sc.Status = 'A'
            order by sh.CreationDate desc, sh.SaleCod desc
            limit :Init, :Limit
            """, nativeQuery = true)
    List<ISaleDeliveryOrderDto> findWebSalesByClientCod(
            @Param("ClientCod") String clientCod,
            @Param("Init") int init,
            @Param("Limit") int limit
    );

    @Query(value = """
            select * from sale_head
            where PresaleCod = :PresaleCod
            order by CreationDate desc
            limit 1
            """, nativeQuery = true)
    Optional<SaleHeadEntity> findByPresaleCod(@Param("PresaleCod") String presaleCod);

    @Query(value = """
            select * from sale_head
            where PresaleCod = :PresaleCod
            order by CreationDate desc
            limit 1
            for update
            """, nativeQuery = true)
    Optional<SaleHeadEntity> findByPresaleCodForUpdate(@Param("PresaleCod") String presaleCod);

    @Query(value = """
            select sh.*
            from sale_head sh
            inner join presale_head ph on ph.PresaleCod = sh.PresaleCod
            where sh.SaleStatus = 'P'
              and ph.SaleStatus = 'C'
              and sh.Status = 'A'
              and ph.Status = 'A'
              and sh.CreationDate <= :ExpirationLimit
              and not exists (
                  select 1 from sale_document sd
                  where sd.SaleCod = sh.SaleCod and sd.Status = 'A'
              )
              and not exists (
                  select 1 from sale_payments sp
                  where sp.SaleCod = sh.SaleCod
              )
            order by sh.CreationDate, sh.SaleCod
            """, nativeQuery = true)
    List<SaleHeadEntity> findExpiredPendingSales(
            @Param("ExpirationLimit") Date expirationLimit
    );

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

    @Query(value = """
            select count(1)
            from sale_head sh
            inner join sale_channel sc
                on sc.SaleCod = sh.SaleCod
                and sc.Status = 'A'
            left join client c on c.ClientCod = sh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where sh.StoreCod = :storeCod
              and sc.ChannelCod = :channelCod
              and (
                    coalesce(:query, '') = ''
                    or sh.SaleCod = :query
                    or concat_ws(' ',
                        sh.SaleCod,
                        p.DocumentNum,
                        p.Names,
                        p.LastNames,
                        p.CommercialName,
                        p.BusinessName
                    ) like concat('%', :query, '%')
              )
            """, nativeQuery = true)
    int countByStoreAndChannel(
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("channelCod") String channelCod
    );

    @Query(value = """
            select sh.*
            from sale_head sh
            inner join sale_channel sc
                on sc.SaleCod = sh.SaleCod
                and sc.Status = 'A'
            left join client c on c.ClientCod = sh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where sh.StoreCod = :storeCod
              and sc.ChannelCod = :channelCod
              and (
                    coalesce(:query, '') = ''
                    or sh.SaleCod = :query
                    or concat_ws(' ',
                        sh.SaleCod,
                        p.DocumentNum,
                        p.Names,
                        p.LastNames,
                        p.CommercialName,
                        p.BusinessName
                    ) like concat('%', :query, '%')
              )
            order by sh.CreationDate desc, sh.SaleCod desc
            limit :init, :limit
            """, nativeQuery = true)
    List<SaleHeadEntity> findByStoreAndChannel(
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("channelCod") String channelCod,
            @Param("init") int init,
            @Param("limit") int limit
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
            tp.CashSessionID = :sessionId
            and sh.Status = 'A'
            and sp.Status = 'A'
            and tp.Status = 'A'
            and tp.PaymentMethodCod <> 'NC001'
        """, nativeQuery = true)
    IExpectedTotalsDto getExpectedTotalsForSession(@Param("sessionId") Long sessionId);

}
