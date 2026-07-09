package com.ccadmin.app.system.service;

import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.StoreRepository;
import com.ccadmin.app.system.model.entity.StoreSequenceEntity;
import com.ccadmin.app.system.model.entity.id.StoreSequenceID;
import com.ccadmin.app.system.repository.StoreSequenceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoreSequenceCreateService {

    @Autowired
    private StoreSequenceRepository storeSequenceRepository;
    @Autowired
    private StoreRepository storeRepository;

    @Transactional
    public List<StoreSequenceEntity> save(StoreSequenceEntity request) {
        if (request == null) {
            throw new IllegalArgumentException("Secuencia por tienda requerida");
        }

        request.SequenceTableType = trim(request.SequenceTableType);
        request.Prefix = normalizeUpper(request.Prefix);

        if (request.StoreCod == null || request.StoreCod.isBlank()) {
            return this.createForAllStores(request);
        }

        request.StoreCod = normalizeUpper(request.StoreCod);
        request.validateForStore();
        return List.of(this.storeSequenceRepository.save(request));
    }

    private List<StoreSequenceEntity> createForAllStores(StoreSequenceEntity request) {
        request.validateForAllStores();

        List<StoreEntity> storeList = this.storeRepository.findAllActive();
        List<StoreSequenceEntity> sequenceList = new ArrayList<>();

        for (StoreEntity store : storeList) {
            StoreSequenceID id = new StoreSequenceID(store.StoreCod, request.PeriodId, request.SequenceTableType);
            if (this.storeSequenceRepository.existsById(id)) {
                continue;
            }

            StoreSequenceEntity sequence = new StoreSequenceEntity();
            sequence.StoreCod = store.StoreCod;
            sequence.PeriodId = request.PeriodId;
            sequence.SequenceTableType = request.SequenceTableType;
            sequence.SequenceTrx = request.SequenceTrx;
            sequence.Prefix = request.Prefix;
            sequence.SequenceLength = request.SequenceLength;
            sequenceList.add(sequence);
        }

        if (sequenceList.isEmpty()) {
            throw new IllegalArgumentException("La secuencia ya existe para todas las tiendas activas. Use la edicion por tienda.");
        }

        return this.storeSequenceRepository.saveAll(sequenceList);
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
