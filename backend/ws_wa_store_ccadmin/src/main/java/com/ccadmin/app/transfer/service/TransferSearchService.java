package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.store.shared.WarehouseShared;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.dto.TransferDetailDto;
import com.ccadmin.app.transfer.model.dto.TransferSearchDto;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import com.ccadmin.app.transfer.model.entity.TransferHeadEntity;
import com.ccadmin.app.transfer.repository.TransferDetRepository;
import com.ccadmin.app.transfer.repository.TransferDocumentRepository;
import com.ccadmin.app.transfer.repository.TransferHeadRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransferSearchService extends SessionService {

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

    public TransferDetailDto findByTransferCod(String transferCod) {
        TransferDetailDto detail = new TransferDetailDto();

        TransferHeadEntity headTe = this.transferHeadRepository.findByTransferCodAndTypeOperation(
                transferCod, TransferConstants.TYPE_OPERATION_REQUEST
        );
        TransferHeadEntity headTs = this.transferHeadRepository.findByTransferCodAndTypeOperation(
                transferCod, TransferConstants.TYPE_OPERATION_SEND
        );

        detail.transferHeadTe = headTe;
        detail.transferHeadTs = headTs;

        if (headTe != null) {
            detail.transferDetTeList = this.transferDetRepository.findByTransferCodAndTypeOperation(
                    transferCod, TransferConstants.TYPE_OPERATION_REQUEST
            );
        } else {
            detail.transferDetTeList = new ArrayList<>();
        }

        if (headTs != null) {
            detail.transferDetTsList = this.transferDetRepository.findByTransferCodAndTypeOperation(
                    transferCod, TransferConstants.TYPE_OPERATION_SEND
            );
            detail.transferDocumentList = this.transferDocumentRepository.findByTransferCodAndTypeOperation(
                    transferCod, TransferConstants.TYPE_OPERATION_SEND
            );
        } else {
            detail.transferDetTsList = new ArrayList<>();
            detail.transferDocumentList = new ArrayList<>();
        }

        List<String> productList = new ArrayList<>();
        detail.transferDetTeList.forEach(det -> productList.add(det.ProductCod));
        detail.transferDetTsList.forEach(det -> productList.add(det.ProductCod));

        if (!productList.isEmpty()) {
            List<ProductEntity> products = this.productShared.findAllById(productList.stream().distinct().toList());
            detail.transferDetTeList.forEach(det -> det.Product = products.stream()
                    .filter(product -> product.ProductCod.equals(det.ProductCod))
                    .findFirst()
                    .orElse(null));
            detail.transferDetTsList.forEach(det -> det.Product = products.stream()
                    .filter(product -> product.ProductCod.equals(det.ProductCod))
                    .findFirst()
                    .orElse(null));
        }

        return detail;
    }

    public ResponseWsDto findDataForm(String transferCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (StringUtil.isNotEmpty(transferCod)) {
            rpt.AddResponseAdditional("transferDetail", findByTransferCod(transferCod));
        }
        rpt.AddResponseAdditional("storeList", this.storeShared.findAll());
        rpt.AddResponseAdditional("warehouseList", this.warehouseShared.findAll());
        return rpt;
    }

    public ResponseWsDto findDataPrint(String transferCod) {
        ResponseWsDto rpt = new ResponseWsDto();
        TransferDetailDto detail = findByTransferCod(transferCod);

        rpt.AddResponseAdditional("transferDetail", detail);
        if (detail.transferHeadTe != null) {
            StoreInfoDto storeOrigin = this.storeShared.findStoreInfo(detail.transferHeadTe.StoreCodOrigin);
            StoreInfoDto storeDest = this.storeShared.findStoreInfo(detail.transferHeadTe.StoreCodDest);
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

        int total = this.transferHeadRepository.countByFilters(
                transferCod,
                storeCodOrigin,
                storeCodDest,
                transferStatus,
                typeOperation,
                storeCodRequestedBy,
                dateStart,
                dateEnd
        );

        List<TransferHeadEntity> result = this.transferHeadRepository.findByFilters(
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
