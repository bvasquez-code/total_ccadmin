package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.*;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDestinationEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.bulkload.repository.BulkLoadDestinationRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadDetRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Persiste exclusivamente la mascara generica de BulkLoad. No interpreta
 * tipos, productos, precios, stock ni el contenido del Payload.
 */
@Service
public class BulkLoadPersistenceService extends SessionService {
    private final BulkLoadHeadRepository headRepository;
    private final BulkLoadDestinationRepository destinationRepository;
    private final BulkLoadDetRepository detRepository;

    public BulkLoadPersistenceService(BulkLoadHeadRepository headRepository,
                                      BulkLoadDestinationRepository destinationRepository,
                                      BulkLoadDetRepository detRepository) {
        this.headRepository = headRepository;
        this.destinationRepository = destinationRepository;
        this.detRepository = detRepository;
    }

    @Transactional(rollbackOn = Exception.class)
    public BulkLoadRegisterDto savePrepared(String bulkLoadCod,
                                            BulkLoadParsedRequestDto request,
                                            BulkLoadPreparedDto prepared) {
        validatePrepared(prepared);
        String userCod = getUserCod();
        BulkLoadHeadEntity head = buildHead(bulkLoadCod, request, userCod);
        return persistPrepared(head, prepared, userCod);
    }

    @Transactional(rollbackOn = Exception.class)
    public BulkLoadRegisterDto replacePrepared(String bulkLoadCod,
                                               BulkLoadParsedRequestDto request,
                                               BulkLoadPreparedDto prepared) {
        validatePrepared(prepared);
        BulkLoadHeadEntity head = headRepository.findForUpdate(clean(bulkLoadCod));
        if (head == null) {
            throw new IllegalArgumentException("No existe la carga masiva");
        }
        if (!isCorrectableValidationError(head)) {
            throw new IllegalStateException(
                    "Solo se puede reemplazar una carga con errores de validacion "
                            + "que aun no haya iniciado su procesamiento"
            );
        }
        if (!Objects.equals(head.BulkLoadType, request.BulkLoadType)) {
            throw new IllegalArgumentException(
                    "El tipo del archivo corregido debe coincidir con la carga original"
            );
        }

        String userCod = getUserCod();
        detRepository.deleteByCode(head.BulkLoadCod);
        destinationRepository.deleteByCode(head.BulkLoadCod);
        resetHeadForCorrection(head, request, userCod);
        return persistPrepared(head, prepared, userCod);
    }

    private BulkLoadRegisterDto persistPrepared(BulkLoadHeadEntity head,
                                                BulkLoadPreparedDto prepared,
                                                String userCod) {
        String bulkLoadCod = head.BulkLoadCod;
        headRepository.save(head);

        List<BulkLoadDestinationEntity> destinations = saveDestinations(
                bulkLoadCod, prepared.StoreCodList, userCod
        );
        List<BulkLoadDetEntity> details = saveDetails(
                bulkLoadCod, prepared.DetailList, userCod
        );
        updateDestinationCounters(bulkLoadCod, details);

        head.NumDestinations = destinations.size();
        head.NumTotalDetails = details.size();
        head.NumErrorDetails = (int) details.stream()
                .filter(item -> BulkLoadConstants.ERROR.equals(item.ProcessStatus))
                .count();
        head.NumWarningDetails = (int) details.stream()
                .filter(item -> item.WarningDetail != null && !item.WarningDetail.isEmpty())
                .count();
        if (!prepared.ErrorList.isEmpty() && head.NumErrorDetails == 0) {
            head.NumErrorDetails = prepared.ErrorList.size();
        }
        head.ValidationDate = new Date();

        if (prepared.ErrorList.isEmpty()) {
            head.ProcessStatus = BulkLoadConstants.PENDING;
            head.StatusMessage = "Archivo validado. Pendiente de confirmacion";
        } else {
            head.ProcessStatus = BulkLoadConstants.ERROR;
            head.StatusMessage = "La carga contiene " + prepared.ErrorList.size()
                    + " error(es). Corrija el archivo y vuelva a cargarlo";
            markDestinationsWithValidationError(destinations, userCod);
        }
        head.addSessionModify(userCod);
        headRepository.save(head);
        return new BulkLoadRegisterDto(head, destinations, prepared.ErrorList);
    }

