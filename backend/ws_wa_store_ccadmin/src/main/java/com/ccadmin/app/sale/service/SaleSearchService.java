package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleDetTaxEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDetRepository;
import com.ccadmin.app.sale.repository.SaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.SaleDetTaxRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.store.shared.CompanyShared;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SaleSearchService extends SessionService {

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private SaleDetRepository saleDetRepository;
    @Autowired
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Autowired
    private SaleDetTaxRepository saleDetTaxRepository;
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
    private CatalogSearchShared catalogSearchShared;
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
        rpt.AddResponseAdditional(
                "IndProformaSales",
                this.catalogSearchShared.findIndicatorSystem(BusinessConfigConstants.ConfigCod.IND_PROFORMA_SALES)
        );
        rpt.AddResponseAdditional(
                "IndAdvancePayment",
                this.catalogSearchShared.findIndicatorSystem(BusinessConfigConstants.ConfigCod.IND_ADVANCE_PAYMENT)
        );
        rpt.AddResponseAdditional(
                "IndMandatoryPicking",
                this.catalogSearchShared.findIndicatorSystem(BusinessConfigConstants.ConfigCod.IND_MANDATORY_PICKING)
        );

        return rpt;
    }

    public SaleDetailDto findById(String SaleCod) {
        SaleDetailDto saleDetail = new SaleDetailDto();

        saleDetail.Headboard = this.saleHeadRepository.findById(SaleCod)
                .orElseThrow(() -> new IllegalArgumentException("No existe la venta " + SaleCod));
        saleDetail.DetailList = this.saleDetRepository.findBySaleCod(SaleCod);
        Map<Integer, List<SaleDetWarehouseEntity>> warehouseDetailByItem =
                this.saleDetWarehouseRepository.findBySaleCod(SaleCod)
                        .stream()
                        .collect(Collectors.groupingBy(item -> item.ItemNumber));
        Map<Integer, List<SaleDetTaxEntity>> taxDetailByItem = this.saleDetTaxRepository.findBySaleCod(SaleCod)
                .stream()
                .collect(Collectors.groupingBy(item -> item.ItemNumber));
        saleDetail.DetailPayment = this.salePaymentRepository.findBySaleCod(SaleCod);
        saleDetail.SaleDocumentList = this.saleDocumentRepository.findBySaleCod(SaleCod);
        this.loadDocumentClients(saleDetail.SaleDocumentList);
        saleDetail.SaleDocument = saleDetail.SaleDocumentList.stream().findFirst().orElse(null);
        saleDetail.CreditNoteDetail = this.creditNoteSearchService.findBySaleCod(SaleCod);

        if(saleDetail.Headboard.existClient())
        {
            saleDetail.Headboard.Client = this.clientShared.findById(saleDetail.Headboard.ClientCod);
        }

        for(var DetailSale : saleDetail.DetailList)
        {
            DetailSale.Product = this.productShared.findById(DetailSale.ProductCod);
            DetailSale.TaxDetailList = taxDetailByItem.getOrDefault(DetailSale.ItemNumber, List.of());
            DetailSale.DetailWarehouse = warehouseDetailByItem.getOrDefault(DetailSale.ItemNumber, List.of());
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

        SaleDetailDto saleDetail = this.findById(saleDocument.SaleCod);
        this.loadDocumentClients(List.of(saleDocument));
        saleDetail.SaleDocument = saleDocument;
        saleDetail.Headboard.ClientCod = saleDocument.ClientCod;
        saleDetail.Headboard.Client = saleDocument.Client;
        return saleDetail;
    }

    public ResponseWsDto findDataPrint(String SaleCod){
        return this.findDataPrint(SaleCod, null);
    }

    public ResponseWsDto findDataPrint(String SaleCod, String DocumentCod){
        ResponseWsDto rpt = new ResponseWsDto();

        SaleDetailDto saleDetail = findById(SaleCod);
        if (DocumentCod != null && !DocumentCod.isBlank()) {
            SaleDocumentEntity selectedDocument = this.saleDocumentRepository.findByDocumentCodAndSaleCod(
                    DocumentCod,
                    SaleCod
            );
            if (selectedDocument == null) {
                throw new IllegalArgumentException(
                        "El documento " + DocumentCod + " no pertenece a la venta " + SaleCod
                );
            }
            this.loadDocumentClients(List.of(selectedDocument));
            saleDetail.SaleDocument = selectedDocument;
        }
        if (saleDetail.SaleDocument != null) {
            saleDetail.Headboard.Client = saleDetail.SaleDocument.Client;
        }

        rpt.AddResponseAdditional("SaleDetail",saleDetail);
        rpt.AddResponseAdditional("PaymentMethodList",this.paymentMethodShared.findAllActive());
        rpt.AddResponseAdditional("CurrencyList",this.currencyShared.findAllActive());
        rpt.AddResponseAdditional("Store",this.storeShared.findStoreInfo(saleDetail.Headboard.StoreCod));

        return rpt;
    }

    private void loadDocumentClients(List<SaleDocumentEntity> documentList) {
        if (documentList == null || documentList.isEmpty()) {
            return;
        }
        List<String> clientCodes = documentList.stream()
                .map(document -> document.ClientCod)
                .filter(Objects::nonNull)
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
        if (clientCodes.isEmpty()) {
            return;
        }
        Map<String, ClientEntity> clientsByCode = this.clientShared.findAllById(clientCodes)
                .stream()
                .collect(Collectors.toMap(client -> client.ClientCod, Function.identity()));
        documentList.forEach(document -> document.Client = clientsByCode.get(document.ClientCod));
    }
}
