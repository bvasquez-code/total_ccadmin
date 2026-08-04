package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.service.KardexCreateService;
import com.ccadmin.app.pucharse.exception.PucharseException;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferRequestDetEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class KardexShared {

    @Autowired
    private KardexCreateService kardexCreateService;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;

    public List<KardexZoneEntity> buildPresaleReservation(
            PresaleHeadEntity head,
            List<PresaleDetWarehouseEntity> detailList,
            String userCod
    ) {
        return this.kardexCreateService.buildPresaleReservation(
                head, this.stockControlled(detailList, head.StoreCod, item -> item.ProductCod), userCod
        );
    }

    public List<KardexEntity> buildSaleConfirmation(
            SaleHeadEntity head,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) {
        return this.kardexCreateService.buildSaleConfirmation(
                head, this.stockControlled(detailList, head.StoreCod, item -> item.ProductCod), userCod
        );
    }

    public List<KardexZoneEntity> buildZoneSaleConfirmation(
            SaleHeadEntity head,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) throws SaleException {
        return this.kardexCreateService.buildZoneSaleConfirmation(
                head, this.stockControlled(detailList, head.StoreCod, item -> item.ProductCod), userCod
        );
    }

    public List<KardexZoneEntity> buildSaleExpirationRelease(
            SaleHeadEntity head,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) throws SaleException {
        return this.kardexCreateService.buildSaleExpirationRelease(
                head, this.stockControlled(detailList, head.StoreCod, item -> item.ProductCod), userCod
        );
    }

    public List<KardexEntity> buildPurchaseReceipt(
            PucharseHeadEntity head,
            List<PucharseDetDeliveryEntity> detailList,
            String userCod
    ) throws PucharseException {
        this.validatePurchaseProducts(detailList, head.StoreCod, item -> item.ProductCod);
        return this.kardexCreateService.buildPurchaseReceipt(head, detailList, userCod);
    }

    public List<KardexZoneEntity> buildZonePurchaseReceipt(
            PucharseHeadEntity head,
            List<PucharseDetDeliveryEntity> detailList,
            String userCod
    ) throws PucharseException {
        this.validatePurchaseProducts(detailList, head.StoreCod, item -> item.ProductCod);
        return this.kardexCreateService.buildZonePurchaseReceipt(head, detailList, userCod);
    }

    public List<KardexEntity> buildTransferDispatch(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildTransferDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferDispatch(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildZoneTransferDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexEntity> buildTransferRequestDispatch(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildTransferRequestDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferRequestDispatch(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildZoneTransferRequestDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexEntity> buildTransferReceipt(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildTransferReceipt(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferReceipt(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildZoneTransferReceipt(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexEntity> buildTransferRequestReceipt(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildTransferRequestReceipt(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferRequestReceipt(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
        this.validateTransferProducts(detailList, storeCod, item -> item.ProductCod);
        return this.kardexCreateService.buildZoneTransferRequestReceipt(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexEntity> buildCreditNoteConfirmation(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        List<CreditNoteDetEntity> stockDetailList = this.stockControlled(
                detailList, head.StoreCod, item -> item.ProductCod
        );
        return stockDetailList.isEmpty() ? List.of() : this.kardexCreateService.buildCreditNoteConfirmation(
                head, stockDetailList, warehouse, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneCreditNoteConfirmation(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        List<CreditNoteDetEntity> stockDetailList = this.stockControlled(
                detailList, head.StoreCod, item -> item.ProductCod
        );
        return stockDetailList.isEmpty() ? List.of() : this.kardexCreateService.buildZoneCreditNoteConfirmation(
                head, stockDetailList, warehouse, userCod
        );
    }

    public List<KardexEntity> buildCreditNoteRejectedExit(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        List<CreditNoteDetEntity> stockDetailList = this.stockControlled(
                detailList, head.StoreCod, item -> item.ProductCod
        );
        return stockDetailList.isEmpty() ? List.of() : this.kardexCreateService.buildCreditNoteRejectedExit(
                head, stockDetailList, warehouse, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneCreditNoteReturn(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        List<CreditNoteDetEntity> stockDetailList = this.stockControlled(
                detailList, head.StoreCod, item -> item.ProductCod
        );
        return stockDetailList.isEmpty() ? List.of() : this.kardexCreateService.buildZoneCreditNoteReturn(
                head, stockDetailList, warehouse, userCod
        );
    }

    public List<KardexEntity> saveAll(
            List<KardexEntity> kardexList,
            List<KardexZoneEntity> kardexZoneList
    ) {
        return this.kardexCreateService.saveAll(kardexList, kardexZoneList);
    }

    private <T> List<T> stockControlled(
            List<T> detailList,
            String storeCod,
            Function<T, String> productCodExtractor
    ) {
        if (detailList == null || detailList.isEmpty()) {
            return List.of();
        }
        return detailList.stream()
                .filter(item -> !this.productOperationConfigShared.isDigital(
                        productCodExtractor.apply(item), storeCod
                ))
                .toList();
    }

    private <T> void validatePurchaseProducts(
            List<T> detailList,
            String storeCod,
            Function<T, String> productCodExtractor
    ) throws PucharseException {
        String digitalProductCod = this.findDigitalProduct(detailList, storeCod, productCodExtractor);
        if (digitalProductCod != null) {
            throw new PucharseException(
                    "El producto " + digitalProductCod + " es digital y no puede utilizarse en compras"
            );
        }
    }

    private <T> void validateTransferProducts(
            List<T> detailList,
            String storeCod,
            Function<T, String> productCodExtractor
    ) throws TransferException {
        String digitalProductCod = this.findDigitalProduct(detailList, storeCod, productCodExtractor);
        if (digitalProductCod != null) {
            throw new TransferException(
                    "El producto " + digitalProductCod + " es digital y no puede utilizarse en transferencias"
            );
        }
    }

    private <T> String findDigitalProduct(
            List<T> detailList,
            String storeCod,
            Function<T, String> productCodExtractor
    ) {
        if (detailList == null || detailList.isEmpty()) {
            return null;
        }
        return detailList.stream()
                .map(productCodExtractor)
                .filter(productCod -> this.productOperationConfigShared.isDigital(productCod, storeCod))
                .findFirst()
                .orElse(null);
    }

}
