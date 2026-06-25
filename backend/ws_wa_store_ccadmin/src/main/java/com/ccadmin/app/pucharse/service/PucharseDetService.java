package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.shared.KardexShared;
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
    @Transactional
    public PucharseDetConfirmDto confirm(PucharseDetConfirmDto pucharseDetConfirm) throws PucharseException {

        PucharseHeadEntity pucharseHead = this.pucharseHeadRepository.findById(pucharseDetConfirm.pucharseDet.PucharseCod).get();
        pucharseDetConfirm.pucharseDetDelivery.PucharseCod = pucharseDetConfirm.pucharseDet.PucharseCod;
        pucharseDetConfirm.pucharseDetDelivery.ItemNumber = pucharseDetConfirm.pucharseDet.ItemNumber;
        pucharseDetConfirm.pucharseDetDelivery.ProductCod = pucharseDetConfirm.pucharseDet.ProductCod;
        pucharseDetConfirm.pucharseDetDelivery.Variant = pucharseDetConfirm.pucharseDet.Variant;
        pucharseDetConfirm.pucharseDetDelivery.LotNumber = pucharseDetConfirm.pucharseDet.LotNumber;
        pucharseDetConfirm.pucharseDetDelivery.ExpirationDate = pucharseDetConfirm.pucharseDet.ExpirationDate;
        pucharseDetConfirm.pucharseDet.validate();
        pucharseDetConfirm.pucharseDetDelivery.validate();

        KardexEntity kardexLast = this.kardexShared.findLastMovement(
                pucharseDetConfirm.pucharseDet.ProductCod,
                pucharseDetConfirm.pucharseDet.Variant,
                pucharseDetConfirm.pucharseDetDelivery.WarehouseCod,
                pucharseHead.StoreCod
        );
        KardexEntity kardex = new KardexEntity(
                kardexLast,pucharseDetConfirm.pucharseDetDelivery,pucharseHead.StoreCod
        );
        kardex.addSession(getUserCod(),true);

        pucharseDetConfirm.pucharseDet.IsKardexAffected = "S";
        pucharseDetConfirm.pucharseDet.addSession(getUserCod(),false);
        pucharseDetConfirm.pucharseDetDelivery.addSession(getUserCod(),false);

        this.pucharseDetRepository.save(pucharseDetConfirm.pucharseDet);
        this.pucharseDetDeliveryRepository.save(pucharseDetConfirm.pucharseDetDelivery);
        this.kardexShared.save(kardex);

        return pucharseDetConfirm;
    }

    @Transactional
    public PucharseDetLotConfirmDto confirmWithLots(PucharseDetLotConfirmDto pucharseDetLotConfirm) throws PucharseException {

        if (pucharseDetLotConfirm.lotDetailList == null || pucharseDetLotConfirm.lotDetailList.isEmpty()) {
            throw new RuntimeException("Debe ingresar al menos un lote para confirmar la recepcion");
        }

        PucharseHeadEntity pucharseHead = this.pucharseHeadRepository.findById(pucharseDetLotConfirm.pucharseDet.PucharseCod).get();
        PucharseDetId pucharseDetId = new PucharseDetId();
        pucharseDetId.PucharseCod = pucharseDetLotConfirm.pucharseDet.PucharseCod;
        pucharseDetId.ItemNumber = pucharseDetLotConfirm.pucharseDet.ItemNumber;

        PucharseDetEntity originDet = this.pucharseDetRepository.findById(pucharseDetId).get();

        if ("S".equals(originDet.IsKardexAffected)) {
            throw new RuntimeException("Producto ya fue confirmado como ingresado");
        }

        int nextItemNumber = this.pucharseDetRepository.findMaxItemNumber(originDet.PucharseCod) + 1;
        List<PucharseDetEntity> detailList = new ArrayList<>();
        List<PucharseDetDeliveryEntity> deliveryList = new ArrayList<>();
        List<KardexEntity> kardexList = new ArrayList<>();
        KardexEntity kardexLast = this.kardexShared.findLastMovement(
                originDet.ProductCod,
                originDet.Variant,
                pucharseDetLotConfirm.WarehouseCod,
                pucharseHead.StoreCod
        );

        for (int index = 0; index < pucharseDetLotConfirm.lotDetailList.size(); index++) {
            PucharseDetEntity lotDet = pucharseDetLotConfirm.lotDetailList.get(index);
            lotDet.validate();
            int itemNumber = index == 0 ? originDet.ItemNumber : nextItemNumber++;
            PucharseDetEntity detail = PucharseDetEntity.buildLotDetail(originDet, lotDet, itemNumber, index == 0, getUserCod());
            PucharseDetDeliveryEntity delivery = PucharseDetDeliveryEntity.buildLotDelivery(detail, pucharseDetLotConfirm.WarehouseCod, getUserCod());

            KardexEntity kardex = new KardexEntity(kardexLast, delivery, pucharseHead.StoreCod);
            kardex.addSession(getUserCod(), true);
            kardexLast = kardex;

            detailList.add(detail);
            deliveryList.add(delivery);
            kardexList.add(kardex);
        }

        this.pucharseDetRepository.saveAll(detailList);
        this.pucharseDetDeliveryRepository.saveAll(deliveryList);
        this.kardexShared.saveAll(kardexList);

        pucharseDetLotConfirm.lotDetailList = detailList;
        return pucharseDetLotConfirm;
    }

}
