package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.StoreVirtualConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreVirtualConfigRepository extends JpaRepository<StoreVirtualConfigEntity, String> {

    @Query(value = """
            select svc.*
            from store_virtual_config svc
            where svc.StoreCod = :storeCod
            limit 1
            """, nativeQuery = true)
    Optional<StoreVirtualConfigEntity> findByStoreCod(@Param("storeCod") String storeCod);

    @Query(value = """
            select svc.*
            from store_virtual_config svc
            inner join store s on s.StoreCod = svc.StoreCod
            where svc.StoreCod = :storeCod
              and svc.Status = 'A'
              and s.Status = 'A'
              and s.IsVirtualStoreEnabled = 'S'
            limit 1
            """, nativeQuery = true)
    Optional<StoreVirtualConfigEntity> findActiveByStoreCod(@Param("storeCod") String storeCod);
}
