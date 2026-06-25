package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.id.CreditNoteDocumentID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditNoteDocumentRepository extends JpaRepository<CreditNoteDocumentEntity,CreditNoteDocumentID> {

    @Query( value = """
            select sd.* from credit_note_document sd where sd.CreditNoteCod = :CreditNoteCod
            """, nativeQuery = true)
    public CreditNoteDocumentEntity findByCreditNoteCod(@Param("CreditNoteCod") String CreditNoteCod);


    @Query( value = """
            select sd.* from credit_note_document sd where sd.DocumentCod = :DocumentCod
            """, nativeQuery = true)
    public CreditNoteDocumentEntity findByDocumentCod(@Param("DocumentCod") String DocumentCod);
}
