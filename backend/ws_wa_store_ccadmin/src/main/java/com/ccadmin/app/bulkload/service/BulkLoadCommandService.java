package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.BulkLoadRegisterDto;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDestinationEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.bulkload.repository.BulkLoadDestinationRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadDetRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;

@Service
public class BulkLoadCommandService extends SessionService {
    private final BulkLoadHeadRepository headRepository;
    private final BulkLoadDestinationRepository destinationRepository;
    private final BulkLoadDetRepository detRepository;
    private final GenericQueuedService queuedService;
    private final BulkLoadProcessService processService;

    public BulkLoadCommandService(BulkLoadHeadRepository headRepository,
                                  BulkLoadDestinationRepository destinationRepository,
                                  BulkLoadDetRepository detRepository,
                                  GenericQueuedService queuedService,
                                  BulkLoadProcessService processService) {
        this.headRepository = headRepository;
        this.destinationRepository = destinationRepository;
        this.detRepository = detRepository;
        this.queuedService = queuedService;
        this.processService = processService;
    }

    @Transactional(rollbackOn = Exception.class)
    public BulkLoadRegisterDto confirm(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(clean(code));
        if (head == null) throw new IllegalArgumentException("No existe la carga masiva");
        if (!BulkLoadConstants.PENDING.equals(head.ProcessStatus)) {
            throw new IllegalStateException("Solo se puede confirmar una carga validada y pendiente");
        }
        if (value(head.NumTotalDetails) == 0 || value(head.NumErrorDetails) > 0) {
            throw new IllegalStateException("La carga no tiene detalles validos para confirmar");
        }
        String userCod = getUserCod();
        Date now = new Date();
        head.ProcessStatus = BulkLoadConstants.QUEUED;
        head.QueueDate = now;
        head.LastHeartbeatDate = now;
        head.StatusMessage = "En cola para procesamiento";
        head.addSessionModify(userCod);
        headRepository.save(head);
        for (BulkLoadDestinationEntity destination
                : destinationRepository.findByCode(head.BulkLoadCod)) {
            destination.ProcessStatus = BulkLoadConstants.QUEUED;
            destination.StatusMessage = "En cola";
            destination.addSessionModify(userCod);
            destinationRepository.save(destination);
        }
        enqueueAfterCommit(head.BulkLoadCod);
        return new BulkLoadRegisterDto(head, destinationRepository.findByCode(head.BulkLoadCod));
    }

    @Transactional(rollbackOn = Exception.class)
    public BulkLoadRegisterDto cancel(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(clean(code));
        if (head == null) throw new IllegalArgumentException("No existe la carga masiva");
        if (!BulkLoadConstants.PENDING.equals(head.ProcessStatus)) {
            throw new IllegalStateException("Solo se puede anular una carga pendiente");
        }
        String userCod = getUserCod();
        Date now = new Date();
        head.ProcessStatus = BulkLoadConstants.CANCELLED;
        head.EndDate = now;
        head.StatusMessage = "Carga anulada por el usuario";
        head.addSessionModify(userCod);
        headRepository.save(head);
        detRepository.cancelPending(head.BulkLoadCod, userCod);
        for (BulkLoadDestinationEntity destination
                : destinationRepository.findByCode(head.BulkLoadCod)) {
            destination.ProcessStatus = BulkLoadConstants.CANCELLED;
            destination.EndDate = now;
            destination.StatusMessage = "Anulado";
            destination.addSessionModify(userCod);
            destinationRepository.save(destination);
        }
        return new BulkLoadRegisterDto(head, destinationRepository.findByCode(head.BulkLoadCod));
    }

    private void enqueueAfterCommit(String code) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            queuedService.addQueued(new BulkLoadTaskService(processService, code));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                queuedService.addQueued(new BulkLoadTaskService(processService, code));
            }
        });
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
