package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaleChannelRepository extends JpaRepository<SaleChannelEntity, String> {

    @Query(value = """
            select sc.*
            from sale_channel sc
            inner join commercial_channel cc on cc.ChannelCod = sc.ChannelCod
            where sc.SaleCod = :saleCod
              and sc.Status = 'A'
              and cc.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<SaleChannelEntity> findActiveBySaleCod(@Param("saleCod") String saleCod);
}
