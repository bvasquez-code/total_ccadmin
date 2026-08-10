package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.VirtualCartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface VirtualCartRepository extends JpaRepository<VirtualCartEntity, String> {

    @Query(value = """
            select vc.*
            from virtual_cart vc
            where vc.CartCod = :cartCod
              and vc.CartStatus = 'ACTIVE'
              and vc.ExpiresDate > now()
              and vc.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<VirtualCartEntity> findActiveByCartCod(@Param("cartCod") String cartCod);

    @Query(value = """
            select vc.*
            from virtual_cart vc
            where vc.ClientCod = :clientCod
              and vc.CartStatus = 'ACTIVE'
              and vc.ExpiresDate > now()
              and vc.Status = 'A'
            order by vc.ModifyDate desc, vc.CreationDate desc
            """, nativeQuery = true)
    List<VirtualCartEntity> findActiveByClientCod(@Param("clientCod") String clientCod);

    @Query(value = """
            select vc.*
            from virtual_cart vc
            where vc.CartStatus = 'ACTIVE'
              and vc.ExpiresDate <= :expirationDate
              and vc.Status = 'A'
            order by vc.ExpiresDate, vc.CartCod
            limit :limit
            """, nativeQuery = true)
    List<VirtualCartEntity> findPendingExpiration(
            @Param("expirationDate") Date expirationDate,
            @Param("limit") int limit
    );
}
