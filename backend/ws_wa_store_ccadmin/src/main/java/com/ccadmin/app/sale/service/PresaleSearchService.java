package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.factory.PresaleDetailDtoFactory;
import com.ccadmin.app.sale.repository.*;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
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
    private PresaleChannelRepository presaleChannelRepository;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private CurrencyShared currencyShared;
    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private CatalogSearchShared catalogSearchShared;

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
                this.saleHeadRepository.findByPresaleCod(Presale.PresaleCod).ifPresent(sale -> {
                    Presale.RelatedSaleCod = sale.SaleCod;
                    Presale.RelatedSaleStatus = sale.SaleStatus;
                });
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
        PresaleHeadEntity presaleHead = this.presaleHeadRepository.findById(PresaleCod).get();
        List<PresaleDetEntity> presaleDetailList = this.presaleDetRepository.findByPresaleCod(PresaleCod);
        PresaleChannelEntity presaleChannel = this.presaleChannelRepository.findById(PresaleCod).get();

        if (presaleHead.existClient())
        {
            presaleHead.Client = this.clientShared.findById(presaleHead.ClientCod);
        }

        for (var item : presaleDetailList)
        {
            item.DetailWarehouse = this.presaleDetWarehouseRepository.findByItemNumber(item.PresaleCod,item.ItemNumber);
            item.Product = this.productShared.findById(item.ProductCod);
        }

        return PresaleDetailDtoFactory.fromEntities(presaleHead, presaleDetailList,presaleChannel);
    }

    public ResponseWsDto findDataForm(String PresaleCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        rpt.AddResponseAdditional("CurrencySystem",this.currencyShared.findCurrencySystem());
        rpt.AddResponseAdditional(
                "IndManualDiscount",
                this.catalogSearchShared.findIndicatorSystem(BusinessConfigConstants.ConfigCod.IND_MANUAL_DISCOUNT)
        );
        if(PresaleCod!=null && !PresaleCod.isEmpty()) rpt.AddResponseAdditional("PresaleDetail",this.findById(PresaleCod));

        return rpt;
    }
}
