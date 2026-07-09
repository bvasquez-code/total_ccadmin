package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.system.model.entity.TableSequenceEntity;
import com.ccadmin.app.system.repository.TableSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableSequenceSearchService {

    @Autowired
    private TableSequenceRepository tableSequenceRepository;

    private SearchTService<TableSequenceEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.tableSequenceRepository);
    }

    public ResponsePageSearchT<TableSequenceEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public TableSequenceEntity findById(Long sequenceTrx) {
        return this.tableSequenceRepository.findById(sequenceTrx).orElse(null);
    }

    public ResponseWsDto findDataForm(Long sequenceTrx) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (sequenceTrx != null) {
            TableSequenceEntity tableSequence = this.findById(sequenceTrx);
            if (tableSequence != null) {
                tableSequence.OriginalSequenceTrx = tableSequence.SequenceTrx;
            }
            rpt.AddResponseAdditional("tableSequence", tableSequence);
        }
        rpt.AddResponseAdditional("sequenceTableTypeList", this.tableSequenceRepository.findSequenceTableTypes());

        return rpt;
    }
}
