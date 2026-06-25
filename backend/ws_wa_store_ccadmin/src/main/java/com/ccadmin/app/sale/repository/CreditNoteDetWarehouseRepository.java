package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.CreditNoteDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.id.CreditNoteDetWarehouseID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteDetWarehouseRepository extends JpaRepository<CreditNoteDetWarehouseEntity, CreditNoteDetWarehouseID> {
}
