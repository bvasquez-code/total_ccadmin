package com.ccadmin.app.inventory.repository;

import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.id.StockEntryDetId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockEntryDetRepository extends JpaRepository<StockEntryDetEntity, StockEntryDetId> {
    @Query(value = "select * from stock_entry_det where StockEntryCod=:code and Status='A' order by ItemNumber", nativeQuery = true)
    List<StockEntryDetEntity> findByCode(@Param("code") String code);

    @Query(value = "select * from stock_entry_det where StockEntryCod=:code and ItemNumber=:item for update", nativeQuery = true)
    StockEntryDetEntity findForUpdate(@Param("code") String code, @Param("item") Integer item);

    @Query(value = """
        select count(1) from stock_entry_det
        where StockEntryCod=:code and Status='A' and NumUnitPending>0
        """, nativeQuery = true)
    int countPendingByCode(@Param("code") String code);

    @Modifying
    @Query(value = "delete from stock_entry_det where StockEntryCod=:code", nativeQuery = true)
    void deleteByCode(@Param("code") String code);
}
