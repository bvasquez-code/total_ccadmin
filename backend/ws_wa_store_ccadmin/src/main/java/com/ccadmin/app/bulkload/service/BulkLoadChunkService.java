package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDestinationEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.bulkload.repository.BulkLoadDestinationRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadDetRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.bulkload.service.handler.BulkLoadTypeHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class BulkLoadChunkService {
    private final BulkLoadHeadRepository headRepository;
    private final BulkLoadDestinationRepository destinationRepository;
    private final BulkLoadDetRepository bulkLoadDetRepository;
    private final BulkLoadTypeHandlerRegistry handlerRegistry;

    public BulkLoadChunkService(BulkLoadHeadRepository headRepository,
                                BulkLoadDestinationRepository destinationRepository,
                                BulkLoadDetRepository bulkLoadDetRepository,
                                BulkLoadTypeHandlerRegistry handlerRegistry) {
        this.headRepository = headRepository;
        this.destinationRepository = destinationRepository;
        this.bulkLoadDetRepository = bulkLoadDetRepository;
        this.handlerRegistry = handlerRegistry;
    }

    /**
     * Prepara recursos particulares antes de abrir la transaccion del bloque.
     * Para stock solicita get_cod_trx, cuyo correlativo se confirma de inmediato.
     */
    public String prepareNextChunk(String code) {
        BulkLoadHeadEntity head = headRepository.findById(code).orElse(null);
        return head == null ? null : handlerRegistry
                .getRequired(head.BulkLoadType)
                .prepareResource(code);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean start(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || !BulkLoadConstants.QUEUED.equals(head.ProcessStatus)) return false;
        Date now = new Date();
        head.ProcessStatus = BulkLoadConstants.WORKING;
        head.StartDate = head.StartDate == null ? now : head.StartDate;
        head.LastHeartbeatDate = now;
        head.AttemptCount = value(head.AttemptCount) + 1;
        head.StatusMessage = "Procesando en segundo plano";
        head.addSessionModify(processUser(head));
        headRepository.save(head);
        for (BulkLoadDestinationEntity destination : destinationRepository.findByCode(code)) {
            if (BulkLoadConstants.FINALIZED.equals(destination.ProcessStatus)) {
                continue;
            }
            destination.ProcessStatus = BulkLoadConstants.WORKING;
            destination.StartDate = destination.StartDate == null ? now : destination.StartDate;
            destination.StatusMessage = "Procesando";
            destination.addSessionModify(processUser(head));
            destinationRepository.save(destination);
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int processNextChunk(String code, String stockEntryCod) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || !BulkLoadConstants.WORKING.equals(head.ProcessStatus)) return 0;
        List<BulkLoadDetEntity> detailList = bulkLoadDetRepository.findNextPendingForUpdate(code);
        if (detailList.isEmpty()) return 0;
        String userCod = processUser(head);
        Date now = new Date();
        detailList.forEach(detail -> {
            detail.ProcessStatus = BulkLoadConstants.WORKING;
            detail.StartDate = detail.StartDate == null ? now : detail.StartDate;
            detail.AttemptCount = value(detail.AttemptCount) + 1;
            detail.addSessionModify(userCod);
        });
        bulkLoadDetRepository.saveAll(detailList);

        handlerRegistry.getRequired(head.BulkLoadType).execute(
                head, detailList, userCod, stockEntryCod
        );

        Date end = new Date();
        detailList.forEach(detail -> {
            detail.ProcessStatus = BulkLoadConstants.CONFIRMED;
            detail.EndDate = end;
            detail.addSessionModify(userCod);
        });
        bulkLoadDetRepository.saveAll(detailList);
        bulkLoadDetRepository.flush();
        updateCounters(head, userCod, false);
        return detailList.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void finish(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || !BulkLoadConstants.WORKING.equals(head.ProcessStatus)) return;
        String userCod = processUser(head);
        updateCounters(head, userCod, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void fail(String code, Exception exception) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || BulkLoadConstants.FINALIZED.equals(head.ProcessStatus)
                || BulkLoadConstants.CANCELLED.equals(head.ProcessStatus)) {
            return;
        }
        String userCod = processUser(head);
        Date now = new Date();
        head.ProcessStatus = BulkLoadConstants.ERROR;
        head.EndDate = now;
        head.LastHeartbeatDate = now;
        head.StatusMessage = trimMessage(exception == null ? null : exception.getMessage());
        head.addSessionModify(userCod);
        headRepository.save(head);
        for (BulkLoadDestinationEntity destination : destinationRepository.findByCode(code)) {
            if (!BulkLoadConstants.FINALIZED.equals(destination.ProcessStatus)) {
                destination.ProcessStatus = BulkLoadConstants.ERROR;
                destination.EndDate = now;
                destination.StatusMessage = head.StatusMessage;
                destination.addSessionModify(userCod);
                destinationRepository.save(destination);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recover(String code) {
        BulkLoadHeadEntity head = headRepository.findForUpdate(code);
        if (head == null || (!BulkLoadConstants.WORKING.equals(head.ProcessStatus)
                && !BulkLoadConstants.QUEUED.equals(head.ProcessStatus))) {
            return;
        }
        head.ProcessStatus = BulkLoadConstants.QUEUED;
        head.StatusMessage = "Recuperado al iniciar la aplicacion";
        head.LastHeartbeatDate = new Date();
        headRepository.save(head);
        for (BulkLoadDestinationEntity destination : destinationRepository.findByCode(code)) {
            if (!BulkLoadConstants.FINALIZED.equals(destination.ProcessStatus)) {
                destination.ProcessStatus = BulkLoadConstants.QUEUED;
                destination.StatusMessage = "En cola";
                destinationRepository.save(destination);
            }
        }
    }

    private void updateCounters(BulkLoadHeadEntity head, String userCod, boolean finish) {
        int success = bulkLoadDetRepository.countByProcessStatus(
                head.BulkLoadCod, BulkLoadConstants.CONFIRMED
        );
        int errors = bulkLoadDetRepository.countByProcessStatus(
                head.BulkLoadCod, BulkLoadConstants.ERROR
        );
        int processed = success + errors;
        int total = value(head.NumTotalDetails);
        head.NumSuccessDetails = success;
        head.NumErrorDetails = errors;
        head.NumProcessedDetails = processed;
        head.ProgressPercent = total == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(processed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        head.LastHeartbeatDate = new Date();
        head.StatusMessage = processed + " de " + total + " registros procesados";

        for (BulkLoadDestinationEntity destination
                : destinationRepository.findByCode(head.BulkLoadCod)) {
            int storeSuccess = bulkLoadDetRepository.countByStoreAndProcessStatus(
                    head.BulkLoadCod, destination.StoreCod, BulkLoadConstants.CONFIRMED
            );
            int storeErrors = bulkLoadDetRepository.countByStoreAndProcessStatus(
                    head.BulkLoadCod, destination.StoreCod, BulkLoadConstants.ERROR
            );
            destination.NumSuccessDetails = storeSuccess;
            destination.NumErrorDetails = storeErrors;
            destination.NumProcessedDetails = storeSuccess + storeErrors;
            destination.StatusMessage = destination.NumProcessedDetails
                    + " de " + value(destination.NumTotalDetails) + " registros procesados";
            if (finish) {
                destination.ProcessStatus = BulkLoadConstants.FINALIZED;
                destination.EndDate = new Date();
            }
            destination.addSessionModify(userCod);
            destinationRepository.save(destination);
        }

        if (finish) {
            if (processed != total) {
                throw new IllegalStateException(
                        "No se puede finalizar: existen registros pendientes"
                );
            }
            head.ProcessStatus = BulkLoadConstants.FINALIZED;
            head.ProgressPercent = BigDecimal.valueOf(100).setScale(2);
            head.EndDate = new Date();
            head.StatusMessage = errors == 0
                    ? "Carga finalizada correctamente"
                    : "Carga finalizada con errores";
        }
        head.addSessionModify(userCod);
        headRepository.save(head);
    }

    private String processUser(BulkLoadHeadEntity head) {
        return head.CreationUser == null || head.CreationUser.isBlank()
                ? "SISTEMA" : head.CreationUser;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private String trimMessage(String message) {
        String result = message == null || message.isBlank()
                ? "Error inesperado durante el procesamiento" : message.trim();
        return result.length() <= 512 ? result : result.substring(0, 512);
    }
}
