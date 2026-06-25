package com.ccadmin.app.cash.shared;


import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.model.entity.CashSessionItemEntity;
import com.ccadmin.app.cash.service.CashSessionAdminService;
import com.ccadmin.app.cash.service.CashSessionItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Component
public class CashSessionShared {

    @Autowired
    private CashSessionAdminService cashSessionAdminService;
    @Autowired
    private CashSessionItemService itemService;

    // Abrir caja
    public CashSessionEntity open(String registerCod, String storeCod, String currencyCod,
                                  String commenter, BigDecimal openingFloat) {
        return cashSessionAdminService.open(norm(registerCod), norm(storeCod), norm(currencyCod), commenter, openingFloat);
    }

    // Agregar items (denominaciones, pagos, movimientos)
    public CashSessionItemEntity addItem(CashSessionItemEntity item) {
        return itemService.addItem(item);
    }

    public List<CashSessionItemEntity> addItems(List<CashSessionItemEntity> items) {
        return itemService.addItems(items);
    }

    public List<CashSessionItemEntity> getItems(Long sessionId) {
        return itemService.getItems(sessionId);
    }

    // Cerrar caja
    public CashSessionEntity close(Long sessionId, String commenter) {
        return cashSessionAdminService.close(sessionId, commenter);
    }

    // helpers
    private static String norm(String v){
        return v==null?null:v.trim().toUpperCase();
    }
}
