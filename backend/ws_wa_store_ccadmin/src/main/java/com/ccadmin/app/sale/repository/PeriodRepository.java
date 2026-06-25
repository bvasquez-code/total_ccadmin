package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.PeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PeriodRepository extends JpaRepository<PeriodEntity,Integer> {

    @Query(value = """
            select * from period p where p.Status = 'A' order by PeriodId desc limit 1
            """, nativeQuery = true)
    public PeriodEntity findPeriodActuality();
}
