package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.constants.SunatElectronicStatusConst;
import com.ccadmin.app.sunat.model.constants.SunatFileTypeConst;
import com.ccadmin.app.sunat.model.constants.SunatOperationTypeConst;
import com.ccadmin.app.sunat.model.dto.SunatElectronicDocumentDto;
import com.ccadmin.app.sunat.model.dto.SunatFileProcessResultDto;
import com.ccadmin.app.sunat.model.dto.SunatPendingOperationDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

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
    private ObjectMapper objectMapper;

    public SunatXmlGenerateResultDto generateXml(SunatElectronicDocumentDto request) {
        this.sunatXmlValidationService.validateForXml(request);
        SunatDocumentEntity document = this.sunatDocumentRepository.findBySource(
                request.SourceModule,
                request.SourceDocumentCod,
                request.SunatDocumentType
        ).orElseGet(() -> this.sunatDocumentCreateService.register(request.toRegisterDto()));
        return this.generateXml(document, request);
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
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.SIGN_XML, true, "XML firmado", null);
            return toFileResult(document, signedFile);
        } catch (Exception ex) {
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error firmando XML";
            document.LastTechnicalError = ex.getMessage();
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
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.GENERATE_ZIP, true, "ZIP generado", null);
            return toFileResult(document, zipFile);
        } catch (Exception ex) {
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error generando ZIP";
            document.LastTechnicalError = ex.getMessage();
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.GENERATE_ZIP, false, ex.getMessage(), "Error generando ZIP");
            throw new IllegalArgumentException("No se pudo generar ZIP: " + ex.getMessage(), ex);
        }
    }

    public SunatPendingOperationDto send(String sunatDocumentCod) {
        return pending("SEND", "FASE_4", sunatDocumentCod);
    }

    public SunatPendingOperationDto consultTicket(String sunatDocumentCod) {
        return pending("CONSULT_TICKET", "FASE_4", sunatDocumentCod);
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
            document.ElectronicStatus = SunatElectronicStatusConst.ERROR;
            document.LastFunctionalError = "Error generando XML";
            document.LastTechnicalError = ex.getMessage();
            document.addSession(this.getUserCod());
            this.sunatDocumentRepository.save(document);
            this.saveAttempt(document, SunatOperationTypeConst.GENERATE_XML, false, ex.getMessage(), "Error generando XML");
            throw new IllegalArgumentException("No se pudo generar XML: " + ex.getMessage(), ex);
        }
    }

    private SunatDocumentEntity findDocumentForProcess(String sunatDocumentCod) {
        SunatDocumentEntity document = this.sunatDocumentRepository.findById(sunatDocumentCod)
                .orElseThrow(() -> new IllegalArgumentException("Documento SUNAT no encontrado"));
        document.ensureNotAccepted();
        return document;
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
}
