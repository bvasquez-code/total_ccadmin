package com.ccadmin.app.producttraceability.service;

import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.producttraceability.model.constants.ProductTraceabilityConstants;
import com.ccadmin.app.producttraceability.model.dto.ProductTraceabilityOperationDto;
import com.ccadmin.app.producttraceability.repository.ProductTraceabilityRepository;
import com.ccadmin.app.pucharse.model.constants.PucharseConstants;
import com.ccadmin.app.system.shared.TableSequenceShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductTraceabilityTechnicalLotCreateServiceTest {

    @Mock
    private KardexRepository kardexRepository;
    @Mock
    private ProductTraceabilityRepository productTraceabilityRepository;
    @Mock
    private TableSequenceShared tableSequenceShared;
    @InjectMocks
    private ProductTraceabilityTechnicalLotCreateService technicalLotCreateService;

    @Test
    void reservesOneGlobalSequenceForEachUnprocessedInboundKardex() {
        KardexEntity movement = KardexEntity.build(
                "CO001", 1, PucharseConstants.KARDEX_ZONE_SOURCE,
                KardexZoneConstants.TYPE_OPERATION_ADD,
                "PR001", "0000", "T001", "A001", 2,
                null, null, 2, "ADMIN"
        );
        movement.kardexID = 50L;
        ProductTraceabilityOperationDto operation =
                new ProductTraceabilityOperationDto(
                        PucharseConstants.KARDEX_ZONE_SOURCE,
                        "CO001", "T001", null, Map.of(), Map.of()
                );

        when(kardexRepository.findTraceabilityMovements(
                PucharseConstants.KARDEX_ZONE_SOURCE, "CO001", "T001"
        )).thenReturn(List.of(movement));
        when(tableSequenceShared.getNextCode(
                ProductTraceabilityConstants.TECHNICAL_LOT_SEQUENCE
        )).thenReturn("LT000000000000000001");

        ProductTraceabilityOperationDto result =
                technicalLotCreateService.reserveTechnicalLots(operation);

        assertEquals("LT000000000000000001", result.technicalLot(50L));
        verify(tableSequenceShared).getNextCode("product_traceability");
    }
}
