package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.id.PresaleDetWarehouseID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresaleDetWarehouseRepository extends JpaRepository<PresaleDetWarehouseEntity, PresaleDetWarehouseID> {

    @Modifying
    @Query( value = """
            update presale_det_warehouse set Status = :Status where PresaleCod = :PresaleCod
            """,nativeQuery = true)
    public void updateStatusAll(
             @Param("PresaleCod") String PresaleCod
            ,@Param("Status") String Status
    );

    @Query( value = """
            select * from presale_det_warehouse 
            where 
            PresaleCod = :PresaleCod 
            and ItemNumber = :ItemNumber
            and Status = 'A'
            """,nativeQuery = true)
    public List<PresaleDetWarehouseEntity> findByItemNumber(
            @Param("PresaleCod") String PresaleCod,
            @Param("ItemNumber") int ItemNumber
    );
}
