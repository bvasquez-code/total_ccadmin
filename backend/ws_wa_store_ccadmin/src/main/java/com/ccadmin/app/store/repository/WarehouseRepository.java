package com.ccadmin.app.store.repository;

import com.ccadmin.app.store.model.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity,String> {

    @Query(value = """
            select count(1)  from warehouse w where w.StoreCod = :StoreCod and Status = 'A'
            """,nativeQuery = true)
    public int countNumberWarehouse(@Param("StoreCod") String StoreCod);

    @Query(value = """
            select *  from warehouse w where w.StoreCod = :StoreCod and Status = 'A'
            """,nativeQuery = true)
    public List<WarehouseEntity> findByStore(@Param("StoreCod") String StoreCod);

}
