package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.pucharse.model.dto.PucharseRequestDetSaveDto;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestHeadEntity;
import com.ccadmin.app.pucharse.model.factory.PucharseRequestDetEntityFactory;
import com.ccadmin.app.pucharse.model.factory.PucharseRequestDetIdFactory;
import com.ccadmin.app.pucharse.repository.PucharseRequestDetRepository;
import com.ccadmin.app.pucharse.repository.PucharseRequestHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PucharseRequestDetService extends SessionService {

    @Autowired
    private PucharseRequestHeadRepository pucharseRequestHeadRepository;
    @Autowired
    private PucharseRequestDetRepository pucharseRequestDetRepository;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;

    @Transactional
    public PucharseRequestDetSaveDto save(PucharseRequestDetSaveDto request)
    {
        PucharseRequestHeadEntity headboard = findPendingHead(request);
        request.Detail = this.pucharseRequestDetRepository.save(
                prepareDetailToSave(request.Detail, headboard.PucharseReqCod)
        );
        request.Headboard = refreshTotal(headboard);
        return request;
    }

    public PucharseRequestDetEntity prepareDetailToSave(
            PucharseRequestDetEntity source,
            String PucharseReqCod
    )
    {
        source.PucharseReqCod = PucharseReqCod;
        if (source.Variant == null || source.Variant.trim().isEmpty()) {
            source.Variant = "0000";
        }

        ProductConfigEntity config = this.productOperationConfigShared.findByProduct(source.ProductCod, getStoreCod());
        if (this.productOperationConfigShared.isDigital(config)) {
            throw new IllegalArgumentException(
                    "El producto " + source.ProductCod + " es digital y no puede utilizarse en compras"
            );
        }
        if (source.ProductUnitName == null || source.ProductUnitName.trim().isEmpty()) {
            source.ProductUnitName = config.ProductUnitName;
        }
        if (source.ProductUnitFactor <= 0) {
            source.ProductUnitFactor = config.ProductUnitFactor;
        }

        this.productOperationConfigShared.validateInternalQuantity(source.ProductCod, source.NumUnit, source.ProductUnitFactor);
        source.NumUnitPrice = source.NumUnitPrice == null ? BigDecimal.ZERO : source.NumUnitPrice;
        source.NumTotalPrice = source.NumUnitPrice.multiply(new BigDecimal(source.NumUnit));

        PucharseRequestDetEntity current = this.pucharseRequestDetRepository
                .findById(PucharseRequestDetIdFactory.fromEntity(source))
                .orElse(null);
        PucharseRequestDetEntity detail =
                PucharseRequestDetEntityFactory.fromSaveRequest(
                        source, current
                );
        boolean isNew = detail.CreationUser == null || detail.CreationUser.trim().isEmpty();
        detail.addSession(getUserCod(), isNew);

        return detail;
    }

    @Transactional
    public PucharseRequestDetSaveDto delete(PucharseRequestDetSaveDto request)
    {
        PucharseRequestHeadEntity headboard = findPendingHead(request);
        request.Detail.PucharseReqCod = headboard.PucharseReqCod;

        PucharseRequestDetEntity detailDb = this.pucharseRequestDetRepository
                .findById(PucharseRequestDetIdFactory.fromEntity(request.Detail))
                .get();
        detailDb.inactive(getUserCod());
        request.Detail = this.pucharseRequestDetRepository.save(detailDb);
        request.Headboard = refreshTotal(headboard);
        return request;
    }

    private PucharseRequestHeadEntity findPendingHead(PucharseRequestDetSaveDto request)
    {
        if (request == null || request.Headboard == null || request.Detail == null) {
            throw new IllegalArgumentException("La cabecera y el detalle son obligatorios");
        }

        PucharseRequestHeadEntity headboard = this.pucharseRequestHeadRepository.findById(request.Headboard.PucharseReqCod).get();
        if (!StatusConst.PENDING.equals(headboard.PurchaseStatus)) {
            throw new RuntimeException("La solicitud de compra ya no esta pendiente");
        }
        return headboard;
    }

    private PucharseRequestHeadEntity refreshTotal(PucharseRequestHeadEntity headboard)
    {
        headboard.NumTotalPrice = this.pucharseRequestDetRepository.sumActiveTotal(headboard.PucharseReqCod);
        headboard.addSession(getUserCod(), false);
        return this.pucharseRequestHeadRepository.save(headboard);
    }
}
