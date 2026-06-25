package com.ccadmin.app.transfer.repository;

import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
import com.ccadmin.app.transfer.model.entity.id.TransferRequestDetId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferRequestDetRepository extends JpaRepository<TransferRequestDetEntity, TransferRequestDetId> {

    @Query(value = """
            select td.* from transfer_request_det td
            where td.TransferReqCod = :TransferReqCod
            and td.TypeOperation = :typeOperation
            and td.Status = 'A'
            order by td.ItemNumber
            """, nativeQuery = true)
    List<TransferRequestDetEntity> findByTransferCodAndTypeOperation(
            @Param("TransferReqCod") String TransferReqCod,
            @Param("typeOperation") String typeOperation
    );

    @Query(value = """
            select td.* from transfer_request_det td
            where td.TransferReqCod = :TransferReqCod
            and td.Status = 'A'
            order by td.ItemNumber
            """, nativeQuery = true)
    List<TransferRequestDetEntity> findByTransferCod(
            @Param("TransferReqCod") String TransferReqCod
    );

    @Modifying
    @Transactional
    @Query(value = """
            update transfer_request_det set Status = :status
            where TransferReqCod = :TransferReqCod
            and TypeOperation = :typeOperation
            """, nativeQuery = true)
    void updateStatusAll(
            @Param("TransferReqCod") String TransferReqCod,
            @Param("typeOperation") String typeOperation,
            @Param("status") String status
    );
}
