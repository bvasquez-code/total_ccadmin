package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.id.SaleDetWarehouseID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleDetWarehouseRepository extends JpaRepository<SaleDetWarehouseEntity, SaleDetWarehouseID> {

    @Query( value = """
            select * from sale_det_warehouse s where s.SaleCod = :SaleCod and s.Status = 'A'
            """, nativeQuery = true)
    public List<SaleDetWarehouseEntity> findBySaleCod(@Param("SaleCod") String SaleCod);
}
