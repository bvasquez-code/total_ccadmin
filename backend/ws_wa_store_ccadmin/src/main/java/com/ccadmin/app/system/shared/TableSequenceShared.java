package com.ccadmin.app.system.shared;

import com.ccadmin.app.system.service.TableSequenceService;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.stereotype.Service;

import java.util.function.Predicate;

@Service
public class TableSequenceShared {
    private final TableSequenceService tableSequenceService;

    public TableSequenceShared(TableSequenceService tableSequenceService) {
        this.tableSequenceService = tableSequenceService;
    }

    public String getNextCode(String sequenceTableType){
        return this.tableSequenceService.getNextCode(sequenceTableType);
    }

    public String getNextAvailableCode(String sequenceTableType,
                                       Predicate<String> codeExists) {
        String code = getNextCode(sequenceTableType);
        while (StringUtil.isBlank(code) || codeExists.test(code)) {
            code = getNextCode(sequenceTableType);
        }
        return code;
    }
}
