package com.ccadmin.app.sale.repository;

import com.ccadmin.app.sale.model.entity.CreditNoteApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CreditNoteApplicationRepository extends JpaRepository<CreditNoteApplicationEntity, Long> {

    @Query(value = """
            select coalesce(sum(cna.AmountApplied), 0)
            from credit_note_application cna
            where cna.CreditNoteCod = :CreditNoteCod
              and cna.Status = 'A'
            """, nativeQuery = true)
    BigDecimal findTotalApplied(@Param("CreditNoteCod") String creditNoteCod);

    @Query(value = """
            select cna.*
            from credit_note_application cna
            where cna.CreditNoteCod = :CreditNoteCod
              and cna.Status = 'A'
            order by cna.ApplicationId
            """, nativeQuery = true)
    List<CreditNoteApplicationEntity> findActiveByCreditNoteCod(
            @Param("CreditNoteCod") String creditNoteCod
    );

    @Query(value = """
            select cna.*
            from credit_note_application cna
            where cna.SaleCod = :SaleCod
              and cna.Status = 'A'
            order by cna.ApplicationId
            """, nativeQuery = true)
    List<CreditNoteApplicationEntity> findActiveBySaleCod(@Param("SaleCod") String saleCod);
}
