package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.TaxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaxRepository extends JpaRepository<TaxEntity,String> {

    @Query( value = """
            select * from tax t where t.Status = 'A'
            """,nativeQuery = true)
    public List<TaxEntity> findAllActive();
}
