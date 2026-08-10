package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.DeliveryTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryTypeRepository extends JpaRepository<DeliveryTypeEntity, String> {

    @Query(value = """
            select dt.*
            from delivery_type dt
            where dt.Status = 'A'
            order by dt.Name, dt.DeliveryTypeCod
            """, nativeQuery = true)
    List<DeliveryTypeEntity> findAllActive();

    @Query(value = """
            select dt.*
            from delivery_type dt
            where dt.DeliveryTypeCod = :deliveryTypeCod
              and dt.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<DeliveryTypeEntity> findActiveByDeliveryTypeCod(
            @Param("deliveryTypeCod") String deliveryTypeCod
    );
}
