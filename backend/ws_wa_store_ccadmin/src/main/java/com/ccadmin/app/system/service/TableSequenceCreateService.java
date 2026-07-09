package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.TableSequenceEntity;
import com.ccadmin.app.system.repository.TableSequenceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableSequenceCreateService {

    @Autowired
    private TableSequenceRepository tableSequenceRepository;

    @Transactional
    public TableSequenceEntity save(TableSequenceEntity request) {
        if (request == null) {
            throw new IllegalArgumentException("Secuencia global requerida");
        }

        request.SequenceTableType = trim(request.SequenceTableType);
        request.Prefix = normalizeUpper(request.Prefix);
        request.UsePrefix = normalizeUpper(request.UsePrefix);
        request.validate();

        Long originalSequenceTrx = request.OriginalSequenceTrx;

        if (originalSequenceTrx != null && !originalSequenceTrx.equals(request.SequenceTrx)) {
            if (!this.tableSequenceRepository.existsById(originalSequenceTrx)) {
                throw new IllegalArgumentException("Secuencia global original no encontrada");
            }
            if (this.tableSequenceRepository.existsById(request.SequenceTrx)) {
                throw new IllegalArgumentException("SequenceTrx ya existe");
            }
            this.tableSequenceRepository.deleteById(originalSequenceTrx);
        }

        request.OriginalSequenceTrx = request.SequenceTrx;
        return this.tableSequenceRepository.save(request);
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