    private void validatePrepared(BulkLoadPreparedDto prepared) {
        if (prepared == null) {
            throw new IllegalArgumentException("La preparacion de la carga es obligatoria");
        }
    }

    private BulkLoadHeadEntity buildHead(String bulkLoadCod,
                                         BulkLoadParsedRequestDto request,
                                         String userCod) {
        BulkLoadHeadEntity head = new BulkLoadHeadEntity();
        head.BulkLoadCod = bulkLoadCod;
        head.BulkLoadType = request.BulkLoadType;
        head.SchemaVersion = request.SchemaVersion == null ? 1 : request.SchemaVersion;
        head.ProcessStatus = BulkLoadConstants.VALIDATING;
        head.OriginalFileName = trimToLength(request.OriginalFileName, 255);
        head.NumSourceRows = request.RowList == null ? 0 : request.RowList.size();
        head.NumDestinations = 0;
        head.NumTotalDetails = 0;
        head.NumProcessedDetails = 0;
        head.NumSuccessDetails = 0;
        head.NumErrorDetails = 0;
        head.NumWarningDetails = 0;
        head.ProgressPercent = BigDecimal.ZERO.setScale(2);
        head.AttemptCount = 0;
        head.Parameters = new LinkedHashMap<>();
        head.Parameters.put("sourceRead", "FRONTEND_XLSX");
        head.Parameters.put("commitSize", BulkLoadConstants.CHUNK_SIZE);
        head.Status = StatusConst.ACTIVE;
        head.addSessionCreate(userCod);
        return head;
    }

    private void resetHeadForCorrection(BulkLoadHeadEntity head,
                                        BulkLoadParsedRequestDto request,
                                        String userCod) {
        head.SchemaVersion = request.SchemaVersion == null ? 1 : request.SchemaVersion;
        head.ProcessStatus = BulkLoadConstants.VALIDATING;
        head.SourceFileCod = null;
        head.ErrorFileCod = null;
        head.OriginalFileName = trimToLength(request.OriginalFileName, 255);
        head.FileHash = null;
        head.NumSourceRows = request.RowList == null ? 0 : request.RowList.size();
        head.NumDestinations = 0;
        head.NumTotalDetails = 0;
        head.NumProcessedDetails = 0;
        head.NumSuccessDetails = 0;
        head.NumErrorDetails = 0;
        head.NumWarningDetails = 0;
        head.ProgressPercent = BigDecimal.ZERO.setScale(2);
        head.ValidationDate = null;
        head.QueueDate = null;
        head.StartDate = null;
        head.EndDate = null;
        head.LastHeartbeatDate = null;
        head.StatusMessage = "Validando archivo corregido";
        head.AttemptCount = 0;

        Map<String, Object> parameters = head.Parameters == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(head.Parameters);
        parameters.put("sourceRead", "FRONTEND_XLSX");
        parameters.put("commitSize", BulkLoadConstants.CHUNK_SIZE);
        parameters.put(
                "correctionCount",
                integerValue(parameters.get("correctionCount")) + 1
        );
        head.Parameters = parameters;
        head.Status = StatusConst.ACTIVE;
        head.addSessionModify(userCod);
    }

    private boolean isCorrectableValidationError(BulkLoadHeadEntity head) {
        return BulkLoadConstants.ERROR.equals(head.ProcessStatus)
                && value(head.NumProcessedDetails) == 0
                && head.QueueDate == null
                && head.StartDate == null;
    }

