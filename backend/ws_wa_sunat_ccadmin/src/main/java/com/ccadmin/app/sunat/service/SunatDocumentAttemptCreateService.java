package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.entity.SunatDocumentAttemptEntity;
import com.ccadmin.app.sunat.repository.SunatDocumentAttemptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SunatDocumentAttemptCreateService extends SessionService {

    @Autowired
    private SunatDocumentAttemptRepository sunatDocumentAttemptRepository;

    public SunatDocumentAttemptEntity save(SunatDocumentAttemptEntity attempt) {
        if (attempt == null) {
            throw new IllegalArgumentException("Intento SUNAT requerido");
        }
        attempt.validate();
        attempt.addSession(this.getUserCod(), true);
        return this.sunatDocumentAttemptRepository.save(attempt);
    }
}
