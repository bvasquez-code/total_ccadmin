package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.constants.SunatElectronicStatusConst;
import com.ccadmin.app.sunat.model.constants.SunatErrorTypeConst;
import com.ccadmin.app.sunat.model.constants.SunatFileTypeConst;
import com.ccadmin.app.sunat.model.constants.SunatOperationTypeConst;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.model.dto.SunatFileProcessResultDto;
import com.ccadmin.app.sunat.model.dto.SunatPendingOperationDto;
import com.ccadmin.app.sunat.model.dto.SunatCdrResultDto;
import com.ccadmin.app.sunat.model.dto.SunatSendResultDto;
import com.ccadmin.app.sunat.model.dto.SunatSoapResponseDto;
import com.ccadmin.app.sunat.model.dto.SunatXmlGenerateResultDto;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentAttemptEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentFileEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentPayloadEntity;
import com.ccadmin.app.sunat.repository.SunatDocumentAttemptRepository;
import com.ccadmin.app.sunat.repository.SunatDocumentPayloadRepository;
import com.ccadmin.app.sunat.repository.SunatDocumentRepository;
import com.ccadmin.app.sunat.repository.SunatConfigRepository;
import com.ccadmin.app.sunat.repository.SunatDocumentFileRepository;
import com.ccadmin.app.sunat.utility.SunatDocumentNameUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class SunatDocumentOperationService extends SessionService {

    @Autowired
    private SunatXmlValidationService sunatXmlValidationService;

    @Autowired
    private SunatUblXmlBuildService sunatUblXmlBuildService;

    @Autowired
    private SunatDocumentCreateService sunatDocumentCreateService;

    @Autowired
    private SunatDocumentRepository sunatDocumentRepository;

    @Autowired
    private SunatConfigRepository sunatConfigRepository;

    @Autowired
    private SunatDocumentPayloadRepository sunatDocumentPayloadRepository;

    @Autowired
    private SunatDocumentFileRepository sunatDocumentFileRepository;

    @Autowired
    private SunatDocumentAttemptRepository sunatDocumentAttemptRepository;

    @Autowired
    private SunatDocumentAttemptCreateService sunatDocumentAttemptCreateService;

    @Autowired
    private SunatFileStorageService sunatFileStorageService;

    @Autowired
    private SunatXmlSignatureService sunatXmlSignatureService;

    @Autowired
    private SunatZipService sunatZipService;

    @Autowired
    private SunatSoapClientService sunatSoapClientService;

    @Autowired
    private SunatCdrProcessService sunatCdrProcessService;

    @Autowired
    private ObjectMapper objectMapper;

    public SunatXmlGenerateResultDto generateXml(SunatElectronicDocumentDto request) {
        SunatDocumentEntity document = this.findOrRegisterDocument(request);
        this.saveRequestPayload(document, request);
        try {
            this.sunatXmlValidationService.validateForXml(request);
        } catch (Exception ex) {
            this.markFunctionalGenerateError(document, ex.getMessage());
            throw ex;
        }
        return this.generateXml(document, request);
    }

    public SunatSendResultDto process(SunatElectronicDocumentDto request) {
        String sunatDocumentCod = null;
        try {
            SunatDocumentEntity document = this.findOrRegisterDocument(request);
            sunatDocumentCod = document.SunatDocumentCod;
            this.saveRequestPayload(document, request);
            this.sunatXmlValidationService.validateForXml(request);
            SunatXmlGenerateResultDto xml = this.generateXml(document, request);
            sunatDocumentCod = xml.SunatDocumentCod;
            this.signXml(sunatDocumentCod);
            this.generateZip(sunatDocumentCod);
            return this.send(sunatDocumentCod);
        } catch (Exception ex) {
            log.error("Error procesando documento SUNAT completo. SunatDocumentCod={}", sunatDocumentCod, ex);
            if (sunatDocumentCod != null) {
                SunatDocumentEntity document = this.sunatDocumentRepository.findById(sunatDocumentCod).orElse(null);
                if (document != null) {
                    document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
                    boolean newFunctionalError = document.LastErrorType == null;
                    document.LastErrorType = document.LastErrorType == null ? SunatErrorTypeConst.FUNCTIONAL : document.LastErrorType;
                    document.LastFunctionalError = document.LastFunctionalError == null ? ex.getMessage() : document.LastFunctionalError;
                    document.LastTechnicalError = document.LastTechnicalError == null && !SunatErrorTypeConst.FUNCTIONAL.equals(document.LastErrorType) ? ex.getMessage() : document.LastTechnicalError;
                    this.sunatDocumentRepository.save(document.session(this.getUserCod()));
                    if (newFunctionalError) {
                        this.saveAttempt(document, SunatOperationTypeConst.GENERATE_XML, false, null, ex.getMessage());
                    }
                    return toSendResult(document, ex.getMessage());
                }
            }
            throw new IllegalArgumentException("No se pudo procesar documento SUNAT: " + ex.getMessage(), ex);
        }
    }

    public SunatXmlGenerateResultDto generateXml(String sunatDocumentCod) {
        SunatDocumentEntity document = this.sunatDocumentRepository.findById(sunatDocumentCod)
                .orElseThrow(() -> new IllegalArgumentException("Documento SUNAT no encontrado"));
        SunatDocumentPayloadEntity payload = this.sunatDocumentPayloadRepository.findById(sunatDocumentCod)
                .orElseThrow(() -> new IllegalArgumentException("No existe payload guardado para regenerar XML"));
        try {
            SunatElectronicDocumentDto request = this.objectMapper.readValue(payload.PayloadJson, SunatElectronicDocumentDto.class);
            this.sunatXmlValidationService.validateForXml(request);
            return this.generateXml(document, request);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo regenerar XML: " + ex.getMessage(), ex);
        }
    }

    public SunatFileProcessResultDto signXml(String sunatDocumentCod) {
        SunatDocumentEntity document = this.findDocumentForProcess(sunatDocumentCod);
        SunatConfigEntity config = this.findDocumentConfig(document);
        SunatDocumentPayloadEntity payload = this.sunatDocumentPayloadRepository.findById(sunatDocumentCod)
                .orElseThrow(() -> new IllegalArgumentException("No existe XML generado para firmar"));
        try {
            if (payload.UnsignedXml == null || payload.UnsignedXml.isBlank()) {
                throw new IllegalArgumentException("XML sin firmar requerido");
            }
            this.sunatFileStorageService.saveXml(
                    config,
                    document,
                    SunatFileTypeConst.XML,
                    payload.UnsignedXmlFileName,
                    payload.UnsignedXml
            );
            String signedXml = this.sunatXmlSignatureService.sign(config, payload.UnsignedXml);
            String signedFileName = SunatDocumentNameUtil.xmlName(
                    document.IssuerRuc,
                    document.SunatDocumentType,
                    document.Series,
                    document.Correlative
            );
            SunatDocumentFileEntity signedFile = this.sunatFileStorageService.saveXml(
                    config,
                    document,
                    SunatFileTypeConst.XML_SIGNED,
                    signedFileName,
                    signedXml
            );
            document.ElectronicStatus = SunatElectronicStatusConst.FIRMADO;
            document.LastTechnicalError = null;
            document.LastFunctionalError = null;
            document.LastErrorType = null;
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.SIGN_XML, true, "XML firmado", null);
            return toFileResult(document, signedFile);
        } catch (Exception ex) {
            log.error("Error firmando XML SUNAT. SunatDocumentCod={}", sunatDocumentCod, ex);
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error firmando XML";
            document.LastTechnicalError = ex.getMessage();
            document.LastErrorType = SunatErrorTypeConst.INTERNAL;
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.SIGN_XML, false, ex.getMessage(), "Error firmando XML");
            throw new IllegalArgumentException("No se pudo firmar XML: " + ex.getMessage(), ex);
        }
    }

    public SunatFileProcessResultDto generateZip(String sunatDocumentCod) {
        SunatDocumentEntity document = this.findDocumentForProcess(sunatDocumentCod);
        SunatConfigEntity config = this.findDocumentConfig(document);
        SunatDocumentFileEntity signedFile = this.sunatDocumentFileRepository.findLastByDocumentAndType(
                sunatDocumentCod,
                SunatFileTypeConst.XML_SIGNED
        ).orElseThrow(() -> new IllegalArgumentException("No existe XML firmado para comprimir"));
        try {
            byte[] signedContent = this.sunatFileStorageService.read(signedFile);
            byte[] zipContent = this.sunatZipService.zip(signedFile.FileName, signedContent);
            String zipFileName = SunatDocumentNameUtil.zipName(
                    document.IssuerRuc,
                    document.SunatDocumentType,
                    document.Series,
                    document.Correlative
            );
            SunatDocumentFileEntity zipFile = this.sunatFileStorageService.saveZip(
                    config,
                    document,
                    SunatFileTypeConst.ZIP_SEND,
                    zipFileName,
                    zipContent
            );
            document.ElectronicStatus = SunatElectronicStatusConst.COMPRIMIDO;
            document.LastTechnicalError = null;
            document.LastFunctionalError = null;
            document.LastErrorType = null;
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.GENERATE_ZIP, true, "ZIP generado", null);
            return toFileResult(document, zipFile);
        } catch (Exception ex) {
            log.error("Error generando ZIP SUNAT. SunatDocumentCod={}", sunatDocumentCod, ex);
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error generando ZIP";
            document.LastTechnicalError = ex.getMessage();
            document.LastErrorType = SunatErrorTypeConst.INTERNAL;
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.GENERATE_ZIP, false, ex.getMessage(), "Error generando ZIP");
            throw new IllegalArgumentException("No se pudo generar ZIP: " + ex.getMessage(), ex);
        }
    }

    public SunatSendResultDto send(String sunatDocumentCod) {
        SunatDocumentEntity document = this.findDocumentForProcess(sunatDocumentCod);
        if (SunatElectronicStatusConst.PENDIENTE_TICKET.equals(document.ElectronicStatus)) {
            throw new IllegalArgumentException("Documento tiene ticket pendiente, consulte ticket antes de reenviar");
        }
        SunatConfigEntity config = this.findDocumentConfig(document);
        SunatDocumentFileEntity zipFile = this.sunatDocumentFileRepository.findLastByDocumentAndType(
                sunatDocumentCod,
                SunatFileTypeConst.ZIP_SEND
        ).orElseThrow(() -> new IllegalArgumentException("No existe ZIP generado para enviar"));
        try {
            byte[] zipContent = this.sunatFileStorageService.read(zipFile);
            boolean summaryDocument = this.isSummaryDocument(document);
            SunatSoapResponseDto response = summaryDocument
                    ? this.sunatSoapClientService.sendSummary(config, zipFile.FileName, zipContent)
                    : this.sunatSoapClientService.sendBill(config, zipFile.FileName, zipContent);
            this.saveSoapResponse(config, document, SunatOperationTypeConst.SEND, response);

            document.SendAttemptCount = document.SendAttemptCount + 1;
            document.TechnicalResponse = response.RawResponse;
            if (response.hasFault() || response.HttpStatusCode != 200) {
                document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
                document.LastFunctionalError = response.FaultString;
                document.LastTechnicalError = response.RawResponse;
                document.LastErrorType = SunatErrorTypeConst.SUNAT;
                this.saveAttempt(document, SunatOperationTypeConst.SEND, false, response.RawResponse, response.FaultString, response);
                this.sunatDocumentRepository.save(document.session(this.getUserCod()));
                return toSendResult(document, response.FaultString);
            }

            if (summaryDocument) {
                if (!response.hasTicket()) {
                    throw new IllegalArgumentException("SUNAT no devolvio ticket para resumen o baja");
                }
                document.TicketSunat = response.Ticket;
                document.ElectronicStatus = SunatElectronicStatusConst.PENDIENTE_TICKET;
                document.LastTechnicalError = null;
                document.LastFunctionalError = null;
                document.LastErrorType = null;
                this.saveAttempt(document, SunatOperationTypeConst.SEND, true, response.RawResponse, null, response);
                this.sunatDocumentRepository.save(document.session(this.getUserCod()));
                return toSendResult(document, "Documento enviado, pendiente de consulta de ticket");
            }

            if (!response.hasApplicationResponse()) {
                throw new IllegalArgumentException("SUNAT no devolvio CDR para comprobante individual");
            }
            SunatCdrResultDto cdr = this.sunatCdrProcessService.processBase64Cdr(config, document, response.ApplicationResponseBase64);
            applyCdrResult(document, cdr);
            this.saveAttempt(document, SunatOperationTypeConst.SEND, true, response.RawResponse, cdr.Description, response);
            this.sunatDocumentRepository.save(document.session(this.getUserCod()));
            return toSendResult(document, cdr.Description);
        } catch (Exception ex) {
            log.error("Error enviando documento SUNAT. SunatDocumentCod={}", sunatDocumentCod, ex);
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error enviando documento SUNAT";
            document.LastTechnicalError = ex.getMessage();
            document.LastErrorType = SunatErrorTypeConst.INTERNAL;
            this.saveAttempt(document, SunatOperationTypeConst.SEND, false, ex.getMessage(), "Error enviando documento SUNAT", null);
            this.sunatDocumentRepository.save(document.session(this.getUserCod()));
            throw new IllegalArgumentException("No se pudo enviar documento SUNAT: " + ex.getMessage(), ex);
        }
    }

    public SunatSendResultDto consultTicket(String sunatDocumentCod) {
        SunatDocumentEntity document = this.findDocumentForProcess(sunatDocumentCod);
        if (document.TicketSunat == null || document.TicketSunat.isBlank()) {
            throw new IllegalArgumentException("Documento no tiene ticket SUNAT pendiente");
        }
        SunatConfigEntity config = this.findDocumentConfig(document);
        try {
            SunatSoapResponseDto response = this.sunatSoapClientService.getStatus(config, document.TicketSunat);
            this.saveSoapResponse(config, document, SunatOperationTypeConst.CONSULT_TICKET, response);
            document.TicketAttemptCount = document.TicketAttemptCount + 1;
            document.TechnicalResponse = response.RawResponse;
            if (response.hasFault() || response.HttpStatusCode != 200) {
                document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
                document.LastFunctionalError = response.FaultString;
                document.LastTechnicalError = response.RawResponse;
                document.LastErrorType = SunatErrorTypeConst.SUNAT;
                this.saveAttempt(document, SunatOperationTypeConst.CONSULT_TICKET, false, response.RawResponse, response.FaultString, response);
                this.sunatDocumentRepository.save(document.session(this.getUserCod()));
                return toSendResult(document, response.FaultString);
            }
            if (!response.hasContent()) {
                document.ElectronicStatus = SunatElectronicStatusConst.PENDIENTE_TICKET;
                this.saveAttempt(document, SunatOperationTypeConst.CONSULT_TICKET, true, response.RawResponse, "Ticket aun sin CDR", response);
                this.sunatDocumentRepository.save(document.session(this.getUserCod()));
                return toSendResult(document, "Ticket consultado sin CDR final");
            }
            SunatCdrResultDto cdr = this.sunatCdrProcessService.processBase64Cdr(config, document, response.ContentBase64);
            applyCdrResult(document, cdr);
            this.saveAttempt(document, SunatOperationTypeConst.CONSULT_TICKET, true, response.RawResponse, cdr.Description, response);
            this.sunatDocumentRepository.save(document.session(this.getUserCod()));
            return toSendResult(document, cdr.Description);
        } catch (Exception ex) {
            log.error("Error consultando ticket SUNAT. SunatDocumentCod={}", sunatDocumentCod, ex);
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error consultando ticket SUNAT";
            document.LastTechnicalError = ex.getMessage();
            document.LastErrorType = SunatErrorTypeConst.INTERNAL;
            this.saveAttempt(document, SunatOperationTypeConst.CONSULT_TICKET, false, ex.getMessage(), "Error consultando ticket SUNAT", null);
            this.sunatDocumentRepository.save(document.session(this.getUserCod()));
            throw new IllegalArgumentException("No se pudo consultar ticket SUNAT: " + ex.getMessage(), ex);
        }
    }

    public SunatPendingOperationDto retry(String sunatDocumentCod) {
        return pending("RETRY", "FASE_5", sunatDocumentCod);
    }

    private SunatPendingOperationDto pending(String operation, String phase, String sunatDocumentCod) {
        return new SunatPendingOperationDto(
                operation,
                phase,
                "Operacion pendiente para documento " + sunatDocumentCod + ". No se ejecuta en esta fase."
        );
    }

    private SunatXmlGenerateResultDto generateXml(SunatDocumentEntity document, SunatElectronicDocumentDto request) {
        document.ensureNotAccepted();
        try {
            String xml = this.sunatUblXmlBuildService.build(request);
            String fileName = SunatDocumentNameUtil.xmlName(
                    document.IssuerRuc,
                    document.SunatDocumentType,
                    document.Series,
                    document.Correlative
            );
            SunatDocumentPayloadEntity payload = this.sunatDocumentPayloadRepository.findById(document.SunatDocumentCod)
                    .orElseGet(SunatDocumentPayloadEntity::new);
            boolean isNewPayload = payload.SunatDocumentCod == null || payload.SunatDocumentCod.isBlank();
            payload.SunatDocumentCod = document.SunatDocumentCod;
            payload.PayloadJson = this.objectMapper.writeValueAsString(request);
            payload.UnsignedXml = xml;
            payload.UnsignedXmlFileName = fileName;
            payload.XmlGeneratedDate = new Date();
            payload.addSession(this.getUserCod(), isNewPayload);
            payload.validate();
            this.sunatDocumentPayloadRepository.save(payload);

            document.ElectronicStatus = SunatElectronicStatusConst.GENERADO;
            document.LastTechnicalError = null;
            document.LastFunctionalError = null;
            document.LastErrorType = null;
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);

            this.saveAttempt(document, SunatOperationTypeConst.GENERATE_XML, true, "XML UBL 2.1 generado", null);
            return new SunatXmlGenerateResultDto(
                    document.SunatDocumentCod,
                    document.FullDocumentNumber,
                    document.ElectronicStatus,
                    fileName,
                    xml
            );
        } catch (Exception ex) {
            log.error("Error generando XML SUNAT. SunatDocumentCod={}", document.SunatDocumentCod, ex);
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error generando XML";
            document.LastTechnicalError = ex.getMessage();
            document.LastErrorType = SunatErrorTypeConst.INTERNAL;
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.GENERATE_XML, false, ex.getMessage(), "Error generando XML");
            throw new IllegalArgumentException("No se pudo generar XML: " + ex.getMessage(), ex);
        }
    }

    private SunatDocumentEntity findOrRegisterDocument(SunatElectronicDocumentDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Documento electronico requerido");
        }
        return this.sunatDocumentRepository.findBySource(
                request.SourceModule,
                request.SourceDocumentCod,
                request.SunatDocumentType
        ).orElseGet(() -> this.sunatDocumentCreateService.register(request.toRegisterDto()));
    }

    private void saveRequestPayload(SunatDocumentEntity document, SunatElectronicDocumentDto request) {
        try {
            SunatDocumentPayloadEntity payload = this.sunatDocumentPayloadRepository.findById(document.SunatDocumentCod)
                    .orElseGet(SunatDocumentPayloadEntity::new);
            boolean isNewPayload = payload.SunatDocumentCod == null || payload.SunatDocumentCod.isBlank();
            payload.SunatDocumentCod = document.SunatDocumentCod;
            payload.PayloadJson = this.objectMapper.writeValueAsString(request);
            payload.addSession(this.getUserCod(), isNewPayload);
            payload.validate();
            this.sunatDocumentPayloadRepository.save(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo guardar payload SUNAT recibido: " + ex.getMessage(), ex);
        }
    }

    private void markFunctionalGenerateError(SunatDocumentEntity document, String message) {
        document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
        document.LastErrorType = SunatErrorTypeConst.FUNCTIONAL;
        document.LastFunctionalError = message;
        document.LastTechnicalError = null;
        this.sunatDocumentRepository.save(document.session(this.getUserCod()));
        this.saveAttempt(document, SunatOperationTypeConst.GENERATE_XML, false, null, message);
    }

    private SunatDocumentEntity findDocumentForProcess(String sunatDocumentCod) {
        SunatDocumentEntity document = this.sunatDocumentRepository.findById(sunatDocumentCod)
                .orElseThrow(() -> new IllegalArgumentException("Documento SUNAT no encontrado"));
        document.ensureNotAccepted();
        return document;
    }

    private boolean isSummaryDocument(SunatDocumentEntity document) {
        return "RC".equals(document.SunatDocumentType) || "RA".equals(document.SunatDocumentType);
    }

    private SunatConfigEntity findDocumentConfig(SunatDocumentEntity document) {
        return this.sunatConfigRepository.findById(document.SunatConfigCod)
                .orElseThrow(() -> new IllegalArgumentException("Configuracion SUNAT del documento no encontrada"));
    }

    private SunatFileProcessResultDto toFileResult(SunatDocumentEntity document, SunatDocumentFileEntity file) {
        return new SunatFileProcessResultDto(
                document.SunatDocumentCod,
                document.ElectronicStatus,
                file.FileType,
                file.FileName,
                file.FilePath,
                file.SizeBytes,
                file.Sha256Hash
        );
    }

    private SunatSendResultDto toSendResult(SunatDocumentEntity document, String message) {
        SunatSendResultDto result = new SunatSendResultDto();
        result.SunatDocumentCod = document.SunatDocumentCod;
        result.ElectronicStatus = document.ElectronicStatus;
        result.TicketSunat = document.TicketSunat;
        result.SunatResponseCode = document.SunatResponseCode;
        result.SunatResponseDescription = document.SunatResponseDescription;
        result.SunatObservations = document.SunatObservations;
        result.LastErrorType = document.LastErrorType;
        result.LastTechnicalError = document.LastTechnicalError;
        result.LastFunctionalError = document.LastFunctionalError;
        result.Processed = SunatElectronicStatusConst.isAccepted(document.ElectronicStatus)
                || SunatElectronicStatusConst.PENDIENTE_TICKET.equals(document.ElectronicStatus);
        result.Message = message;
        return result;
    }

    private void applyCdrResult(SunatDocumentEntity document, SunatCdrResultDto cdr) {
        document.SunatResponseCode = cdr.ResponseCode;
        document.SunatResponseDescription = cdr.Description;
        document.SunatObservations = cdr.Observations;
        document.ElectronicStatus = cdr.ElectronicStatus;
        document.LastTechnicalError = null;
        document.LastFunctionalError = null;
        document.LastErrorType = null;
        if (cdr.Accepted || cdr.AcceptedWithObservations) {
            document.AcceptedDate = new Date();
        }
        if (cdr.Rejected) {
            document.RejectedDate = new Date();
            document.LastErrorType = SunatErrorTypeConst.SUNAT;
            document.LastFunctionalError = cdr.Description;
        }
    }

    private void saveSoapResponse(SunatConfigEntity config, SunatDocumentEntity document, String operationType, SunatSoapResponseDto response) {
        if (response == null || response.RawResponse == null || response.RawResponse.isBlank()) {
            return;
        }
        String fileName = operationType + "-" + System.currentTimeMillis() + ".xml";
        this.sunatFileStorageService.saveText(
                config,
                document,
                SunatFileTypeConst.RESPONSE,
                fileName,
                "text/xml",
                response.RawResponse
        );
    }

    private void saveAttempt(SunatDocumentEntity document, String operationType, boolean success,
                             String technicalMessage, String functionalMessage) {
        SunatDocumentAttemptEntity attempt = new SunatDocumentAttemptEntity();
        attempt.SunatDocumentCod = document.SunatDocumentCod;
        attempt.OperationType = operationType;
        attempt.Environment = document.Environment;
        attempt.AttemptNumber = this.sunatDocumentAttemptRepository.findBySunatDocumentCod(document.SunatDocumentCod).size() + 1;
        attempt.Success = success ? "S" : "N";
        attempt.TechnicalMessage = technicalMessage;
        attempt.FunctionalMessage = functionalMessage;
        this.sunatDocumentAttemptCreateService.save(attempt);
    }

    private void saveAttempt(SunatDocumentEntity document, String operationType, boolean success,
                             String technicalMessage, String functionalMessage, SunatSoapResponseDto response) {
        SunatDocumentAttemptEntity attempt = new SunatDocumentAttemptEntity();
        attempt.SunatDocumentCod = document.SunatDocumentCod;
        attempt.OperationType = operationType;
        attempt.Environment = document.Environment;
        attempt.Endpoint = response == null ? null : response.Endpoint;
        attempt.AttemptNumber = this.sunatDocumentAttemptRepository.findBySunatDocumentCod(document.SunatDocumentCod).size() + 1;
        attempt.Success = success ? "S" : "N";
        attempt.TechnicalMessage = technicalMessage;
        attempt.FunctionalMessage = functionalMessage;
        attempt.SunatTicket = response == null ? null : response.Ticket;
        attempt.SunatResponseCode = response == null ? null : limit(response.FaultCode, 128);
        this.sunatDocumentAttemptCreateService.save(attempt);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
