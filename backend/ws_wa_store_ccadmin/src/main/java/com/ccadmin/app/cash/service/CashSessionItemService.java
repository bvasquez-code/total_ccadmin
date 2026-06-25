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
        item.validate().session(this.getUserCod());
        return itemRepository.save(item);
    }

    public List<CashSessionItemEntity> addItems(List<CashSessionItemEntity> items) {
        items.forEach(i -> i.validate().session(this.getUserCod()));
        return itemRepository.saveAll(items);
    }

    public List<CashSessionItemEntity> getItems(Long sessionId) {
        return itemRepository.findByCashSessionID(sessionId);
    }
}
