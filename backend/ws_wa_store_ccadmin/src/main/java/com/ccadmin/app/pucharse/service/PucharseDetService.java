package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.pucharse.model.dto.PucharseDetConfirmDto;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.repository.PucharseDetDeliveryRepository;
import com.ccadmin.app.pucharse.repository.PucharseDetRepository;
import com.ccadmin.app.pucharse.repository.PucharseHeadRepository;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public PucharseDetConfirmDto confirm(PucharseDetConfirmDto pucharseDetConfirm){

        PucharseHeadEntity pucharseHead = this.pucharseHeadRepository.findById(pucharseDetConfirm.pucharseDet.PucharseCod).get();
        pucharseDetConfirm.pucharseDetDelivery.PucharseCod = pucharseDetConfirm.pucharseDet.PucharseCod;
        pucharseDetConfirm.pucharseDetDelivery.ItemNumber = pucharseDetConfirm.pucharseDet.ItemNumber;
        pucharseDetConfirm.pucharseDetDelivery.ProductCod = pucharseDetConfirm.pucharseDet.ProductCod;
        pucharseDetConfirm.pucharseDetDelivery.Variant = pucharseDetConfirm.pucharseDet.Variant;
        pucharseDetConfirm.pucharseDetDelivery.LotNumber = pucharseDetConfirm.pucharseDet.LotNumber;
        pucharseDetConfirm.pucharseDetDelivery.ExpirationDate = pucharseDetConfirm.pucharseDet.ExpirationDate;

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
}
