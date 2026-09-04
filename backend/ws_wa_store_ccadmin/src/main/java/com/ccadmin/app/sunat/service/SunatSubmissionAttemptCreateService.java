package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatSubmissionConstants;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptResultDto;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptStartDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatProcessRequestDto;
import com.ccadmin.app.sunat.model.entity.SunatSubmissionEntity;
import com.ccadmin.app.sunat.repository.SunatSubmissionRepository;
import com.ccadmin.app.system.shared.TableSequenceShared;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class SunatSubmissionAttemptCreateService {

    private static final String SEQUENCE_TABLE_TYPE = "sunat_submission";
    private static final String SYSTEM_USER = "SISTEMA";

    private final SunatSubmissionRepository sunatSubmissionRepository;
    private final TableSequenceShared tableSequenceShared;

    public SunatSubmissionAttemptCreateService(
            SunatSubmissionRepository sunatSubmissionRepository,
            TableSequenceShared tableSequenceShared
    ) {
        this.sunatSubmissionRepository = sunatSubmissionRepository;
        this.tableSequenceShared = tableSequenceShared;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String generateCode() {
        return this.tableSequenceShared.getNextCode(SEQUENCE_TABLE_TYPE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SunatSubmissionAttemptStartDto beginInitialAttempt(
            String candidateCode,
            String requestType,
            String endpointKey,
            String sunatDocumentType,
            SunatProcessRequestDto request,
            String payloadJson
    ) {
        validateRequest(candidateCode, requestType, endpointKey, sunatDocumentType, request, payloadJson);
        String userCod = auditUser(request.AuditUserCod);
        SunatSubmissionEntity submission = this.sunatSubmissionRepository.findBySourceForUpdate(
                request.SourceModule,
                request.SourceDocumentCod,
                sunatDocumentType
        ).orElse(null);

        if (submission != null && "A".equals(submission.Status)
                && (SunatSubmissionConstants.SEND_STATUS_SENT.equals(submission.SendStatus)
                || SunatSubmissionConstants.SEND_STATUS_SENDING.equals(submission.SendStatus))) {
            return new SunatSubmissionAttemptStartDto(submission, false);
        }

        boolean isNew = submission == null;
        if (isNew) {
            submission = new SunatSubmissionEntity();
            submission.SunatSubmissionCod = candidateCode;
            submission.StoreCod = request.StoreCod;
            submission.SourceModule = request.SourceModule;
            submission.SourceDocumentCod = request.SourceDocumentCod;
            submission.SourceDocumentType = request.SourceDocumentType;
            submission.SunatDocumentType = sunatDocumentType;
            submission.AttemptCount = 0;
        }

        submission.StoreCod = request.StoreCod;
        submission.SourceDocumentType = request.SourceDocumentType;
        submission.Series = request.Series;
        submission.Correlative = request.Correlative;
        submission.RequestType = requestType;
        submission.EndpointKey = endpointKey;
        submission.PayloadJson = payloadJson;
        submission.Status = "A";
        startAttempt(submission, userCod, isNew);
        this.sunatSubmissionRepository.save(submission);
        return new SunatSubmissionAttemptStartDto(submission, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SunatSubmissionEntity beginRetry(String sunatSubmissionCod, String userCod) {
        SunatSubmissionEntity submission = findForUpdate(sunatSubmissionCod);
        if (!SunatSubmissionConstants.SEND_STATUS_ERROR.equals(submission.SendStatus)
                && !SunatSubmissionConstants.SEND_STATUS_PENDING.equals(submission.SendStatus)
                && !isStalledSending(submission)) {
            throw new IllegalStateException(
                    "Solo se pueden reenviar documentos pendientes, con error o envios detenidos"
            );
        }
        startAttempt(submission, auditUser(userCod), false);
        return this.sunatSubmissionRepository.save(submission);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SunatSubmissionEntity finishAttempt(
            String sunatSubmissionCod,
            String userCod,
            SunatSubmissionAttemptResultDto result
    ) {
        if (result == null) {
            throw new IllegalArgumentException("Resultado del intento SUNAT requerido");
        }
        SunatSubmissionEntity submission = findForUpdate(sunatSubmissionCod);
        submission.SendStatus = result.Successful
                ? SunatSubmissionConstants.SEND_STATUS_SENT
                : SunatSubmissionConstants.SEND_STATUS_ERROR;
        submission.SunatStatus = cleanToNull(result.SunatStatus);
        submission.RemoteSunatDocumentCod = cleanToNull(result.RemoteSunatDocumentCod);
        submission.SunatTicket = cleanToNull(result.SunatTicket);
        submission.LastResponseStatus = cleanToNull(result.ResponseStatus);
        submission.LastResponseJson = cleanToNull(result.ResponseJson);
        submission.LastErrorReason = result.Successful ? null : errorReason(result.ErrorReason);
        if (result.Successful) {
            submission.LastSuccessDate = new Date();
        }
        submission.addSessionModify(auditUser(userCod));
        return this.sunatSubmissionRepository.save(submission);
    }

    private void startAttempt(SunatSubmissionEntity submission, String userCod, boolean isNew) {
        submission.SendStatus = SunatSubmissionConstants.SEND_STATUS_SENDING;
        submission.AttemptCount = value(submission.AttemptCount) + 1;
        submission.LastAttemptDate = new Date();
        submission.LastAttemptUser = userCod;
        submission.SunatStatus = null;
        submission.RemoteSunatDocumentCod = null;
        submission.SunatTicket = null;
        submission.LastResponseStatus = null;
        submission.LastResponseJson = null;
        submission.LastErrorReason = null;
        submission.addSession(userCod, isNew);
    }

    private SunatSubmissionEntity findForUpdate(String sunatSubmissionCod) {
        String code = clean(sunatSubmissionCod);
        if (code.isBlank()) {
            throw new IllegalArgumentException("Codigo de envio SUNAT requerido");
        }
        SunatSubmissionEntity submission = this.sunatSubmissionRepository.findForUpdate(code);
        if (submission == null || !"A".equals(submission.Status)) {
            throw new IllegalArgumentException("No existe el envio SUNAT indicado");
        }
        return submission;
    }

    private void validateRequest(
            String candidateCode,
            String requestType,
            String endpointKey,
            String sunatDocumentType,
            SunatProcessRequestDto request,
            String payloadJson
    ) {
        if (clean(candidateCode).isBlank()) {
            throw new IllegalArgumentException("No se pudo generar el codigo del envio SUNAT");
        }
        if (request == null) {
            throw new IllegalArgumentException("Documento requerido para enviar a SUNAT");
        }
        if (clean(request.StoreCod).isBlank()) {
            throw new IllegalArgumentException("Local requerido para registrar el envio SUNAT");
        }
        if (clean(request.SourceModule).isBlank()
                || clean(request.SourceDocumentCod).isBlank()
                || clean(request.SourceDocumentType).isBlank()) {
            throw new IllegalArgumentException("Documento origen requerido para registrar el envio SUNAT");
        }
        if (clean(request.Series).isBlank() || request.Correlative <= 0) {
            throw new IllegalArgumentException("Serie y correlativo requeridos para registrar el envio SUNAT");
        }
        if (clean(requestType).isBlank() || clean(endpointKey).isBlank()
                || clean(sunatDocumentType).isBlank()) {
            throw new IllegalArgumentException("Tipo de envio SUNAT requerido");
        }
        if (clean(payloadJson).isBlank()) {
            throw new IllegalArgumentException("Payload requerido para registrar el envio SUNAT");
        }
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isStalledSending(SunatSubmissionEntity submission) {
        if (!SunatSubmissionConstants.SEND_STATUS_SENDING.equals(submission.SendStatus)) {
            return false;
        }
        return submission.LastAttemptDate == null
                || submission.LastAttemptDate.getTime()
                <= System.currentTimeMillis()
                - SunatSubmissionConstants.SENDING_RETRY_DELAY_MILLIS;
    }

    private String auditUser(String userCod) {
        String cleanUser = clean(userCod);
        return cleanUser.isBlank() ? SYSTEM_USER : cleanUser;
    }

    private String errorReason(String reason) {
        String cleanReason = clean(reason);
        return cleanReason.isBlank() ? "El intento de envio SUNAT no concluyo correctamente" : cleanReason;
    }

    private String cleanToNull(String value) {
        String cleanValue = clean(value);
        return cleanValue.isBlank() ? null : cleanValue;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
