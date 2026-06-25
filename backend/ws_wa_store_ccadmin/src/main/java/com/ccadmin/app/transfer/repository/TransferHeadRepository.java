package com.ccadmin.app.transfer.repository;

import com.ccadmin.app.shared.interfaceccadmin.CcAdminRepository;
import com.ccadmin.app.transfer.model.entity.TransferHeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferHeadRepository extends JpaRepository<TransferHeadEntity, String>, CcAdminRepository<TransferHeadEntity, String> {

    @Query(value = """
            CALL db_store_01.get_cod_trx(:storeCod, 'transfer_head')
            """, nativeQuery = true)
    String getTransferCod(@Param("storeCod") String storeCod);

    @Query(value = """
            select th.* from transfer_head th
            where th.TransferCod = :transferCod
            """, nativeQuery = true)
    List<TransferHeadEntity> findByTransferCod(@Param("transferCod") String transferCod);

    @Query(value = """
            select th.* from transfer_head th
            where th.TransferCod = :transferCod
            and th.TypeOperation = :typeOperation
            """, nativeQuery = true)
    TransferHeadEntity findByTransferCodAndTypeOperation(
            @Param("transferCod") String transferCod,
            @Param("typeOperation") String typeOperation
    );

    @Query(value = """
            select th.* from transfer_head th
            where th.TransferCod = :transferCod
            and th.TypeOperation = :typeOperation
            """, nativeQuery = true)
    TransferHeadEntity findByTransferCodAndTypeOperationForUpdate(
            @Param("transferCod") String transferCod,
            @Param("typeOperation") String typeOperation
    );

    @Query(value = """
            select count(1) from transfer_head th
            where
                (:transferCod = '' or th.TransferCod = :transferCod)
                and (:storeCodOrigin = '' or th.StoreCodOrigin = :storeCodOrigin)
                and (:storeCodDest = '' or th.StoreCodDest = :storeCodDest)
                and (:transferStatus = '' or th.TransferStatus = :transferStatus)
                and (:typeOperation = '' or th.TypeOperation = :typeOperation)
                and (:storeCodRequestedBy = '' or th.StoreCodRequestedBy = :storeCodRequestedBy)
                and (:dateStart is null or th.CreationDate >= :dateStart)
                and (:dateEnd is null or th.CreationDate <= :dateEnd)
            """, nativeQuery = true)
    int countByFilters(
            @Param("transferCod") String transferCod,
            @Param("storeCodOrigin") String storeCodOrigin,
            @Param("storeCodDest") String storeCodDest,
            @Param("transferStatus") String transferStatus,
            @Param("typeOperation") String typeOperation,
            @Param("storeCodRequestedBy") String storeCodRequestedBy,
            @Param("dateStart") String dateStart,
            @Param("dateEnd") String dateEnd
    );

    @Query(value = """
            select th.* from transfer_head th
            where
                (:transferCod = '' or th.TransferCod = :transferCod)
                and (:storeCodOrigin = '' or th.StoreCodOrigin = :storeCodOrigin)
                and (:storeCodDest = '' or th.StoreCodDest = :storeCodDest)
                and (:transferStatus = '' or th.TransferStatus = :transferStatus)
                and (:typeOperation = '' or th.TypeOperation = :typeOperation)
                and (:storeCodRequestedBy = '' or th.StoreCodRequestedBy = :storeCodRequestedBy)
                and (:dateStart is null or th.CreationDate >= :dateStart)
                and (:dateEnd is null or th.CreationDate <= :dateEnd)
            order by th.CreationDate desc
            limit :init,:limit
            """, nativeQuery = true)
    List<TransferHeadEntity> findByFilters(
            @Param("transferCod") String transferCod,
            @Param("storeCodOrigin") String storeCodOrigin,
            @Param("storeCodDest") String storeCodDest,
            @Param("transferStatus") String transferStatus,
            @Param("typeOperation") String typeOperation,
            @Param("storeCodRequestedBy") String storeCodRequestedBy,
            @Param("dateStart") String dateStart,
            @Param("dateEnd") String dateEnd,
            @Param("init") int init,
            @Param("limit") int limit
    );
}
