package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.ShippingProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingProviderRepository extends JpaRepository<ShippingProviderEntity, String> {

    @Query(value = """
            select sp.*
            from shipping_provider sp
            where sp.Status = 'A'
            order by sp.Name, sp.ShippingProviderCod
            """, nativeQuery = true)
    List<ShippingProviderEntity> findAllActive();

    @Query(value = """
            select sp.*
            from shipping_provider sp
            where sp.ShippingProviderCod = :shippingProviderCod
              and sp.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<ShippingProviderEntity> findActiveByShippingProviderCod(
            @Param("shippingProviderCod") String shippingProviderCod
    );
}
