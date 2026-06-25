package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.id.SaleDetID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleDetRepository extends JpaRepository<SaleDetEntity, SaleDetID> {

    @Query( value = """
            select sd.* from sale_det sd where sd.SaleCod = :SaleCod and sd.Status = 'A'
            """, nativeQuery = true)
    public List<SaleDetEntity> findBySaleCod(@Param("SaleCod")String SaleCod);
}
