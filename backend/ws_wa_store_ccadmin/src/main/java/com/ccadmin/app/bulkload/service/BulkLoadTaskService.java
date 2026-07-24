package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.shared.service.IGenericTaskService;

public class BulkLoadTaskService implements IGenericTaskService {
    private final BulkLoadProcessService processService;
    private final String code;

    public BulkLoadTaskService(BulkLoadProcessService processService, String code) {
        this.processService = processService;
        this.code = code;
    }

    @Override
    public void execute() {
        processService.process(code);
    }
}
