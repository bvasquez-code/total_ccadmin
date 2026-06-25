package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.SaleAppliedTaxEntity;
import com.ccadmin.app.sale.model.entity.id.SaleAppliedTaxID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleAppliedTaxRepository extends JpaRepository<SaleAppliedTaxEntity, SaleAppliedTaxID> {
}
