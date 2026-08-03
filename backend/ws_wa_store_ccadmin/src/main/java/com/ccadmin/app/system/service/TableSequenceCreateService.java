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

        return this.tableSequenceRepository.save(request);
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
