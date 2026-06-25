package com.ccadmin.app.transfer.repository;

import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferDocumentRepository extends JpaRepository<TransferDocumentEntity, String> {

    @Query(value = """
            select td.* from transfer_document td
            where td.TransferCod = :transferCod
            and td.TypeOperation = :typeOperation
            and td.Status = 'A'
            """, nativeQuery = true)
    List<TransferDocumentEntity> findByTransferCodAndTypeOperation(
            @Param("transferCod") String transferCod,
            @Param("typeOperation") String typeOperation
    );


    @Query(value = """
            select td.* from transfer_document td
            where td.TransferCod = :transferCod
            and td.Status = 'A'
            """, nativeQuery = true)
    List<TransferDocumentEntity> findByTransferCod(
            @Param("transferCod") String transferCod
    );
}
