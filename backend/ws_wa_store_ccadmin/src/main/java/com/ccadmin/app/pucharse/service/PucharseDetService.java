package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.pucharse.exception.PucharseException;
import com.ccadmin.app.pucharse.model.dto.PucharseDetConfirmDto;
import com.ccadmin.app.pucharse.model.dto.PucharseDetLotConfirmDto;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;
import com.ccadmin.app.pucharse.model.entity.id.PucharseDetId;
import com.ccadmin.app.pucharse.repository.PucharseDetDeliveryRepository;
import com.ccadmin.app.pucharse.repository.PucharseDetRepository;
import com.ccadmin.app.pucharse.repository.PucharseHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PucharseDetService extends SessionService {
    @Autowired
    private PucharseHeadRepository pucharseHeadRepository;
    @Autowired
    private PucharseDetRepository pucharseDetRepository;
    @Autowired
    private PucharseDetDeliveryRepository pucharseDetDeliveryRepository;
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;
    @Transactional
    public PucharseDetConfirmDto confirm(PucharseDetConfirmDto pucharseDetConfirm) throws PucharseException {

        if (pucharseDetConfirm == null || pucharseDetConfirm.pucharseDet == null
                || pucharseDetConfirm.pucharseDetDelivery == null) {
            throw new PucharseException("El detalle de recepcion es obligatorio");
        }
        String purchaseCod = pucharseDetConfirm.pucharseDet.PucharseCod;
        int itemNumber = pucharseDetConfirm.pucharseDet.ItemNumber;
        PucharseHeadEntity pucharseHead = this.pucharseHeadRepository.findByIdForUpdate(purchaseCod)
                .orElseThrow(() -> new PucharseException("No existe la compra " + purchaseCod));
        PucharseDetEntity originDet = this.pucharseDetRepository.findByIdForUpdate(purchaseCod, itemNumber)
                .orElseThrow(() -> new PucharseException("No existe el detalle " + itemNumber));
        this.validatePendingReceipt(pucharseHead, originDet);
        this.validateNonDigitalProduct(pucharseHead, originDet);

        PucharseDetDeliveryEntity delivery = pucharseDetConfirm.pucharseDetDelivery;
        delivery.PucharseCod = purchaseCod;
        delivery.ItemNumber = itemNumber;
        delivery.ProductCod = originDet.ProductCod;
        delivery.Variant = originDet.Variant;
        delivery.ProductUnitName = originDet.ProductUnitName;
        delivery.ProductUnitFactor = originDet.ProductUnitFactor;
        delivery.LotNumber = originDet.LotNumber;
        delivery.ExpirationDate = originDet.ExpirationDate;
        originDet.validate();
        delivery.validate();

        originDet.NumUnitDelivered = delivery.NumUnit;
        originDet.IsKardexAffected = "S";
        originDet.addSession(getUserCod(),false);
        delivery.addSession(getUserCod(),false);

        List<KardexEntity> kardexList = this.kardexShared.buildPurchaseReceipt(
                pucharseHead, List.of(delivery), getUserCod()
        );
        List<KardexZoneEntity> kardexZoneList = this.kardexShared.buildZonePurchaseReceipt(
                pucharseHead, List.of(delivery), getUserCod()
        );
        this.pucharseDetRepository.save(originDet);
        this.pucharseDetDeliveryRepository.save(pucharseDetConfirm.pucharseDetDelivery);
        this.kardexShared.saveAll(kardexList, kardexZoneList);

        pucharseDetConfirm.pucharseDet = originDet;
        return pucharseDetConfirm;
    }

    @Transactional
    public PucharseDetLotConfirmDto confirmWithLots(PucharseDetLotConfirmDto pucharseDetLotConfirm) throws PucharseException {

        if (pucharseDetLotConfirm == null || pucharseDetLotConfirm.pucharseDet == null) {
            throw new PucharseException("El detalle de recepcion es obligatorio");
        }
        if (pucharseDetLotConfirm.WarehouseCod == null || pucharseDetLotConfirm.WarehouseCod.isBlank()) {
            throw new PucharseException("El almacen de recepcion es obligatorio");
        }
        if (pucharseDetLotConfirm.lotDetailList == null || pucharseDetLotConfirm.lotDetailList.isEmpty()) {
            throw new PucharseException("Debe ingresar al menos un lote para confirmar la recepcion");
        }

        PucharseHeadEntity pucharseHead = this.pucharseHeadRepository.findByIdForUpdate(
                        pucharseDetLotConfirm.pucharseDet.PucharseCod
                )
                .orElseThrow(() -> new PucharseException(
                        "No existe la compra " + pucharseDetLotConfirm.pucharseDet.PucharseCod
                ));
        PucharseDetId pucharseDetId = new PucharseDetId();
        pucharseDetId.PucharseCod = pucharseDetLotConfirm.pucharseDet.PucharseCod;
        pucharseDetId.ItemNumber = pucharseDetLotConfirm.pucharseDet.ItemNumber;

        PucharseDetEntity originDet = this.pucharseDetRepository.findByIdForUpdate(
                        pucharseDetId.PucharseCod,
                        pucharseDetId.ItemNumber
                )
                .orElseThrow(() -> new PucharseException("No existe el detalle " + pucharseDetId.ItemNumber));
        this.validatePendingReceipt(pucharseHead, originDet);
        this.validateNonDigitalProduct(pucharseHead, originDet);

        int nextItemNumber = this.pucharseDetRepository.findMaxItemNumber(originDet.PucharseCod) + 1;
        List<PucharseDetEntity> detailList = new ArrayList<>();
        List<PucharseDetDeliveryEntity> deliveryList = new ArrayList<>();

        for (int index = 0; index < pucharseDetLotConfirm.lotDetailList.size(); index++) {
            PucharseDetEntity lotDet = pucharseDetLotConfirm.lotDetailList.get(index);
            lotDet.validate();
            int itemNumber = index == 0 ? originDet.ItemNumber : nextItemNumber++;
            PucharseDetEntity detail = PucharseDetEntity.buildLotDetail(originDet, lotDet, itemNumber, index == 0, getUserCod());
            PucharseDetDeliveryEntity delivery = PucharseDetDeliveryEntity.buildLotDelivery(detail, pucharseDetLotConfirm.WarehouseCod, getUserCod());

            detailList.add(detail);
            deliveryList.add(delivery);
        }

        List<KardexEntity> kardexList = this.kardexShared.buildPurchaseReceipt(
                pucharseHead, deliveryList, getUserCod()
        );
        List<KardexZoneEntity> kardexZoneList = this.kardexShared.buildZonePurchaseReceipt(
                pucharseHead, deliveryList, getUserCod()
        );
        this.pucharseDetRepository.saveAll(detailList);
        this.pucharseDetDeliveryRepository.saveAll(deliveryList);
        this.kardexShared.saveAll(kardexList, kardexZoneList);

        pucharseDetLotConfirm.lotDetailList = detailList;
        return pucharseDetLotConfirm;
    }

    private void validatePendingReceipt(
            PucharseHeadEntity pucharseHead,
            PucharseDetEntity detail
    ) throws PucharseException {
        if (!StatusConst.PENDING.equals(pucharseHead.PurchaseStatus)) {
            throw new PucharseException("La recepcion de la compra ya fue finalizada");
        }
        if ("S".equals(detail.IsKardexAffected)) {
            throw new PucharseException("Producto ya fue confirmado como ingresado");
        }
    }

    private void validateNonDigitalProduct(
            PucharseHeadEntity pucharseHead,
            PucharseDetEntity detail
    ) throws PucharseException {
        if (this.productOperationConfigShared.isDigital(detail.ProductCod, pucharseHead.StoreCod)) {
            throw new PucharseException(
                    "El producto " + detail.ProductCod + " es digital y no puede recibirse mediante una compra"
            );
        }
    }

}
