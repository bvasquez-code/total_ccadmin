package com.ccadmin.app.inventory.repository;

import com.ccadmin.app.inventory.model.entity.StockExitDetEntity;
import com.ccadmin.app.inventory.model.entity.id.StockExitDetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockExitDetRepository extends JpaRepository<StockExitDetEntity, StockExitDetId> {
    @Query(value = "select * from stock_exit_det where StockExitCod=:code and Status='A' order by ItemNumber", nativeQuery = true)
    List<StockExitDetEntity> findByCode(@Param("code") String code);

    @Query(value = "select * from stock_exit_det where StockExitCod=:code and ItemNumber=:item for update", nativeQuery = true)
    StockExitDetEntity findForUpdate(@Param("code") String code, @Param("item") Integer item);

    @Query(value = """
        select count(1) from stock_exit_det
        where StockExitCod=:code and Status='A' and NumUnitPending>0
        """, nativeQuery = true)
    int countPendingByCode(@Param("code") String code);

    @Modifying
    @Query(value = "delete from stock_exit_det where StockExitCod=:code", nativeQuery = true)
    void deleteByCode(@Param("code") String code);
}
