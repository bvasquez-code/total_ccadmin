package com.ccadmin.app.sunat.repository;

import com.ccadmin.app.sunat.model.entity.SunatDocumentFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SunatDocumentFileRepository extends JpaRepository<SunatDocumentFileEntity, String> {

    @Query(value = """
            select sdf.*
            from sunat_document_file sdf
            where sdf.SunatDocumentCod = :sunatDocumentCod
              and sdf.Status = 'A'
            order by sdf.CreationDate desc
            """, nativeQuery = true)
    List<SunatDocumentFileEntity> findBySunatDocumentCod(@Param("sunatDocumentCod") String sunatDocumentCod);

    @Query(value = """
            select sdf.*
            from sunat_document_file sdf
            where sdf.SunatDocumentCod = :sunatDocumentCod
              and sdf.FileType = :fileType
              and sdf.Status = 'A'
            order by sdf.CreationDate desc
            limit 1
            """, nativeQuery = true)
    Optional<SunatDocumentFileEntity> findLastByDocumentAndType(
            @Param("sunatDocumentCod") String sunatDocumentCod,
            @Param("fileType") String fileType
    );
}
