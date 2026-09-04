package com.ccadmin.app.sunat.repository;

import com.ccadmin.app.sunat.model.entity.SunatSubmissionEntity;
import com.ccadmin.app.sunat.model.idto.ISunatSubmissionSearchDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SunatSubmissionRepository extends JpaRepository<SunatSubmissionEntity, String> {

    @Query(value = """
            select *
            from sunat_submission
            where SunatSubmissionCod = :sunatSubmissionCod
            for update
            """, nativeQuery = true)
    SunatSubmissionEntity findForUpdate(
            @Param("sunatSubmissionCod") String sunatSubmissionCod
    );

    @Query(value = """
            select *
            from sunat_submission
            where SourceModule = :sourceModule
              and SourceDocumentCod = :sourceDocumentCod
              and SunatDocumentType = :sunatDocumentType
            limit 1
            for update
            """, nativeQuery = true)
    Optional<SunatSubmissionEntity> findBySourceForUpdate(
            @Param("sourceModule") String sourceModule,
            @Param("sourceDocumentCod") String sourceDocumentCod,
            @Param("sunatDocumentType") String sunatDocumentType
    );

    @Query(value = """
            select count(1)
            from sunat_submission submission
            where submission.Status = 'A'
              and (:query = ''
                   or submission.SunatSubmissionCod like concat('%', :query, '%')
                   or submission.SourceDocumentCod like concat('%', :query, '%')
                   or submission.Series like concat('%', :query, '%')
                   or submission.RemoteSunatDocumentCod like concat('%', :query, '%')
                   or submission.SunatTicket like concat('%', :query, '%'))
              and (:storeCod = '' or submission.StoreCod = :storeCod)
              and (:requestType = '' or submission.RequestType = :requestType)
              and (:sendStatus = '' or submission.SendStatus = :sendStatus)
              and (:dateStart is null or date(submission.CreationDate) >= :dateStart)
              and (:dateEnd is null or date(submission.CreationDate) <= :dateEnd)
            """, nativeQuery = true)
    int countSearch(
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("requestType") String requestType,
            @Param("sendStatus") String sendStatus,
            @Param("dateStart") String dateStart,
            @Param("dateEnd") String dateEnd
    );

    @Query(value = """
            select submission.SunatSubmissionCod as SunatSubmissionCod,
                   submission.StoreCod as StoreCod,
                   store.Name as StoreName,
                   submission.SourceModule as SourceModule,
                   submission.SourceDocumentCod as SourceDocumentCod,
                   submission.SourceDocumentType as SourceDocumentType,
                   submission.SunatDocumentType as SunatDocumentType,
                   submission.Series as Series,
                   submission.Correlative as Correlative,
                   submission.RequestType as RequestType,
                   submission.SendStatus as SendStatus,
                   submission.SunatStatus as SunatStatus,
                   submission.RemoteSunatDocumentCod as RemoteSunatDocumentCod,
                   submission.SunatTicket as SunatTicket,
                   submission.AttemptCount as AttemptCount,
                   submission.LastAttemptDate as LastAttemptDate,
                   submission.LastSuccessDate as LastSuccessDate,
                   submission.LastAttemptUser as LastAttemptUser,
                   submission.LastResponseStatus as LastResponseStatus,
                   submission.LastErrorReason as LastErrorReason,
                   submission.CreationUser as CreationUser,
                   submission.CreationDate as CreationDate,
                   submission.ModifyUser as ModifyUser,
                   submission.ModifyDate as ModifyDate
            from sunat_submission submission
            join store on store.StoreCod = submission.StoreCod
            where submission.Status = 'A'
              and (:query = ''
                   or submission.SunatSubmissionCod like concat('%', :query, '%')
                   or submission.SourceDocumentCod like concat('%', :query, '%')
                   or submission.Series like concat('%', :query, '%')
                   or submission.RemoteSunatDocumentCod like concat('%', :query, '%')
                   or submission.SunatTicket like concat('%', :query, '%'))
              and (:storeCod = '' or submission.StoreCod = :storeCod)
              and (:requestType = '' or submission.RequestType = :requestType)
              and (:sendStatus = '' or submission.SendStatus = :sendStatus)
              and (:dateStart is null or date(submission.CreationDate) >= :dateStart)
              and (:dateEnd is null or date(submission.CreationDate) <= :dateEnd)
            order by submission.CreationDate desc
            limit :init, :limit
            """, nativeQuery = true)
    List<ISunatSubmissionSearchDto> search(
            @Param("query") String query,
            @Param("storeCod") String storeCod,
            @Param("requestType") String requestType,
            @Param("sendStatus") String sendStatus,
            @Param("dateStart") String dateStart,
            @Param("dateEnd") String dateEnd,
            @Param("init") int init,
            @Param("limit") int limit
    );

    @Query(value = """
            select submission.SunatSubmissionCod as SunatSubmissionCod,
                   submission.StoreCod as StoreCod,
                   store.Name as StoreName,
                   submission.SourceModule as SourceModule,
                   submission.SourceDocumentCod as SourceDocumentCod,
                   submission.SourceDocumentType as SourceDocumentType,
                   submission.SunatDocumentType as SunatDocumentType,
                   submission.Series as Series,
                   submission.Correlative as Correlative,
                   submission.RequestType as RequestType,
                   submission.SendStatus as SendStatus,
                   submission.SunatStatus as SunatStatus,
                   submission.RemoteSunatDocumentCod as RemoteSunatDocumentCod,
                   submission.SunatTicket as SunatTicket,
                   submission.AttemptCount as AttemptCount,
                   submission.LastAttemptDate as LastAttemptDate,
                   submission.LastSuccessDate as LastSuccessDate,
                   submission.LastAttemptUser as LastAttemptUser,
                   submission.LastResponseStatus as LastResponseStatus,
                   submission.LastErrorReason as LastErrorReason,
                   submission.CreationUser as CreationUser,
                   submission.CreationDate as CreationDate,
                   submission.ModifyUser as ModifyUser,
                   submission.ModifyDate as ModifyDate
            from sunat_submission submission
            join store on store.StoreCod = submission.StoreCod
            where submission.SunatSubmissionCod = :sunatSubmissionCod
              and submission.Status = 'A'
            limit 1
            """, nativeQuery = true)
    Optional<ISunatSubmissionSearchDto> findSearchById(
            @Param("sunatSubmissionCod") String sunatSubmissionCod
    );
}
