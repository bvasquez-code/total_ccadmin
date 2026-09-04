package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatClientConstants;
import com.ccadmin.app.sunat.model.constants.SunatSubmissionConstants;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptResultDto;
import com.ccadmin.app.sunat.model.dto.SunatSubmissionAttemptStartDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatCreditNoteProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatDebitNoteProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatDespatchAdviceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatInvoiceProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatReceiptProcessRequestDto;
import com.ccadmin.app.sunat.model.dto.sunat.SunatWsResponseDto;
import com.ccadmin.app.sunat.model.entity.SunatSubmissionEntity;
import com.ccadmin.app.shared.model.dto.UrlDataDto;
import com.ccadmin.app.shared.shared.UrlSearchShared;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class SaleSunatClientService {

    private final UrlSearchShared urlSearchShared;
    private final SunatSubmissionAttemptCreateService sunatSubmissionAttemptCreateService;
    private final SunatSubmissionSearchService sunatSubmissionSearchService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public SaleSunatClientService(
            UrlSearchShared urlSearchShared,
            SunatSubmissionAttemptCreateService sunatSubmissionAttemptCreateService,
            SunatSubmissionSearchService sunatSubmissionSearchService,
            ObjectMapper objectMapper,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.urlSearchShared = urlSearchShared;
        this.sunatSubmissionAttemptCreateService = sunatSubmissionAttemptCreateService;
        this.sunatSubmissionSearchService = sunatSubmissionSearchService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder.build();
    }


    private void validateResponse(SunatWsResponseDto response, String operation) {
        if (response == null) {
            throw new IllegalArgumentException("SUNAT sin respuesta en " + operation);
        }
        if (response.ErrorStatus || !SunatClientConstants.RESPONSE_STATUS_OK.equals(response.Status)) {
            throw new IllegalArgumentException("SUNAT error en " + operation + ": " + response.Message);
        }
    }


    public SunatWsResponseDto processInvoice(SunatInvoiceProcessRequestDto request) {
        return this.postProcess(
                SunatClientConstants.URL_KEY_INVOICE,
                SunatClientConstants.MESSAGE_INACTIVE_INVOICE_URL,
                SunatSubmissionConstants.REQUEST_TYPE_INVOICE,
                "01",
                request
        );
    }

    public SunatWsResponseDto processReceipt(SunatReceiptProcessRequestDto request) {
        return this.postProcess(
                SunatClientConstants.URL_KEY_RECEIPT,
                SunatClientConstants.MESSAGE_INACTIVE_RECEIPT_URL,
                SunatSubmissionConstants.REQUEST_TYPE_RECEIPT,
                "03",
                request
        );
    }

    public SunatWsResponseDto processCreditNote(SunatCreditNoteProcessRequestDto request) {
        return this.postProcess(
                SunatClientConstants.URL_KEY_CREDIT_NOTE,
                SunatClientConstants.MESSAGE_INACTIVE_CREDIT_NOTE_URL,
                SunatSubmissionConstants.REQUEST_TYPE_CREDIT_NOTE,
                "07",
                request
        );
    }

    public SunatWsResponseDto processDebitNote(SunatDebitNoteProcessRequestDto request) {
        return this.postProcess(
                SunatClientConstants.URL_KEY_DEBIT_NOTE,
                SunatClientConstants.MESSAGE_INACTIVE_DEBIT_NOTE_URL,
                SunatSubmissionConstants.REQUEST_TYPE_DEBIT_NOTE,
                "08",
                request
        );
    }

    public SunatWsResponseDto processDespatchAdvice(SunatDespatchAdviceProcessRequestDto request) {
        return this.postProcess(
                SunatClientConstants.URL_KEY_DESPATCH_ADVICE,
                SunatClientConstants.MESSAGE_INACTIVE_DESPATCH_ADVICE_URL,
                SunatSubmissionConstants.REQUEST_TYPE_DESPATCH_ADVICE,
                "09",
                request
        );
    }

    public SunatWsResponseDto retrySubmission(String sunatSubmissionCod, String userCod) {
        SunatSubmissionEntity current = this.sunatSubmissionSearchService
                .findEntityById(sunatSubmissionCod);
        SunatProcessRequestDto request = deserializeRequest(current);
        SunatSubmissionEntity submission = this.sunatSubmissionAttemptCreateService
                .beginRetry(current.SunatSubmissionCod, userCod);
        return executeAttempt(
                submission,
                request,
                inactiveMessage(submission.EndpointKey),
                userCod
        );
    }

    private SunatWsResponseDto postProcess(
            String urlKey,
            String inactiveMessage,
            String requestType,
            String sunatDocumentType,
            SunatProcessRequestDto request
    ) {
        try {
            String payloadJson = this.objectMapper.writeValueAsString(request);
            String candidateCode = this.sunatSubmissionAttemptCreateService.generateCode();
            SunatSubmissionAttemptStartDto attempt = this.sunatSubmissionAttemptCreateService
                    .beginInitialAttempt(
                            candidateCode,
                            requestType,
                            urlKey,
                            sunatDocumentType,
                            request,
                            payloadJson
                    );
            if (!attempt.sendRequired()) {
                return previousResponse(attempt.submission());
            }
            return executeAttempt(
                    attempt.submission(),
                    request,
                    inactiveMessage,
                    request.AuditUserCod
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "No se pudo preparar el envio SUNAT: " + errorMessage(exception),
                    exception
            );
        }
    }

    private SunatWsResponseDto executeAttempt(
            SunatSubmissionEntity submission,
            SunatProcessRequestDto request,
            String inactiveMessage,
            String userCod
    ) {
        boolean resultPersisted = false;
        try {
            UrlDataDto urlData = this.urlSearchShared.findUrlDtaSunat(submission.EndpointKey);
            if (!SunatClientConstants.URL_STATUS_ACTIVE.equals(urlData.status)) {
                SunatWsResponseDto response = SunatWsResponseDto.alert(inactiveMessage);
                finishAttempt(submission, userCod, response, false, inactiveMessage);
                return response;
            }

            ResponseEntity<SunatWsResponseDto> httpResponse = this.restTemplate.postForEntity(
                    urlData.urlAddress,
                    request,
                    SunatWsResponseDto.class
            );
            SunatWsResponseDto response = httpResponse.getBody();
            SunatSubmissionAttemptResultDto result = responseResult(response);
            this.sunatSubmissionAttemptCreateService.finishAttempt(
                    submission.SunatSubmissionCod,
                    userCod,
                    result
            );
            resultPersisted = true;
            this.validateResponse(response, SunatClientConstants.OPERATION_PROCESS);
            return response;
        } catch (Exception exception) {
            if (!resultPersisted) {
                SunatSubmissionAttemptResultDto result = exceptionResult(exception);
                try {
                    this.sunatSubmissionAttemptCreateService.finishAttempt(
                            submission.SunatSubmissionCod,
                            userCod,
                            result
                    );
                } catch (Exception persistenceException) {
                    log.error(
                            "No se pudo registrar el resultado fallido del envio SUNAT {}",
                            submission.SunatSubmissionCod,
                            persistenceException
                    );
                }
            }
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException(
                    "No se pudo enviar el documento a SUNAT: " + errorMessage(exception),
                    exception
            );
        }
    }

    private void finishAttempt(
            SunatSubmissionEntity submission,
            String userCod,
            SunatWsResponseDto response,
            boolean successful,
            String errorReason
    ) throws Exception {
        SunatSubmissionAttemptResultDto result = responseResult(response);
        result.Successful = successful;
        result.ErrorReason = errorReason;
        this.sunatSubmissionAttemptCreateService.finishAttempt(
                submission.SunatSubmissionCod,
                userCod,
                result
        );
    }

    private SunatSubmissionAttemptResultDto responseResult(
            SunatWsResponseDto response
    ) throws Exception {
        SunatSubmissionAttemptResultDto result = new SunatSubmissionAttemptResultDto();
        JsonNode data = this.objectMapper.valueToTree(response == null ? null : response.Data);
        boolean hasProcessed = data != null && data.isObject() && data.has("Processed");
        result.Successful = response != null
                && !response.ErrorStatus
                && SunatClientConstants.RESPONSE_STATUS_OK.equals(response.Status)
                && (!hasProcessed || data.path("Processed").asBoolean(false));
        result.ResponseStatus = response == null ? null : response.Status;
        result.ResponseJson = response == null
                ? null
                : this.objectMapper.writeValueAsString(response);
        result.SunatStatus = text(data, "ElectronicStatus");
        result.RemoteSunatDocumentCod = text(data, "SunatDocumentCod");
        result.SunatTicket = text(data, "TicketSunat");
        if (!result.Successful) {
            result.ErrorReason = firstNotBlank(
                    text(data, "LastFunctionalError"),
                    text(data, "LastTechnicalError"),
                    text(data, "Message"),
                    response == null ? null : response.Message,
                    "El servicio SUNAT no confirmo el procesamiento del documento"
            );
        }
        return result;
    }

    private SunatSubmissionAttemptResultDto exceptionResult(Exception exception) {
        SunatSubmissionAttemptResultDto result = new SunatSubmissionAttemptResultDto();
        result.Successful = false;
        result.ErrorReason = errorMessage(exception);
        if (exception instanceof RestClientResponseException responseException) {
            result.ResponseStatus = String.valueOf(responseException.getStatusCode().value());
            result.ResponseJson = responseException.getResponseBodyAsString();
            enrichFromResponseBody(result, responseException.getResponseBodyAsString());
        }
        return result;
    }

    private void enrichFromResponseBody(
            SunatSubmissionAttemptResultDto result,
            String responseBody
    ) {
        if (responseBody == null || responseBody.isBlank()) {
            return;
        }
        try {
            JsonNode responseNode = this.objectMapper.readTree(responseBody);
            JsonNode data = responseNode.path("Data");
            result.SunatStatus = text(data, "ElectronicStatus");
            result.RemoteSunatDocumentCod = text(data, "SunatDocumentCod");
            result.SunatTicket = text(data, "TicketSunat");
            result.ErrorReason = firstNotBlank(
                    text(data, "LastFunctionalError"),
                    text(data, "LastTechnicalError"),
                    text(data, "Message"),
                    text(responseNode, "Message"),
                    text(responseNode, "message"),
                    result.ErrorReason
            );
        } catch (Exception parseException) {
            log.debug("La respuesta HTTP SUNAT no tiene formato JSON", parseException);
        }
    }

    private SunatProcessRequestDto deserializeRequest(SunatSubmissionEntity submission) {
        try {
            Class<? extends SunatProcessRequestDto> requestClass = switch (submission.RequestType) {
                case SunatSubmissionConstants.REQUEST_TYPE_INVOICE -> SunatInvoiceProcessRequestDto.class;
                case SunatSubmissionConstants.REQUEST_TYPE_RECEIPT -> SunatReceiptProcessRequestDto.class;
                case SunatSubmissionConstants.REQUEST_TYPE_CREDIT_NOTE -> SunatCreditNoteProcessRequestDto.class;
                case SunatSubmissionConstants.REQUEST_TYPE_DEBIT_NOTE -> SunatDebitNoteProcessRequestDto.class;
                case SunatSubmissionConstants.REQUEST_TYPE_DESPATCH_ADVICE -> SunatDespatchAdviceProcessRequestDto.class;
                default -> throw new IllegalArgumentException(
                        "Tipo de payload SUNAT no soportado: " + submission.RequestType
                );
            };
            return this.objectMapper.readValue(submission.PayloadJson, requestClass);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "No se pudo recuperar el payload guardado para el reenvio SUNAT: "
                            + errorMessage(exception),
                    exception
            );
        }
    }

    private SunatWsResponseDto previousResponse(SunatSubmissionEntity submission) {
        if (submission.LastResponseJson != null && !submission.LastResponseJson.isBlank()) {
            try {
                return this.objectMapper.readValue(
                        submission.LastResponseJson,
                        SunatWsResponseDto.class
                );
            } catch (Exception exception) {
                log.warn(
                        "No se pudo reconstruir la respuesta previa SUNAT {}",
                        submission.SunatSubmissionCod,
                        exception
                );
            }
        }
        return SunatWsResponseDto.alert(
                SunatSubmissionConstants.SEND_STATUS_SENDING.equals(submission.SendStatus)
                        ? "El documento ya tiene un envio SUNAT en proceso"
                        : "El documento ya fue enviado a SUNAT"
        );
    }

    private String inactiveMessage(String endpointKey) {
        return switch (endpointKey) {
            case SunatClientConstants.URL_KEY_INVOICE -> SunatClientConstants.MESSAGE_INACTIVE_INVOICE_URL;
            case SunatClientConstants.URL_KEY_RECEIPT -> SunatClientConstants.MESSAGE_INACTIVE_RECEIPT_URL;
            case SunatClientConstants.URL_KEY_CREDIT_NOTE -> SunatClientConstants.MESSAGE_INACTIVE_CREDIT_NOTE_URL;
            case SunatClientConstants.URL_KEY_DEBIT_NOTE -> SunatClientConstants.MESSAGE_INACTIVE_DEBIT_NOTE_URL;
            case SunatClientConstants.URL_KEY_DESPATCH_ADVICE -> SunatClientConstants.MESSAGE_INACTIVE_DESPATCH_ADVICE_URL;
            default -> "Url de procesamiento SUNAT inactiva";
        };
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.isObject() || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? null : value;
    }

    private String errorMessage(Exception exception) {
        if (exception == null) {
            return "Error no especificado";
        }
        return firstNotBlank(exception.getMessage(), exception.getClass().getSimpleName());
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

}
