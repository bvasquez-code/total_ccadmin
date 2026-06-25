package com.ccadmin.app.transfer.repository;

import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.id.TransferDetId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferDetRepository extends JpaRepository<TransferDetEntity, TransferDetId> {

    @Query(value = """
            select td.* from transfer_det td
            where td.TransferCod = :transferCod
            and td.TypeOperation = :typeOperation
            and td.Status = 'A'
            order by td.ItemNumber
            """, nativeQuery = true)
    List<TransferDetEntity> findByTransferCodAndTypeOperation(
            @Param("transferCod") String transferCod,
            @Param("typeOperation") String typeOperation
    );

    @Query(value = """
            select td.* from transfer_det td
            where td.TransferCod = :transferCod
            and td.Status = 'A'
            order by td.ItemNumber
            """, nativeQuery = true)
    List<TransferDetEntity> findByTransferCod(
            @Param("transferCod") String transferCod
    );

    @Modifying
    @Transactional
    @Query(value = """
            update transfer_det set Status = :status
            where TransferCod = :transferCod
            and TypeOperation = :typeOperation
            """, nativeQuery = true)
    void updateStatusAll(
            @Param("transferCod") String transferCod,
            @Param("typeOperation") String typeOperation,
            @Param("status") String status
    );
}
