package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.store.shared.WarehouseShared;
import com.ccadmin.app.system.utility.StringUtil;
import com.ccadmin.app.transfer.model.dto.TransferRequestDetailDto;
import com.ccadmin.app.transfer.model.dto.TransferSearchDto;
import com.ccadmin.app.transfer.model.entity.TransferHeadEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestHeadEntity;
import com.ccadmin.app.transfer.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransferRequestSearchService extends SessionService {

    @Autowired
    private TransferRequestHeadRepository transferRequestHeadRepository;
    @Autowired
    private TransferRequestDetRepository transferRequestDetRepository;
    @Autowired
    private TransferHeadRepository transferHeadRepository;
    @Autowired
    private TransferDetRepository transferDetRepository;
    @Autowired
    private TransferDocumentRepository transferDocumentRepository;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private WarehouseShared warehouseShared;

    public TransferRequestDetailDto findByTransferCod(String transferCod) {
        TransferRequestDetailDto detail = new TransferRequestDetailDto();

        TransferRequestHeadEntity headTe = this.transferRequestHeadRepository.findById(
                transferCod
        ).orElse(null);
        TransferHeadEntity headTs = this.transferHeadRepository.findById(
                transferCod
        ).orElse(null);

        detail.transferHeadRequest = headTe;
        detail.transferHead = headTs;

        if (headTe != null) {
            detail.transferDetRequestList = this.transferRequestDetRepository.findByTransferCod(
                    transferCod
            );
        } else {
            detail.transferDetRequestList = new ArrayList<>();
        }

        if (headTs != null) {
            detail.transferDetList = this.transferDetRepository.findByTransferCod(
                    transferCod
            );
            detail.transferDocumentList = this.transferDocumentRepository.findByTransferCod(
                    transferCod
            );
        } else {
            detail.transferDetList = new ArrayList<>();
            detail.transferDocumentList = new ArrayList<>();
        }

        List<String> productList = new ArrayList<>();
        detail.transferDetRequestList.forEach(det -> productList.add(det.ProductCod));
        detail.transferDetList.forEach(det -> productList.add(det.ProductCod));

        if (!productList.isEmpty()) {
            List<ProductEntity> products = this.productShared.findAllById(productList.stream().distinct().toList());
            detail.transferDetRequestList.forEach(det -> det.Product = products.stream()
                    .filter(product -> product.ProductCod.equals(det.ProductCod))
                    .findFirst()
                    .orElse(null));
            detail.transferDetList.forEach(det -> det.Product = products.stream()
                    .filter(product -> product.ProductCod.equals(det.ProductCod))
                    .findFirst()
                    .orElse(null));
        }

        return detail;
    }

    public ResponseWsDto findDataForm(String TransferReqCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (StringUtil.isNotEmpty(TransferReqCod)) {
            rpt.AddResponseAdditional("transferDetail", findByTransferCod(TransferReqCod));
        }
        rpt.AddResponseAdditional("storeList", this.storeShared.findAll());
        rpt.AddResponseAdditional("warehouseList", this.warehouseShared.findAll());
        return rpt;
    }

    public ResponseWsDto findDataPrint(String transferCod) {
        ResponseWsDto rpt = new ResponseWsDto();
        TransferRequestDetailDto detail = findByTransferCod(transferCod);

        rpt.AddResponseAdditional("transferDetail", detail);
        if (detail.transferHeadRequest != null) {
            StoreInfoDto storeOrigin = this.storeShared.findStoreInfo(detail.transferHeadRequest.StoreCodOrigin);
            StoreInfoDto storeDest = this.storeShared.findStoreInfo(detail.transferHeadRequest.StoreCodDest);
            rpt.AddResponseAdditional("storeOrigin", storeOrigin);
            rpt.AddResponseAdditional("storeDest", storeDest);
        }
        return rpt;
    }

    public ResponsePageSearch findAll(TransferSearchDto searchDto) {
        TransferSearchDto search = (searchDto == null) ? new TransferSearchDto() : searchDto;

        String transferCod = StringUtil.nvl(search.TransferCod,"").trim();
        String storeCodOrigin = StringUtil.nvl(search.StoreCodOrigin,"").trim();
        String storeCodDest = StringUtil.nvl(search.StoreCodDest,"").trim();
        String transferStatus = StringUtil.nvl(search.TransferStatus,"").trim();
        String typeOperation = StringUtil.nvl(search.TypeOperation,"").trim();
        String storeCodRequestedBy = StringUtil.nvl(search.StoreCodRequestedBy,"").trim();
        String dateStart = StringUtil.nvlEmpty(search.DateStart,null);
        String dateEnd = StringUtil.nvlEmpty(search.DateEnd,null);

        int limit = 10;
        int page = (search.Page <= 0) ? 1 : search.Page;
        int init = (page - 1) * limit;

        int total = this.transferRequestHeadRepository.countByFilters(
                transferCod,
                storeCodOrigin,
                storeCodDest,
                transferStatus,
                typeOperation,
                storeCodRequestedBy,
                dateStart,
                dateEnd
        );

        List<TransferRequestHeadEntity> result = this.transferRequestHeadRepository.findByFilters(
                transferCod,
                storeCodOrigin,
                storeCodDest,
                transferStatus,
                typeOperation,
                storeCodRequestedBy,
                dateStart,
                dateEnd,
                init,
                limit
        );

        return new ResponsePageSearch(result, page, limit, total);
    }

    private String normalize(String value) {
        return (value == null) ? "" : value.trim();
    }
}
