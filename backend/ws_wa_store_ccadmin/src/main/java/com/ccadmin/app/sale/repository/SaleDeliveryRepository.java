package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleDeliveryEntity;
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
