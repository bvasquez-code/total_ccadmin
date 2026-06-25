package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.sale.model.dto.CreditNoteDetDto;
import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.dto.CreditNoteHeadDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.repository.*;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CreditNoteSearchService {

    @Autowired
    private CreditNoteHeadRepository creditNoteHeadRepository;
    @Autowired
    private CreditNoteDetRepository creditNoteDetRepository;
    @Autowired
    private CreditNoteDocumentRepository creditNoteDocumentRepository;
    @Autowired
    private SalePaymentRepository salePaymentRepository;
    @Autowired
    private SaleDocumentRepository saleDocumentRepository;
    @Autowired
    private ClientShared clientShared;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private CurrencyShared currencyShared;
    @Autowired
    private StoreShared storeShared;
    @Autowired
    private PaymentMethodShared paymentMethodShared;

    private SearchTService<CreditNoteHeadEntity> searchTService;

    public ResponsePageSearchT<CreditNoteHeadDto> findAll(String Query, int Page, String StoreCod){

        this.searchTService = new SearchTService<>(this.creditNoteHeadRepository);
        SearchDto search = new SearchDto(Query,Page,StoreCod);
        ResponsePageSearchT<CreditNoteHeadEntity> responsePage = this.searchTService.findAllStore(search);

        List<ClientEntity> clientList = this.clientShared.findAllById(
                responsePage.resultSearch.stream()
                        .filter( CreditNoteHead -> CreditNoteHead.ClientCod != null && !CreditNoteHead.ClientCod.isEmpty())
                        .map( CreditNoteHead -> CreditNoteHead.ClientCod )
                        .toList()
        );

        List<CreditNoteHeadDto> creditNoteHeadList  = responsePage.resultSearch.stream().map( CreditNoteHead -> {
            ClientEntity client = null;
            if( CreditNoteHead.ClientCod != null && !CreditNoteHead.ClientCod.isEmpty() ){
                client = clientList.stream()
                        .filter( clientMap ->  clientMap.ClientCod.equals(CreditNoteHead.ClientCod))
                        .findFirst()
                        .orElse(null);
            }

            CreditNoteDocumentEntity creditNoteDocument = this.creditNoteDocumentRepository.findByCreditNoteCod( CreditNoteHead.CreditNoteCod );

            return new CreditNoteHeadDto(CreditNoteHead,client,creditNoteDocument);
        }).toList();

        return new ResponsePageSearchT<CreditNoteHeadDto>().clone(creditNoteHeadList,responsePage);
    }

    public CreditNoteDetailDto findById(String CreditNoteCod){
        CreditNoteDetailDto creditNoteDetail = new CreditNoteDetailDto();

        creditNoteDetail.Headboard = this.creditNoteHeadRepository.findById(CreditNoteCod).orElse(null);
        creditNoteDetail.DetailList = this.creditNoteDetRepository.findByCreditNoteCod(CreditNoteCod)
                .stream()
                .map( creditNoteDet -> {
                    CreditNoteDetDto creditNoteDetDto = new CreditNoteDetDto();
                    creditNoteDetDto.CreditNoteDet = creditNoteDet;
                    creditNoteDetDto.Product = this.productShared.findById(creditNoteDet.ProductCod);
                    return creditNoteDetDto;
                } )
                .toList();
        if(creditNoteDetail.Headboard.ClientCod != null && !creditNoteDetail.Headboard.ClientCod.isEmpty()){
            creditNoteDetail.Client = this.clientShared.findById(creditNoteDetail.Headboard.ClientCod);
        }
        creditNoteDetail.DetailPayment = this.salePaymentRepository.findByCreditNoteCod(creditNoteDetail.Headboard.CreditNoteCod);
        creditNoteDetail.Document = this.creditNoteDocumentRepository.findByCreditNoteCod(creditNoteDetail.Headboard.CreditNoteCod);
        creditNoteDetail.DocumentReference = this.saleDocumentRepository.findBySaleCod(creditNoteDetail.Headboard.SaleCod);
        return creditNoteDetail;
    }

    public CreditNoteDetailDto findBySaleCod(String SaleCod){

        CreditNoteHeadEntity creditNoteHead = this.creditNoteHeadRepository.findBySaleCod(SaleCod);

        if(creditNoteHead == null) return null;

        return this.findById(creditNoteHead.CreditNoteCod);

    }

    public CreditNoteDetailDto findByDocumentCod(String DocumentCod) {

        CreditNoteDocumentEntity creditNoteDocument = creditNoteDocumentRepository.findByDocumentCod(DocumentCod);

        return this.findById(creditNoteDocument.CreditNoteCod);
    }

    public ResponseWsDto findDataPrint(String CreditNoteCod){
        ResponseWsDto rpt = new ResponseWsDto();
        CreditNoteDetailDto creditNoteDetail = this.findById(CreditNoteCod);
        rpt.AddResponseAdditional("CreditNoteDetail",creditNoteDetail);
        rpt.AddResponseAdditional("PaymentMethodList",this.paymentMethodShared.findAllActive());
        rpt.AddResponseAdditional("CurrencyList",this.currencyShared.findAllActive());
        rpt.AddResponseAdditional("Store",this.storeShared.findStoreInfo(creditNoteDetail.Headboard.StoreCod));
        return rpt;
    }
}
