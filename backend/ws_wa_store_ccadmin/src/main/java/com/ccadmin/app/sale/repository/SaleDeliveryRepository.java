package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
import com.ccadmin.app.sale.model.idto.ISaleWebOrderDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaleDeliveryRepository extends JpaRepository<SaleDeliveryEntity, String> {

    @Query(value = """
            select sd.*
            from sale_delivery sd
            where sd.SaleCod = :saleCod
              and sd.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<SaleDeliveryEntity> findActiveBySaleCod(@Param("saleCod") String saleCod);

    @Query(value = """
            select sd.*
            from sale_delivery sd
            where sd.SaleCod = :saleCod
              and sd.Status = 'A'
            limit 1
            for update
            """, nativeQuery = true)
    Optional<SaleDeliveryEntity> findActiveBySaleCodForUpdate(@Param("saleCod") String saleCod);

    @Query(value = """
            select count(1)
            from sale_delivery sd
            inner join sale_head sh
                on sh.SaleCod = sd.SaleCod
                and sh.Status = 'A'
            inner join sale_channel sc
                on sc.SaleCod = sh.SaleCod
                and sc.ChannelCod = 'WEB'
                and sc.Status = 'A'
            left join client c on c.ClientCod = sh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where sh.StoreCod = :storeCod
              and sd.Status = 'A'
              and (coalesce(:deliveryTypeCod, '') = '' or sd.DeliveryTypeCod = :deliveryTypeCod)
              and (coalesce(:deliveryStatus, '') = '' or sd.DeliveryStatus = :deliveryStatus)
              and (
                    coalesce(:query, '') = ''
                    or sh.SaleCod = :query
                    or concat_ws(' ',
                        sh.SaleCod,
                        sh.ClientCod,
                        p.DocumentNum,
                        p.Names,
                        p.LastNames,
                        p.CommercialName,
                        p.BusinessName
                    ) like concat('%', :query, '%')
              )
            """, nativeQuery = true)
    int countWebOrders(
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("deliveryTypeCod") String deliveryTypeCod,
            @Param("deliveryStatus") String deliveryStatus
    );

    @Query(value = """
            select
                sh.SaleCod as SaleCod,
                sh.PresaleCod as PresaleCod,
                sh.ClientCod as ClientCod,
                trim(concat_ws(' ',
                    nullif(p.DocumentNum, ''),
                    '-',
                    case
                        when p.PersonType = '01' then concat_ws(' ', p.Names, p.LastNames)
                        else coalesce(nullif(p.BusinessName, ''), nullif(p.CommercialName, ''), p.Names)
                    end
                )) as ClientName,
                sh.NumTotalPrice as NumTotalPrice,
                sh.CreationDate as CreationDate,
                sh.SaleStatus as SaleStatus,
                sh.IsPaid as IsPaid,
                sh.HasFiscalDocument as HasFiscalDocument,
                case
                    when sh.HasCreditNote = 'S'
                      or exists (
                          select 1
                          from credit_note_head cnh
                          where cnh.SaleCod = sh.SaleCod
                            and cnh.Status = 'A'
                      ) then 'S'
                    else 'N'
                end as HasCreditNote,
                sd.DeliveryTypeCod as DeliveryTypeCod,
                coalesce(dt.Name, sd.DeliveryTypeCod) as DeliveryTypeName,
                sd.DeliveryStatus as DeliveryStatus
            from sale_delivery sd
            inner join sale_head sh
                on sh.SaleCod = sd.SaleCod
                and sh.Status = 'A'
            inner join sale_channel sc
                on sc.SaleCod = sh.SaleCod
                and sc.ChannelCod = 'WEB'
                and sc.Status = 'A'
            inner join delivery_type dt
                on dt.DeliveryTypeCod = sd.DeliveryTypeCod
                and dt.Status = 'A'
            left join client c on c.ClientCod = sh.ClientCod
            left join person p on p.PersonCod = c.PersonCod
            where sh.StoreCod = :storeCod
              and sd.Status = 'A'
              and (coalesce(:deliveryTypeCod, '') = '' or sd.DeliveryTypeCod = :deliveryTypeCod)
              and (coalesce(:deliveryStatus, '') = '' or sd.DeliveryStatus = :deliveryStatus)
              and (
                    coalesce(:query, '') = ''
                    or sh.SaleCod = :query
                    or concat_ws(' ',
                        sh.SaleCod,
                        sh.ClientCod,
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
    List<ISaleWebOrderDto> findWebOrders(
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("deliveryTypeCod") String deliveryTypeCod,
            @Param("deliveryStatus") String deliveryStatus,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Query(value = """
            select sd.*
            from sale_delivery sd
            inner join sale_head sh on sh.SaleCod = sd.SaleCod
            where sh.StoreCod = :storeCod
              and sd.DeliveryStatus = :deliveryStatus
              and sd.Status = 'A'
              and sh.Status = 'A'
            order by coalesce(sd.ScheduledFrom, sd.CreationDate), sd.SaleCod
            """, nativeQuery = true)
    List<SaleDeliveryEntity> findActiveByStoreCodAndDeliveryStatus(
            @Param("storeCod") String storeCod,
            @Param("deliveryStatus") String deliveryStatus
    );
}
