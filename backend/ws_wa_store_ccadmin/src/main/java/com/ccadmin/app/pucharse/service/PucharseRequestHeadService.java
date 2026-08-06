package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.pucharse.model.dto.PucharseRequestDetailsDto;
import com.ccadmin.app.pucharse.model.dto.PucharseRequestRegisterDto;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestHeadEntity;
import com.ccadmin.app.pucharse.model.factory.PucharseRequestDetailsDtoFactory;
import com.ccadmin.app.pucharse.repository.PucharseRequestDetRepository;
import com.ccadmin.app.pucharse.repository.PucharseRequestHeadRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Service
public class PucharseRequestHeadService extends SessionService {

    public static Logger log = LogManager.getLogger(PucharseRequestHeadService.class);
    @Autowired
    private PucharseRequestHeadRepository pucharseRequestHeadRepository;
    @Autowired
    private PucharseRequestDetRepository pucharseRequestDetRepository;

    @Autowired
    private CurrencyShared currencyShared;
    private SearchService searchService;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private PucharseRequestDetService pucharseRequestDetService;

    @Transactional
    public PucharseRequestRegisterDto save(PucharseRequestRegisterDto pucharseRegister)
    {
        boolean isNew = ( pucharseRegister.Headboard.PucharseReqCod == null ||  pucharseRegister.Headboard.PucharseReqCod.trim().isEmpty() == true);

        if( isNew  )
        {
            pucharseRegister.Headboard.PucharseReqCod = pucharseRequestHeadRepository.getPucharseReqCod(getStoreCod());
        }
        else
        {
            this.pucharseRequestDetRepository.updateStatusAll(pucharseRegister.Headboard.PucharseReqCod,"I");
        }

        CurrencyEntity currencySystem = this.currencyShared.findCurrencySystem();

        pucharseRegister.Headboard.addSession(getUserCod(),isNew);
        pucharseRegister.Headboard.StoreCod = getStoreCod();
        pucharseRegister.Headboard.CurrencyCodSys = currencySystem.CurrencyCod;
        pucharseRegister.Headboard.NumExchangevalue = new BigDecimal(1);
        pucharseRegister.Headboard.PurchaseStatus = StatusConst.PENDING;

        if(!pucharseRegister.Headboard.CurrencyCodSys.equals(pucharseRegister.Headboard.CurrencyCod))
        {
            if(pucharseRegister.Headboard.CurrencyCod==null || pucharseRegister.Headboard.CurrencyCod.isEmpty())
            {
                pucharseRegister.Headboard.CurrencyCod = pucharseRegister.Headboard.CurrencyCodSys;
            }
            CurrencyEntity currencyPucharse = this.currencyShared.findById(pucharseRegister.Headboard.CurrencyCod);
            pucharseRegister.Headboard.NumExchangevalue = currencyPucharse.NumExchangevalue;
        }

        for(var product : pucharseRegister.DetailList)
        {
            this.pucharseRequestDetService.prepareDetailToSave(
                    product, pucharseRegister.Headboard.PucharseReqCod
            );
        }

        pucharseRegister.Headboard.NumTotalPrice = new BigDecimal(
                pucharseRegister.DetailList
                        .stream()
                        .mapToDouble( e -> e.NumTotalPrice.doubleValue() )
                        .sum()
        );

        this.pucharseRequestHeadRepository.save(pucharseRegister.Headboard);
        this.pucharseRequestDetRepository.saveAll(pucharseRegister.DetailList);

        return pucharseRegister;
    }

    public ResponsePageSearch findAll(String Query,int Page,String StoreCod)
    {
        SearchDto search = new SearchDto(Query,Page,StoreCod);
        this.searchService = new SearchService(this.pucharseRequestHeadRepository);
        return this.searchService.findAllStore(search,10);
    }

    public ResponseWsDto findDataForm(String PucharseReqCod)
    {
        ResponseWsDto rpt = new ResponseWsDto();
        PucharseRequestDetailsDto pucharseRequestDetails = this.findById(PucharseReqCod);

        rpt.AddResponseAdditional("PucharseRequestDetails",pucharseRequestDetails);

        return rpt;
    }

    public PucharseRequestDetailsDto findById(String PucharseReqCod)
    {
        PucharseRequestHeadEntity head = this.pucharseRequestHeadRepository
                .findById(PucharseReqCod)
                .get();
        List<PucharseRequestDetEntity> details =
                this.pucharseRequestDetRepository.findAllActive(PucharseReqCod);

        for(var item : details)
        {
            item.Product = this.productShared.findById(item.ProductCod);
        }

        return PucharseRequestDetailsDtoFactory.fromEntities(head, details);
    }

}
