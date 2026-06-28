package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.service.StockZoneMovementService;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockZoneMovementShared {

    @Autowired
    private StockZoneMovementService stockZoneMovementService;

    public void addCreditNoteUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.stockZoneMovementService.addCreditNoteUnavailableStock(creditNoteHead, detailList, warehouse, userCod);
    }

    public void resolveCreditNoteUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.stockZoneMovementService.resolveCreditNoteUnavailableStock(creditNoteHead, detailList, warehouse, userCod);
    }

    public boolean existsCreditNoteUnavailableStock(String creditNoteCod) {
        return this.stockZoneMovementService.existsCreditNoteUnavailableStock(creditNoteCod);
    }
}
