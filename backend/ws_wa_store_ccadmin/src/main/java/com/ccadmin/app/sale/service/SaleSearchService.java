package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.shared.CompanyShared;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleSearchService extends SessionService {

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private SaleDetRepository saleDetRepository;
    @Autowired
    private SalePaymentRepository salePaymentRepository;
    @Autowired
    private SaleDocumentRepository saleDocumentRepository;
    @Autowired
    private CurrencyShared currencyShared;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private PaymentMethodShared paymentMethodShared;
    @Autowired
    private ClientShared clientShared;
    @Autowired
    private TrxPaymentShared trxPaymentShared;
    @Autowired
    private CompanyShared companyShared;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private CreditNoteSearchService creditNoteSearchService;
    private SearchService searchService;

    private SearchTService<SaleHeadEntity> searchTService;

    public ResponseWsDto findDataForm(String SaleCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if(SaleCod != null && !SaleCod.trim().equals(""))
        {
            rpt.AddResponseAdditional("SaleDetail",findById(SaleCod));
        }
        rpt.AddResponseAdditional("PaymentMethodList",this.paymentMethodShared.findAllActive());
        rpt.AddResponseAdditional("CurrencyList",this.currencyShared.findAllActive());

        return rpt;
    }

    public SaleDetailDto findById(String SaleCod) {
        SaleDetailDto saleDetail = new SaleDetailDto();

        saleDetail.Headboard = this.saleHeadRepository.findById(SaleCod).get();
        saleDetail.DetailList = this.saleDetRepository.findBySaleCod(SaleCod);
        saleDetail.DetailPayment = this.salePaymentRepository.findBySaleCod(SaleCod);
        saleDetail.SaleDocument = this.saleDocumentRepository.findBySaleCod(SaleCod);
        saleDetail.CreditNoteDetail = this.creditNoteSearchService.findBySaleCod(SaleCod);

        if(saleDetail.Headboard.existClient())
        {
            saleDetail.Headboard.Client = this.clientShared.findById(saleDetail.Headboard.ClientCod);
        }

        for(var DetailSale : saleDetail.DetailList)
        {
            DetailSale.Product = this.productShared.findById(DetailSale.ProductCod);
        }

        for(var Payment : saleDetail.DetailPayment)
        {
            Payment.TrxPayment = this.trxPaymentShared.findById(Payment.TrxPaymentId);
        }

        return saleDetail;
    }

    public ResponsePageSearchT<SaleHeadEntity> findAll(String Query, int Page, String StoreCod,String a){
        this.searchService = new SearchService(this.saleHeadRepository);
        SearchDto search = new SearchDto(Query,Page,StoreCod);
        ResponsePageSearchT<SaleHeadEntity> responsePage = this.searchTService.findAllStore(search,10);

        if( responsePage.resultSearch != null )
        {
            List<ClientEntity> clientList = this.clientShared.findAllById(responsePage.resultSearch
                    .stream()
                    .filter(SaleHeadEntity::existClient)
                    .map( e -> e.ClientCod)
                    .toList()
            );

            responsePage.resultSearch.forEach( sale -> {
                if(sale.existClient()){
                    sale.Client = clientList.stream()
                            .filter( client -> client.ClientCod.equals(sale.ClientCod))
                            .findFirst()
                            .orElse(null);
                }
            } );
        }
        return responsePage;
    }

    public ResponsePageSearch findAll(String Query, int Page, String StoreCod){
        this.searchService = new SearchService(this.saleHeadRepository);
        SearchDto search = new SearchDto(Query,Page,StoreCod);
        ResponsePageSearch responsePage = this.searchService.findAllStore(search,10);

        if( responsePage.resultSearch != null )
        {
            for (SaleHeadEntity Sale : (List<SaleHeadEntity>)responsePage.resultSearch)
            {
                if(Sale.ClientCod != null && !Sale.ClientCod.isEmpty()) Sale.Client = this.clientShared.findById(Sale.ClientCod);
            }
        }
        return responsePage;
    }

    public SaleDetailDto findByDocumentCod(String DocumentCod) {

        SaleDocumentEntity saleDocument = this.saleDocumentRepository.findByDocumentCod(DocumentCod);

        if(saleDocument==null) return null;

        return this.findById(saleDocument.SaleCod);
    }

    public ResponseWsDto findDataPrint(String SaleCod){
        ResponseWsDto rpt = new ResponseWsDto();

        SaleDetailDto saleDetail = findById(SaleCod);

        rpt.AddResponseAdditional("SaleDetail",saleDetail);
        rpt.AddResponseAdditional("PaymentMethodList",this.paymentMethodShared.findAllActive());
        rpt.AddResponseAdditional("CurrencyList",this.currencyShared.findAllActive());
        rpt.AddResponseAdditional("Store",this.storeShared.findStoreInfo(saleDetail.Headboard.StoreCod));

        return rpt;
    }
}
