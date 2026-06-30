package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.sunat.model.dto.SunatDocumentStatusDto;
import com.ccadmin.app.sunat.model.entity.SunatDocumentAttemptEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentFileEntity;
import com.ccadmin.app.sunat.repository.SunatDocumentAttemptRepository;
import com.ccadmin.app.sunat.repository.SunatDocumentFileRepository;
import com.ccadmin.app.sunat.repository.SunatDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SunatDocumentSearchService {

    @Autowired
    private SunatDocumentRepository sunatDocumentRepository;

    @Autowired
    private SunatDocumentFileRepository sunatDocumentFileRepository;

    @Autowired
    private SunatDocumentAttemptRepository sunatDocumentAttemptRepository;

    private SearchTService<SunatDocumentEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.sunatDocumentRepository);
    }

    public ResponsePageSearchT<SunatDocumentEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public SunatDocumentEntity findById(String sunatDocumentCod) {
        return this.sunatDocumentRepository.findById(sunatDocumentCod).orElse(null);
    }

    public SunatDocumentEntity findByIdOrThrow(String sunatDocumentCod) {
        return this.sunatDocumentRepository.findById(sunatDocumentCod)
                .orElseThrow(() -> new IllegalArgumentException("Documento SUNAT no encontrado"));
    }

    public SunatDocumentStatusDto findStatus(String sunatDocumentCod) {
        SunatDocumentEntity document = this.findByIdOrThrow(sunatDocumentCod);
        SunatDocumentStatusDto dto = new SunatDocumentStatusDto();
        dto.SunatDocumentCod = document.SunatDocumentCod;
        dto.SourceModule = document.SourceModule;
        dto.SourceDocumentCod = document.SourceDocumentCod;
        dto.SunatDocumentType = document.SunatDocumentType;
        dto.FullDocumentNumber = document.FullDocumentNumber;
        dto.ElectronicStatus = document.ElectronicStatus;
        dto.TicketSunat = document.TicketSunat;
        dto.SunatResponseCode = document.SunatResponseCode;
        dto.SunatResponseDescription = document.SunatResponseDescription;
        dto.SunatObservations = document.SunatObservations;
        dto.AcceptedDate = document.AcceptedDate;
        dto.RejectedDate = document.RejectedDate;
        return dto;
    }

    public List<SunatDocumentFileEntity> findFiles(String sunatDocumentCod) {
        return this.sunatDocumentFileRepository.findBySunatDocumentCod(sunatDocumentCod);
    }

    public List<SunatDocumentAttemptEntity> findAttempts(String sunatDocumentCod) {
        return this.sunatDocumentAttemptRepository.findBySunatDocumentCod(sunatDocumentCod);
    }
}
