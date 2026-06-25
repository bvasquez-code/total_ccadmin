package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.repository.PresaleDetRepository;
import com.ccadmin.app.sale.repository.PresaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.system.shared.CurrencyShared;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PresaleSearchService {

    @Autowired
    private PresaleHeadRepository presaleHeadRepository;
    @Autowired
    private ClientShared clientShared;
    @Autowired
    private PresaleDetRepository presaleDetRepository;
    @Autowired
    private PresaleDetWarehouseRepository presaleDetWarehouseRepository;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private CurrencyShared currencyShared;

    private SearchTService searchService;



    public ResponsePageSearchT<PresaleHeadEntity> findAll(String Query, int Page, String StoreCod)
    {
        this.searchService = new SearchTService(this.presaleHeadRepository);
        SearchDto search = new SearchDto(Query,Page,StoreCod);
        ResponsePageSearchT<PresaleHeadEntity> responsePage = this.searchService.findAllStore(search,10);

        if( responsePage.resultSearch != null )
        {
            List<ClientEntity> clientList = this.clientShared.findAllById(
                    responsePage.resultSearch.stream()
                            .filter( Presale -> Presale.existClient() )
                            .map( PresaleClient -> PresaleClient.ClientCod )
                            .collect(Collectors.toList())
            );

            for (PresaleHeadEntity Presale : responsePage.resultSearch)
            {
                if(Presale.existClient()) {
                    Presale.Client = clientList.stream()
                            .filter( Client -> Client.ClientCod.equals(Presale.ClientCod) )
                            .findFirst()
                            .orElse(null);
                }
            }
        }

        return responsePage;
    }

    public PresaleDetailDto findById(String PresaleCod) {
        PresaleDetailDto  presaleDetail = new PresaleDetailDto();

        presaleDetail.Headboard = this.presaleHeadRepository.findById(PresaleCod).get();
        presaleDetail.DetailList = this.presaleDetRepository.findByPresaleCod(PresaleCod);

        if( presaleDetail.Headboard.existClient() )
        {
            presaleDetail.Headboard.Client = this.clientShared.findById(presaleDetail.Headboard.ClientCod);
        }

        for(var item : presaleDetail.DetailList)
        {
            item.DetailWarehouse = this.presaleDetWarehouseRepository.findByItemNumber(item.PresaleCod,item.ItemNumber);
            item.Product = this.productShared.findById(item.ProductCod);
        }

        return presaleDetail;
    }

    public ResponseWsDto findDataForm(String PresaleCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        rpt.AddResponseAdditional("CurrencySystem",this.currencyShared.findCurrencySystem());
        if(PresaleCod!=null && !PresaleCod.isEmpty()) rpt.AddResponseAdditional("PresaleDetail",this.findById(PresaleCod));

        return rpt;
    }
}
