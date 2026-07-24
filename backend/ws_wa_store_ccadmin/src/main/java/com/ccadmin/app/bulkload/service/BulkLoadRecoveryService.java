package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.shared.service.GenericQueuedService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BulkLoadRecoveryService {
    private final BulkLoadHeadRepository headRepository;
    private final BulkLoadChunkService chunkService;
    private final GenericQueuedService queuedService;
    private final BulkLoadProcessService processService;

    public BulkLoadRecoveryService(BulkLoadHeadRepository headRepository,
                                   BulkLoadChunkService chunkService,
                                   GenericQueuedService queuedService,
                                   BulkLoadProcessService processService) {
        this.headRepository = headRepository;
        this.chunkService = chunkService;
        this.queuedService = queuedService;
        this.processService = processService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingJobs() {
        for (BulkLoadHeadEntity head : headRepository.findRecoverable()) {
            chunkService.recover(head.BulkLoadCod);
            queuedService.addQueued(new BulkLoadTaskService(processService, head.BulkLoadCod));
        }
    }
}
