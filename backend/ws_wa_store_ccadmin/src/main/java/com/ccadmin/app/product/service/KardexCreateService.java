package com.ccadmin.app.product.service;

import com.ccadmin.app.product.repository.KardexRepository;
import com.ccadmin.app.shared.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KardexCreateService extends SessionService {

    @Autowired
    private KardexRepository kardexRepository;

    public void saveForSale(){

    }

    public void saveForPucharse(){

    }
}
