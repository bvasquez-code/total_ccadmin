package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.BulkLoadDetailSearchDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadRegisterDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadSearchDto;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.bulkload.repository.BulkLoadDestinationRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadDetRepository;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BulkLoadSearchService {
    private final BulkLoadHeadRepository headRepository;
    private final BulkLoadDestinationRepository destinationRepository;
    private final BulkLoadDetRepository detRepository;

    public BulkLoadSearchService(BulkLoadHeadRepository headRepository,
                                 BulkLoadDestinationRepository destinationRepository,
                                 BulkLoadDetRepository detRepository) {
        this.headRepository = headRepository;
        this.destinationRepository = destinationRepository;
        this.detRepository = detRepository;
    }

    public ResponsePageSearchT<BulkLoadHeadEntity> findAll(BulkLoadSearchDto request) {
        if (request == null) request = new BulkLoadSearchDto();
        int page = Math.max(1, request.Page);
        String query = clean(request.Query);
        String type = clean(request.BulkLoadType);
        String processStatus = clean(request.ProcessStatus);
        int total = headRepository.countSearch(
                query, type, processStatus, request.DateStart, request.DateEnd
        );
        List<BulkLoadHeadEntity> result = headRepository.search(
                query, type, processStatus, request.DateStart, request.DateEnd,
                (page - 1) * BulkLoadConstants.PAGE_SIZE, BulkLoadConstants.PAGE_SIZE
        );
        return new ResponsePageSearchT<>(result, page, BulkLoadConstants.PAGE_SIZE, total);
    }

    public BulkLoadRegisterDto findById(String code) {
        BulkLoadHeadEntity head = headRepository.findById(clean(code))
                .orElseThrow(() -> new IllegalArgumentException("No existe la carga masiva"));
        return new BulkLoadRegisterDto(head, destinationRepository.findByCode(head.BulkLoadCod));
    }

    public ResponsePageSearchT<BulkLoadDetEntity> findDetails(BulkLoadDetailSearchDto request) {
        if (request == null || clean(request.BulkLoadCod).isBlank()) {
            throw new IllegalArgumentException("Codigo de carga masiva requerido");
        }
        if (!headRepository.existsById(request.BulkLoadCod)) {
            throw new IllegalArgumentException("No existe la carga masiva");
        }
        int page = Math.max(1, request.Page);
        String storeCod = clean(request.StoreCod);
        String processStatus = clean(request.ProcessStatus);
        int total = detRepository.countSearch(request.BulkLoadCod, storeCod, processStatus);
        List<BulkLoadDetEntity> result = detRepository.search(
                request.BulkLoadCod, storeCod, processStatus,
                (page - 1) * BulkLoadConstants.DETAIL_PAGE_SIZE,
                BulkLoadConstants.DETAIL_PAGE_SIZE
        );
        return new ResponsePageSearchT<>(
                result, page, BulkLoadConstants.DETAIL_PAGE_SIZE, total
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
