package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.constants.SunatElectronicStatusConst;
import com.ccadmin.app.sunat.model.dto.SunatDocumentRegisterDto;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentEntity;
import com.ccadmin.app.sunat.repository.SunatDocumentRepository;
import com.ccadmin.app.sunat.utility.SunatCodeUtil;
import com.ccadmin.app.sunat.utility.SunatDocumentNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SunatDocumentCreateService extends SessionService {

    @Autowired
    private SunatConfigSearchService sunatConfigSearchService;

    @Autowired
    private SunatDocumentRepository sunatDocumentRepository;

    public SunatDocumentEntity register(SunatDocumentRegisterDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Documento SUNAT requerido");
        }
        SunatConfigEntity config = this.sunatConfigSearchService.findActive();
        this.sunatDocumentRepository.findBySource(
                request.SourceModule,
                request.SourceDocumentCod,
                request.SunatDocumentType
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("Ya existe documento SUNAT para el documento origen indicado");
        });

        SunatDocumentEntity document = new SunatDocumentEntity();
        document.SunatDocumentCod = SunatCodeUtil.newCode("SD");
        document.SunatConfigCod = config.SunatConfigCod;
        document.SourceModule = request.SourceModule;
        document.SourceDocumentCod = request.SourceDocumentCod;
        document.SourceDocumentType = request.SourceDocumentType;
        document.SunatDocumentType = request.SunatDocumentType;
        document.Series = request.Series;
        document.Correlative = request.Correlative;
        document.FullDocumentNumber = SunatDocumentNameUtil.fullDocumentNumber(request.Series, request.Correlative);
        document.IssuerRuc = request.IssuerRuc == null || request.IssuerRuc.isBlank() ? config.IssuerRuc : request.IssuerRuc;
        document.IssueDate = request.IssueDate == null ? new Date() : request.IssueDate;
        document.CurrencyCod = request.CurrencyCod;
        document.NumTotalPrice = request.NumTotalPrice;
        document.NumTotalTax = request.NumTotalTax;
        document.Environment = config.Environment;
        document.ElectronicStatus = SunatElectronicStatusConst.PENDIENTE;
        document.OriginalSunatDocumentCod = request.OriginalSunatDocumentCod;
        document.RelatedDocumentNumber = request.RelatedDocumentNumber;
        document.RelatedDocumentType = request.RelatedDocumentType;
        document.validate();
        document.addSession(this.getUserCod(), true);
        return this.sunatDocumentRepository.save(document);
    }

    public SunatDocumentEntity markPendingRetry(String sunatDocumentCod, String technicalMessage, String functionalMessage) {
        SunatDocumentEntity document = this.sunatDocumentRepository.findById(sunatDocumentCod)
                .orElseThrow(() -> new IllegalArgumentException("Documento SUNAT no encontrado"));
        document.ensureNotAccepted();
        document.ElectronicStatus = SunatElectronicStatusConst.PENDIENTE_REINTENTO;
        document.LastTechnicalError = technicalMessage;
        document.LastFunctionalError = functionalMessage;
        document.addSession(this.getUserCod());
        return this.sunatDocumentRepository.save(document);
    }
}
