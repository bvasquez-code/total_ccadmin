package com.ccadmin.app.producttraceability.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.producttraceability.model.constants.ProductTraceabilityConstants;
import com.ccadmin.app.producttraceability.model.dto.ProductTraceabilityOperationDto;
import com.ccadmin.app.producttraceability.repository.ProductTraceabilityRepository;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.system.shared.TableSequenceShared;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProductTraceabilityTechnicalLotCreateService {

    private final KardexRepository kardexRepository;
    private final ProductTraceabilityRepository productTraceabilityRepository;
    private final TableSequenceShared tableSequenceShared;

    public ProductTraceabilityTechnicalLotCreateService(
            KardexRepository kardexRepository,
            ProductTraceabilityRepository productTraceabilityRepository,
            TableSequenceShared tableSequenceShared
    ) {
        this.kardexRepository = kardexRepository;
        this.productTraceabilityRepository = productTraceabilityRepository;
        this.tableSequenceShared = tableSequenceShared;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProductTraceabilityOperationDto reserveTechnicalLots(
            ProductTraceabilityOperationDto operation
    ) {
        if (this.inboundPreservesTechnicalLot(operation.sourceTable())) {
            return operation;
        }

        Map<Long, String> technicalLotByKardexId = new LinkedHashMap<>();
        for (KardexEntity movement : this.kardexRepository.findTraceabilityMovements(
                operation.sourceTable(), operation.operationCode(), operation.storeCode()
        )) {
            if (!KardexZoneConstants.TYPE_OPERATION_ADD.equals(movement.TypeOperation)
                    || this.productTraceabilityRepository.countByKardexId(movement.kardexID) > 0) {
                continue;
            }
            technicalLotByKardexId.put(movement.kardexID, this.nextTechnicalLot());
        }
        return operation.withTechnicalLots(technicalLotByKardexId);
    }

    private String nextTechnicalLot() {
        String technicalLot = this.tableSequenceShared.getNextCode(
                ProductTraceabilityConstants.TECHNICAL_LOT_SEQUENCE
        );
        if (technicalLot == null
                || !technicalLot.startsWith(ProductTraceabilityConstants.TECHNICAL_LOT_PREFIX)
                || technicalLot.length() != ProductTraceabilityConstants.TECHNICAL_LOT_LENGTH) {
            throw new IllegalStateException(
                    "La secuencia global de lote tecnico no esta configurada correctamente"
            );
        }
        return technicalLot;
    }

    private boolean inboundPreservesTechnicalLot(String sourceTable) {
        return TransferConstants.KARDEX_SOURCE_TABLE.equals(sourceTable)
                || TransferConstants.KARDEX_ZONE_SOURCE_REQUEST.equals(sourceTable)
                || SaleConstants.KARDEX_ZONE_SOURCE_CREDIT_NOTE.equals(sourceTable);
    }
}
