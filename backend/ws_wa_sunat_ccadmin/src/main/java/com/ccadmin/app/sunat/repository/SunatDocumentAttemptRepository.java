package com.ccadmin.app.sunat.repository;

import com.ccadmin.app.sunat.model.entity.SunatDocumentAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SunatDocumentAttemptRepository extends JpaRepository<SunatDocumentAttemptEntity, Long> {

    @Query(value = """
            select sda.*
            from sunat_document_attempt sda
            where sda.SunatDocumentCod = :sunatDocumentCod
            order by sda.CreationDate desc
            """, nativeQuery = true)
    List<SunatDocumentAttemptEntity> findBySunatDocumentCod(@Param("sunatDocumentCod") String sunatDocumentCod);
}
