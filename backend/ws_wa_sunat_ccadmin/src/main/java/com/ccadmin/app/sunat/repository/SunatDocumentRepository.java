package com.ccadmin.app.sunat.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.sunat.model.entity.SunatDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SunatDocumentRepository extends JpaRepository<SunatDocumentEntity, String>,
        CcAdminRepository<SunatDocumentEntity, String> {

    @Query(value = """
            select sd.*
            from sunat_document sd
            where sd.SourceModule = :sourceModule
              and sd.SourceDocumentCod = :sourceDocumentCod
              and sd.SunatDocumentType = :sunatDocumentType
              and sd.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<SunatDocumentEntity> findBySource(
            @Param("sourceModule") String sourceModule,
            @Param("sourceDocumentCod") String sourceDocumentCod,
            @Param("sunatDocumentType") String sunatDocumentType
    );

    @Query(value = """
            select sd.*
            from sunat_document sd
            where sd.ElectronicStatus in ('TCK', 'RET', 'ERR')
              and sd.Status = 'A'
            order by sd.CreationDate asc
            """, nativeQuery = true)
    List<SunatDocumentEntity> findPendingProcess();

    @Override
    @Query(value = """
            select count(1)
            from sunat_document sd
            where sd.SunatDocumentCod = :id
               or sd.SourceDocumentCod like %:query%
               or sd.FullDocumentNumber like %:query%
               or sd.IssuerRuc like %:query%
               or sd.TicketSunat like %:query%
               or sd.ElectronicStatus like %:query%
            """, nativeQuery = true)
    int countByQueryText(@Param("id") String id, @Param("query") String query);

    @Override
    @Query(value = """
            select sd.*
            from sunat_document sd
            where sd.SunatDocumentCod = :id
               or sd.SourceDocumentCod like %:query%
               or sd.FullDocumentNumber like %:query%
               or sd.IssuerRuc like %:query%
               or sd.TicketSunat like %:query%
               or sd.ElectronicStatus like %:query%
            order by sd.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<SunatDocumentEntity> findByQueryText(
            @Param("id") String id,
            @Param("query") String query,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
