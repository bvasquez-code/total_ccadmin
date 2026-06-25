package com.ccadmin.app.system.service;

import com.ccadmin.app.system.repository.TableSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableSequenceService {

    @Autowired
    TableSequenceRepository tableSequenceRepository;

    public String getNextCode(String sequenceTableType){
        return this.tableSequenceRepository.getNextCode(sequenceTableType);
    }

}
