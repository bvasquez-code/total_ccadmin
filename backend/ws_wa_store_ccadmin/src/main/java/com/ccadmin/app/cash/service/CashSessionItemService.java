package com.ccadmin.app.cash.service;


import com.ccadmin.app.cash.model.entity.CashSessionItemEntity;
import com.ccadmin.app.cash.repository.CashSessionItemRepository;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CashSessionItemService extends SessionService {

    @Autowired
    private CashSessionItemRepository itemRepository;

    public CashSessionItemEntity addItem(CashSessionItemEntity item) {
        item.CashSessionID = requireCurrentCashSessionId();
        item.validate().session(this.getUserCod());
        return itemRepository.save(item);
    }

    public List<CashSessionItemEntity> addItems(List<CashSessionItemEntity> items) {
        Long cashSessionId = requireCurrentCashSessionId();
        items.forEach(item -> {
            item.CashSessionID = cashSessionId;
            item.validate().session(this.getUserCod());
        });
        return itemRepository.saveAll(items);
    }

    public List<CashSessionItemEntity> getItems(Long sessionId) {
        return itemRepository.findByCashSessionID(sessionId);
    }

    private Long requireCurrentCashSessionId() {
        Long cashSessionId = getCashSessionID();
        if (cashSessionId == null) {
            throw new IllegalStateException("La sesión autenticada no tiene una caja abierta");
        }
        return cashSessionId;
    }
}