    private List<BulkLoadDestinationEntity> saveDestinations(
            String bulkLoadCod,
            List<String> storeCodList,
            String userCod
    ) {
        List<BulkLoadDestinationEntity> result = new ArrayList<>();
        if (storeCodList == null) return result;
        for (String storeCod : storeCodList.stream().distinct().toList()) {
            BulkLoadDestinationEntity destination = new BulkLoadDestinationEntity();
            destination.BulkLoadCod = bulkLoadCod;
            destination.StoreCod = storeCod;
            destination.ProcessStatus = BulkLoadConstants.PENDING;
            destination.NumTotalDetails = 0;
            destination.NumProcessedDetails = 0;
            destination.NumSuccessDetails = 0;
            destination.NumErrorDetails = 0;
            destination.Status = StatusConst.ACTIVE;
            destination.addSessionCreate(userCod);
            result.add(destinationRepository.save(destination));
        }
        return result;
    }

    private List<BulkLoadDetEntity> saveDetails(
            String bulkLoadCod,
            List<BulkLoadPreparedDetailDto> preparedDetails,
            String userCod
    ) {
        List<BulkLoadDetEntity> result = new ArrayList<>();
        if (preparedDetails == null) return result;
        int itemNumber = 1;
        for (BulkLoadPreparedDetailDto prepared : preparedDetails) {
            BulkLoadDetEntity detail = new BulkLoadDetEntity();
            detail.BulkLoadCod = bulkLoadCod;
            detail.ItemNumber = itemNumber++;
            detail.SourceRowNumber = prepared.SourceRowNumber == null
                    || prepared.SourceRowNumber < 1 ? 1 : prepared.SourceRowNumber;
            detail.StoreCod = prepared.StoreCod;
            detail.BusinessKey = trimToLength(prepared.BusinessKey, 128);
            detail.Payload = prepared.Payload == null
                    ? new LinkedHashMap<>() : new LinkedHashMap<>(prepared.Payload);
            detail.ErrorDetail = toMapList(prepared.ErrorList);
            detail.WarningDetail = toMapList(prepared.WarningList);
            detail.ResultData = null;
            detail.ProcessStatus = detail.ErrorDetail == null
                    ? BulkLoadConstants.PENDING : BulkLoadConstants.ERROR;
            detail.AttemptCount = 0;
            detail.Status = StatusConst.ACTIVE;
            detail.addSessionCreate(userCod);
            result.add(detRepository.save(detail));
        }
        return result;
    }

    private void updateDestinationCounters(String bulkLoadCod,
                                           List<BulkLoadDetEntity> details) {
        Map<String, Integer> totalByStore = new HashMap<>();
        Map<String, Integer> errorByStore = new HashMap<>();
        for (BulkLoadDetEntity detail : details) {
            if (detail.StoreCod == null) continue;
            totalByStore.merge(detail.StoreCod, 1, Integer::sum);
            if (BulkLoadConstants.ERROR.equals(detail.ProcessStatus)) {
                errorByStore.merge(detail.StoreCod, 1, Integer::sum);
            }
        }
        for (BulkLoadDestinationEntity destination
                : destinationRepository.findByCode(bulkLoadCod)) {
            destination.NumTotalDetails = totalByStore.getOrDefault(
                    destination.StoreCod, 0
            );
            destination.NumErrorDetails = errorByStore.getOrDefault(
                    destination.StoreCod, 0
            );
            destinationRepository.save(destination);
        }
    }

    private void markDestinationsWithValidationError(
            List<BulkLoadDestinationEntity> destinations,
            String userCod
    ) {
        for (BulkLoadDestinationEntity destination : destinations) {
            destination.ProcessStatus = BulkLoadConstants.ERROR;
            destination.StatusMessage = "La validacion contiene errores";
            destination.addSessionModify(userCod);
            destinationRepository.save(destination);
        }
    }

    private List<Map<String, Object>> toMapList(List<BulkLoadErrorDto> errorList) {
        if (errorList == null || errorList.isEmpty()) return null;
        return errorList.stream().map(BulkLoadErrorDto::toMap).toList();
    }

    private String trimToLength(String value, int length) {
        String cleanValue = value == null ? "" : value.trim();
        return cleanValue.length() <= length
                ? cleanValue : cleanValue.substring(0, length);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private int integerValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return 0;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
