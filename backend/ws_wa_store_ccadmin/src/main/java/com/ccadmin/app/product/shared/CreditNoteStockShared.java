package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.service.CreditNoteStockService;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CreditNoteStockShared {

    @Autowired
    private CreditNoteStockService creditNoteStockService;

    public void addUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.creditNoteStockService.addUnavailableStock(creditNoteHead, detailList, warehouse, userCod);
    }

    public void resolveUnavailableStock(
            CreditNoteHeadEntity creditNoteHead,
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouse,
            String userCod
    ) throws SaleException {
        this.creditNoteStockService.resolveUnavailableStock(creditNoteHead, detailList, warehouse, userCod);
    }
}
