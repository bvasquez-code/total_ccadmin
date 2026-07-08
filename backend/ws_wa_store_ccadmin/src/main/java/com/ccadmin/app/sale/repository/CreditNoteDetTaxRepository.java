package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.CreditNoteDetTaxEntity;
import com.ccadmin.app.sale.model.entity.id.CreditNoteDetTaxID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CreditNoteDetTaxRepository extends JpaRepository<CreditNoteDetTaxEntity, CreditNoteDetTaxID> {

    @Query(value = """
            select cndt.*
            from credit_note_det_tax cndt
            where cndt.CreditNoteCod = :CreditNoteCod
              and cndt.Status = 'A'
            order by cndt.ItemNumber, cndt.TaxLineNumber
            """, nativeQuery = true)
    List<CreditNoteDetTaxEntity> findByCreditNoteCod(@Param("CreditNoteCod") String CreditNoteCod);

    @Modifying
    @Query(value = """
            update credit_note_det_tax
            set Status = :Status
            where CreditNoteCod = :CreditNoteCod
            """, nativeQuery = true)
    void updateStatusAll(
            @Param("CreditNoteCod") String CreditNoteCod,
            @Param("Status") String Status
    );
}
