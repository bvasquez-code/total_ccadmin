package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
import com.ccadmin.app.system.shared.PaymentMethodShared;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrxPaymentSearchService extends SessionService {

    @Autowired
    private TrxPaymentRepository trxPaymentRepository;

    @Autowired
    private PaymentMethodShared paymentMethodShared;

    @Autowired
    private CurrencyShared currencyShared;

    private SearchTService<TrxPaymentEntity> searchService;

    @Autowired
    private void init() {
        this.searchService = new SearchTService<>(this.trxPaymentRepository);
    }

    public TrxPaymentEntity findById(Long trxPaymentId) {
        return this.trxPaymentRepository.findById(trxPaymentId).orElse(null);
    }

    public List<TrxPaymentEntity> findAllById(List<Long> trxPaymentId) {
        return this.trxPaymentRepository.findAllById(trxPaymentId);
    }

    public TrxPaymentEntity findByTransactionId(String transactionId) {
        return this.trxPaymentRepository.findByTransactionId(transactionId);
    }

    public ResponsePageSearchT<TrxPaymentEntity> findAll(String query, int page) {
        return searchService.findAll(new SearchDto(query, page), 10);
    }

    public ResponseWsDto findDataForm() {
        ResponseWsDto rpt = new ResponseWsDto();

        List<PaymentMethodEntity> paymentMethodList = this.paymentMethodShared.findAllActive();
        List<CurrencyEntity> currencyList = this.currencyShared.findAllActive();

        rpt.AddResponseAdditional("paymentMethodList", paymentMethodList);
        rpt.AddResponseAdditional("currencyList", currencyList);

        return rpt;
    }

    public ResponseWsDto findDataFormView(Long trxPaymentId) {
        ResponseWsDto rpt = new ResponseWsDto();

        TrxPaymentEntity trxPayment = this.trxPaymentRepository.findById(trxPaymentId).orElse(null);
        List<PaymentMethodEntity> paymentMethodList = this.paymentMethodShared.findAllActive();
        List<CurrencyEntity> currencyList = this.currencyShared.findAllActive();

        rpt.AddResponseAdditional("trxPayment", trxPayment);
        rpt.AddResponseAdditional("paymentMethodList", paymentMethodList);
        rpt.AddResponseAdditional("currencyList", currencyList);

        return rpt;
    }
}
