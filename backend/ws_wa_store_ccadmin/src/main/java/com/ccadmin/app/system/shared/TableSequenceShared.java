package com.ccadmin.app.system.shared;

import com.ccadmin.app.system.service.TableSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TableSequenceShared {

    @Autowired
    TableSequenceService tableSequenceService;

    public String getNextCode(String sequenceTableType){
        return this.tableSequenceService.getNextCode(sequenceTableType);
    }


}
