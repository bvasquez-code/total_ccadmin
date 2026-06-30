package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.repository.SunatDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SunatSchedulerService {

    @Autowired
    private SunatDocumentRepository sunatDocumentRepository;

    @Scheduled(fixedDelayString = "${sunat.scheduler.fixed-delay-ms:60000}")
    public void processPendingDocuments() {
        // Fase 1 solo deja el punto de extension programado.
        // La consulta de tickets y reintentos se implementan en fase 5.
        this.sunatDocumentRepository.findPendingProcess();
    }
}
