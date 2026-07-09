package com.ccadmin.app.system.service;

import com.ccadmin.app.sale.repository.PeriodRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.store.repository.StoreRepository;
import com.ccadmin.app.system.model.entity.StoreSequenceEntity;
import com.ccadmin.app.system.model.entity.id.StoreSequenceID;
import com.ccadmin.app.system.repository.StoreSequenceRepository;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StoreSequenceSearchService {

    @Autowired
    private StoreSequenceRepository storeSequenceRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private StoreRepository storeRepository;

    private SearchTService<StoreSequenceEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.storeSequenceRepository);
    }

    public ResponsePageSearchT<StoreSequenceEntity> findAll(String query, int page, String storeCod) {
        if (StringUtil.isNotEmpty(storeCod)) {
            return this.searchTService.findAllStore(new SearchDto(query, page, storeCod), 10);
        }
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public StoreSequenceEntity findById(String storeCod, Integer periodId, String sequenceTableType) {
        return this.storeSequenceRepository.findById(new StoreSequenceID(storeCod, periodId, sequenceTableType)).orElse(null);
    }

    public ResponseWsDto findDataForm(String storeCod, Integer periodId, String sequenceTableType) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (StringUtil.isNotEmpty(storeCod) && periodId != null && StringUtil.isNotEmpty(sequenceTableType)) {
            rpt.AddResponseAdditional("storeSequence", this.findById(storeCod, periodId, sequenceTableType));
        }
        rpt.AddResponseAdditional("activePeriod", this.periodRepository.findPeriodActuality());
        rpt.AddResponseAdditional("sequenceTableTypeList", this.storeSequenceRepository.findSequenceTableTypes());
        rpt.AddResponseAdditional("StoreList", this.storeRepository.findAllActive());

        return rpt;
    }
}
