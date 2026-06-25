package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.id.CreditNoteDetID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditNoteDetRepository extends JpaRepository<CreditNoteDetEntity, CreditNoteDetID> {


    @Query(value = """
            SELECT cnd.* FROM credit_note_det cnd WHERE cnd.CreditNoteCod = :CreditNoteCod AND cnd.Status = 'A'
            """, nativeQuery = true)
    public List<CreditNoteDetEntity> findByCreditNoteCod(@Param("CreditNoteCod") String CreditNoteCod);

    @Modifying
    @Query(value = """
            update credit_note_det set Status = :Status where CreditNoteCod = :CreditNoteCod
            """, nativeQuery = true)
    public void updateStatusAll(
             @Param("CreditNoteCod") String CreditNoteCod
            ,@Param("Status") String Status
    );

}
