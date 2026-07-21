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

@Service
public class KardexShared {

    @Autowired
    private KardexCreateService kardexCreateService;

    public List<KardexZoneEntity> buildPresaleReservation(
            PresaleHeadEntity head,
            List<PresaleDetWarehouseEntity> detailList,
            String userCod
    ) {
        return this.kardexCreateService.buildPresaleReservation(head, detailList, userCod);
    }

    public List<KardexEntity> buildSaleConfirmation(
            SaleHeadEntity head,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) {
        return this.kardexCreateService.buildSaleConfirmation(head, detailList, userCod);
    }

    public List<KardexZoneEntity> buildZoneSaleConfirmation(
            SaleHeadEntity head,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) throws SaleException {
        return this.kardexCreateService.buildZoneSaleConfirmation(head, detailList, userCod);
    }

    public List<KardexZoneEntity> buildSaleExpirationRelease(
            SaleHeadEntity head,
            List<SaleDetWarehouseEntity> detailList,
            String userCod
    ) throws SaleException {
        return this.kardexCreateService.buildSaleExpirationRelease(head, detailList, userCod);
    }

    public List<KardexEntity> buildPurchaseReceipt(
            PucharseHeadEntity head,
            List<PucharseDetDeliveryEntity> detailList,
            String userCod
    ) throws PucharseException {
        return this.kardexCreateService.buildPurchaseReceipt(head, detailList, userCod);
    }

    public List<KardexZoneEntity> buildZonePurchaseReceipt(
            PucharseHeadEntity head,
            List<PucharseDetDeliveryEntity> detailList,
            String userCod
    ) throws PucharseException {
        return this.kardexCreateService.buildZonePurchaseReceipt(head, detailList, userCod);
    }

    public List<KardexEntity> buildTransferDispatch(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        return this.kardexCreateService.buildTransferDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferDispatch(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        return this.kardexCreateService.buildZoneTransferDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexEntity> buildTransferRequestDispatch(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
        return this.kardexCreateService.buildTransferRequestDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferRequestDispatch(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
        return this.kardexCreateService.buildZoneTransferRequestDispatch(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexEntity> buildTransferReceipt(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        return this.kardexCreateService.buildTransferReceipt(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferReceipt(
            String operationCod, String storeCod,
            List<TransferDetEntity> detailList, String userCod
    ) throws TransferException {
        return this.kardexCreateService.buildZoneTransferReceipt(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexEntity> buildTransferRequestReceipt(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
        return this.kardexCreateService.buildTransferRequestReceipt(
                operationCod, storeCod, detailList, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneTransferRequestReceipt(
            String operationCod, String storeCod,
            List<TransferRequestDetEntity> detailList, String userCod
    ) throws TransferException {
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
        return this.kardexCreateService.buildCreditNoteConfirmation(
                head, detailList, warehouse, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneCreditNoteConfirmation(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        return this.kardexCreateService.buildZoneCreditNoteConfirmation(
                head, detailList, warehouse, userCod
        );
    }

    public List<KardexEntity> buildCreditNoteRejectedExit(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        return this.kardexCreateService.buildCreditNoteRejectedExit(
                head, detailList, warehouse, userCod
        );
    }

    public List<KardexZoneEntity> buildZoneCreditNoteReturn(
            CreditNoteHeadEntity head,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        return this.kardexCreateService.buildZoneCreditNoteReturn(
                head, detailList, warehouse, userCod
        );
    }

    public List<KardexEntity> saveAll(
            List<KardexEntity> kardexList,
            List<KardexZoneEntity> kardexZoneList
    ) {
        return this.kardexCreateService.saveAll(kardexList, kardexZoneList);
    }

}
