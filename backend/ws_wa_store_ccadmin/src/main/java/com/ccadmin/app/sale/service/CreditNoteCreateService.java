package com.ccadmin.app.sale.service;


import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.CreditNoteStockShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.exception.SalePaymentException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.dto.CreditNoteRegisterDto;
import com.ccadmin.app.sale.model.dto.CreditNoteReturnPaymentRegisterDto;
import com.ccadmin.app.sale.model.dto.SalePaymentDto;
import com.ccadmin.app.sale.model.entity.*;
import com.ccadmin.app.sale.repository.*;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.shared.WarehouseShared;
import com.ccadmin.app.system.shared.CounterfoilShared;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CreditNoteCreateService extends SessionService {

    @Autowired
    private CreditNoteHeadRepository creditNoteHeadRepository;
    @Autowired
    private CreditNoteDetRepository creditNoteDetRepository;
    @Autowired
    private CreditNoteDetTaxRepository creditNoteDetTaxRepository;
    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private SaleDetRepository saleDetRepository;
    @Autowired
    private SaleDetTaxRepository saleDetTaxRepository;
    @Autowired
    private SaleDocumentRepository saleDocumentRepository;
    @Autowired
    private CreditNoteDetWarehouseRepository creditNoteDetWarehouseRepository;
    @Autowired
    private CreditNoteDocumentRepository creditNoteDocumentRepository;
    @Autowired
    private CreditNoteSearchService creditNoteSearchService;
    @Autowired
    private SalePaymentSearchService salePaymentSearchService;
    @Autowired
    private SalePaymentCreateService salePaymentCreateService;
    @Autowired
    private CounterfoilShared counterfoilShared;
    @Autowired
    private WarehouseShared warehouseShared;
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private CreditNoteStockShared creditNoteStockShared;
    @Autowired
    private TrxPaymentShared trxPaymentShared;
    @Autowired
    private GenericQueuedService genericQueuedService;
    @Autowired
    private CreditNoteSunatEmissionService creditNoteSunatEmissionService;

    public String createCode(){
        String PresaleCod = creditNoteHeadRepository.getCreditNoteCod(getStoreCod());
        return PresaleCod;
    }

    @Transactional
    public CreditNoteDetailDto save(CreditNoteRegisterDto creditNoteRegister) throws SaleException {

        log.info("INI_CREACION_NOTA_CREDITO -->> {}",creditNoteRegister.Headboard.CreditNoteCod);

        this.validateCreditNoteRegisterDto(creditNoteRegister);

        SaleHeadEntity saleHead = this.saleHeadRepository.findById(creditNoteRegister.Headboard.SaleCod).get();

        int itemNumber = 1;
        List<SaleDetEntity> saleDetList = this.saleDetRepository.findBySaleCod(creditNoteRegister.Headboard.SaleCod);
        List<SaleDetTaxEntity> saleDetTaxList = this.saleDetTaxRepository.findBySaleCod(creditNoteRegister.Headboard.SaleCod);
        Map<Integer, List<SaleDetTaxEntity>> saleTaxByItem = saleDetTaxList.stream()
                .collect(Collectors.groupingBy(item -> item.ItemNumber));
        List<CreditNoteDetTaxEntity> creditNoteDetTaxList = new ArrayList<>();
        for (var product : creditNoteRegister.DetailList) {
            product.CreditNoteCod = creditNoteRegister.Headboard.CreditNoteCod;
            if (product.ItemNumber <= 0) {
                product.ItemNumber = itemNumber;
            }
            SaleDetEntity originDetail = saleDetList.stream()
                    .filter(e -> e.ItemNumber == product.ItemNumber
                            && e.ProductCod.equals(product.ProductCod)
                            && e.Variant.equals(product.Variant))
                    .findFirst()
                    .orElseThrow(() -> new SaleException(" producto no existe en la compra de origen  "+ product.ProductCod));
            product.ProductUnitName = originDetail.ProductUnitName;
            product.ProductUnitFactor = originDetail.ProductUnitFactor;
            product.NumTotalPrice = product.NumUnitPriceSale.multiply(BigDecimal.valueOf(product.NumUnit));
            List<CreditNoteDetTaxEntity> productTaxList = this.createCreditNoteDetTaxList(
                    creditNoteRegister.Headboard.CreditNoteCod,
                    product,
                    originDetail,
                    saleTaxByItem.getOrDefault(originDetail.ItemNumber, List.of())
            );
            product.NumTotalTax = productTaxList.stream()
                    .map(tax -> amount(tax.TaxAmount))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            if (productTaxList.isEmpty()) {
                product.NumTotalTax = prorateAmount(originDetail.NumTotalTax, product.NumUnit, originDetail.NumUnit);
            }
            product.NumPriceSubTotal = amount(product.NumTotalPrice).subtract(product.NumTotalTax).setScale(2, RoundingMode.HALF_UP);
            product.IsAppliedTax = product.NumTotalTax.compareTo(BigDecimal.ZERO) > 0 ? "S" : "N";
            creditNoteDetTaxList.addAll(productTaxList);
            product.validate().session(getUserCod());
            itemNumber++;
        }

        creditNoteRegister.Headboard.NumTotalPrice = creditNoteRegister.DetailList
                .stream()
                .map( product -> product.NumTotalPrice )
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal numTotalPriceNoTax = creditNoteRegister.DetailList
                .stream()
                .map(product -> amount(product.NumPriceSubTotal))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal numTotalTax = creditNoteRegister.DetailList
                .stream()
                .map(product -> amount(product.NumTotalTax))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        creditNoteRegister.Headboard
                .build(saleHead, SaleConstants.PENDING)
                .tax(numTotalPriceNoTax, numTotalTax)
                .validate()
                .session(getUserCod());

        this.creditNoteDetTaxRepository.updateStatusAll(creditNoteRegister.Headboard.CreditNoteCod,"I");
        this.creditNoteDetRepository.updateStatusAll(creditNoteRegister.Headboard.CreditNoteCod,"I");
        this.creditNoteHeadRepository.save(creditNoteRegister.Headboard);
        this.creditNoteDetRepository.saveAll(creditNoteRegister.DetailList);
        this.creditNoteDetTaxRepository.saveAll(creditNoteDetTaxList);

        log.info("FIN_CREACION_NOTA_CREDITO -->> {}",creditNoteRegister.Headboard.CreditNoteCod);

        return this.creditNoteSearchService.findById(creditNoteRegister.Headboard.CreditNoteCod);
    }

    @Transactional
    public CreditNoteDetailDto confirm(CreditNoteRegisterDto creditNoteRegister) throws SaleException, SalePaymentException {

        CreditNoteHeadEntity creditNoteHead = this.creditNoteHeadRepository.findById(creditNoteRegister.Headboard.CreditNoteCod).get();

        if(creditNoteHead.CreditNoteStatus.equals(SaleConstants.CONFIRMED)){
            throw new SaleException("Nota de crédito ya fue confirmada");
        }

        creditNoteHead.CreditNoteStatus = SaleConstants.CONFIRMED;
        List<CreditNoteDetEntity> detailList = this.creditNoteDetRepository.findByCreditNoteCod(creditNoteHead.CreditNoteCod);
        WarehouseEntity warehouseDefault = this.warehouseShared.findByStore(creditNoteHead.StoreCod).get(0);
        CreditNoteDocumentEntity creditNoteDocument = this.creditNoteDocumentRepository.findByCreditNoteCod(creditNoteHead.CreditNoteCod);

        if (creditNoteDocument == null) {
            SaleDocumentEntity saleDocument = this.saleDocumentRepository.findBySaleCod(creditNoteHead.SaleCod);
            String GroupDocument = (saleDocument.DocumentCod.startsWith("B")) ? "B" : "F";
            creditNoteDocument = this.counterfoilShared.generateDocumentCreditNote(getStoreCod(),"07",creditNoteHead.CreditNoteCod,GroupDocument);
            log.info("DOCUMENTO_NOTA_CREDITO -->> {}",creditNoteDocument.DocumentCod);
            this.creditNoteDocumentRepository.save(creditNoteDocument);
        }

        this.creditNoteHeadRepository.save(creditNoteHead);
        this.saleHeadRepository.updateHasCreditNote(creditNoteHead.SaleCod,"S");
        this.creditNoteStockShared.addUnavailableStock(creditNoteHead, detailList, warehouseDefault, getUserCod());
        this.kardexShared.saveAllLedgerOnly(
                this.createCreditNoteKardexList(detailList, warehouseDefault, "S", false)
        );

        CreditNoteDetailDto creditNoteDetail = this.creditNoteSearchService.findById(creditNoteRegister.Headboard.CreditNoteCod);
        this.emitSunatAfterCommit(creditNoteHead.CreditNoteCod);

        return creditNoteDetail;
    }

    private void emitSunatAfterCommit(String creditNoteCod) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    queueSunatEmission(creditNoteCod);
                }
            });
            return;
        }
        queueSunatEmission(creditNoteCod);
    }

    private void queueSunatEmission(String creditNoteCod) {
        this.genericQueuedService.addQueued(new CreditNoteSunatEmissionTaskService(this.creditNoteSunatEmissionService, creditNoteCod));
    }

    @Transactional
    public SalePaymentEntity addReturnPayment(CreditNoteReturnPaymentRegisterDto payment) throws SaleException, SalePaymentException {

        CreditNoteHeadEntity creditNoteHead = this.creditNoteHeadRepository.findById(payment.CreditNoteCod).get();
        if(creditNoteHead.CreditNoteStatus.equals(SaleConstants.CONFIRMED)){
            throw new SaleException("Nota de credito ya fue confirmada");
        }

        List<SalePaymentDto> salePaymentList = salePaymentSearchService.findBySaleCod(creditNoteHead.SaleCod);
        BigDecimal totalReturned = this.totalReturned(salePaymentList);

        if(totalReturned.doubleValue() >= creditNoteHead.NumTotalPrice.doubleValue()){
            throw new SalePaymentException("Nota de credito ya completo la devolucion");
        }

        TrxPaymentEntity trxPayment = this.trxPaymentShared.findById(payment.TrxPaymentId);
        if(!"E".equals(trxPayment.TypeMovement) || trxPayment.ReversalOfTrxPaymentId == null){
            throw new SalePaymentException("La transaccion de pago no corresponde a una reversa");
        }
        if(this.existsSalePayment(salePaymentList, trxPayment.TrxPaymentId)){
            throw new SalePaymentException("Reversa de pago ya fue registrada");
        }

        SalePaymentDto originalSalePayment = this.findOriginalPayment(salePaymentList, trxPayment.ReversalOfTrxPaymentId);

        int PaymentNumber = salePaymentList.size() + 1;
        SalePaymentEntity salePayment = SalePaymentEntity.buildReversal(originalSalePayment.SalePayment,trxPayment,getUserCod(),PaymentNumber);
        salePayment = salePaymentCreateService.save(salePayment);

        BigDecimal newTotalReturned = totalReturned.add(salePayment.NumAmountPaid.negate());
        if(newTotalReturned.doubleValue() >= creditNoteHead.NumTotalPrice.doubleValue()){
            creditNoteHead.IsPaid = "S";
            this.creditNoteHeadRepository.save(creditNoteHead);
            // CreditNoteRegisterDto creditNoteRegister = new CreditNoteRegisterDto();
            // creditNoteRegister.Headboard = creditNoteHead;
            // this.confirm(creditNoteRegister);
        }

        return salePayment;
    }

    @Transactional
    public CreditNoteDetailDto saveReturnStock(CreditNoteRegisterDto creditNoteRegister) throws SaleException {

        CreditNoteHeadEntity creditNoteHead = this.creditNoteHeadRepository.findById(creditNoteRegister.Headboard.CreditNoteCod).get();
        WarehouseEntity warehouseDefault = this.warehouseShared.findByStore(creditNoteHead.StoreCod).get(0);

        if(!creditNoteHead.CreditNoteStatus.equals(SaleConstants.CONFIRMED)){
            throw new SaleException("Nota de credito debe estar confirmada para retornar stock");
        }
        if("S".equals(creditNoteHead.IsStockReturned)){
            throw new SaleException("Stock de nota de credito ya fue procesado");
        }

        creditNoteHead.IsStockReturned = "S";
        creditNoteHead.addSessionModify(getUserCod());

        List<CreditNoteDetEntity> creditNoteDetList = this.creditNoteDetRepository.findByCreditNoteCod(creditNoteHead.CreditNoteCod);
        this.applyReturnedUnits(creditNoteDetList, creditNoteRegister.DetailList);

        List<CreditNoteDetWarehouseEntity> creditNoteDetWarehouseList = creditNoteDetList.stream()
                .filter(e -> e.NumUnitStockReturned != null && e.NumUnitStockReturned > 0)
                .map(e -> new CreditNoteDetWarehouseEntity(
                        e.CreditNoteCod,
                        e.ItemNumber,
                        e.ProductCod,
                        e.Variant,
                        warehouseDefault.WarehouseCod,
                        e.NumUnitStockReturned,
                        e.ProductUnitName,
                        e.ProductUnitFactor,
                        e.LotNumber,
                        e.ExpirationDate
                ).session(getUserCod()))
                .toList();

        List<KardexEntity> kardexRejectedList = this.createCreditNoteKardexList(creditNoteDetList, warehouseDefault, "R", true);

        this.creditNoteStockShared.resolveUnavailableStock(creditNoteHead, creditNoteDetList, warehouseDefault, getUserCod());
        this.creditNoteDetRepository.saveAll(creditNoteDetList);
        this.creditNoteHeadRepository.save(creditNoteHead);
        this.creditNoteDetWarehouseRepository.saveAll(creditNoteDetWarehouseList);
        this.kardexShared.saveAllLedgerOnly(kardexRejectedList);

        return this.creditNoteSearchService.findById(creditNoteRegister.Headboard.CreditNoteCod);
    }

    private List<CreditNoteDetTaxEntity> createCreditNoteDetTaxList(
            String creditNoteCod,
            CreditNoteDetEntity creditNoteDet,
            SaleDetEntity originDetail,
            List<SaleDetTaxEntity> originTaxList
    ) {
        if (originTaxList == null || originTaxList.isEmpty()) {
            return List.of();
        }
        List<CreditNoteDetTaxEntity> taxList = originTaxList.stream()
                .map(originTax -> buildCreditNoteTaxLine(creditNoteCod, creditNoteDet, originDetail, originTax))
                .sorted(Comparator
                        .comparingInt((CreditNoteDetTaxEntity tax) -> tax.CalculationOrder)
                        .thenComparing(tax -> tax.TaxCod))
                .toList();
        for (int i = 0; i < taxList.size(); i++) {
            taxList.get(i).TaxLineNumber = i + 1;
            taxList.get(i).session(getUserCod()).validate();
        }
        return taxList;
    }

    private CreditNoteDetTaxEntity buildCreditNoteTaxLine(
            String creditNoteCod,
            CreditNoteDetEntity creditNoteDet,
            SaleDetEntity originDetail,
            SaleDetTaxEntity originTax
    ) {
        CreditNoteDetTaxEntity tax = new CreditNoteDetTaxEntity();
        tax.CreditNoteCod = creditNoteCod;
        tax.ItemNumber = creditNoteDet.ItemNumber;
        tax.TaxLineNumber = originTax.TaxLineNumber;
        tax.TaxCod = originTax.TaxCod;
        tax.SunatTaxCod = originTax.SunatTaxCod;
        tax.TaxName = originTax.TaxName;
        tax.TaxAffectationCod = originTax.TaxAffectationCod;
        tax.TaxAffectationName = originTax.TaxAffectationName;
        tax.TaxCalculationType = originTax.TaxCalculationType;
        tax.IsInformative = originTax.IsInformative;
        tax.TaxRateValue = originTax.TaxRateValue;
        tax.FixedUnitAmount = originTax.FixedUnitAmount;
        tax.TaxBaseAmount = prorateAmount(originTax.TaxBaseAmount, creditNoteDet.NumUnit, originDetail.NumUnit);
        tax.TaxQuantity = prorateQuantity(originTax.TaxQuantity, creditNoteDet.NumUnit, originDetail.NumUnit);
        tax.TaxAmount = prorateAmount(originTax.TaxAmount, creditNoteDet.NumUnit, originDetail.NumUnit);
        tax.CalculationOrder = originTax.CalculationOrder;
        return tax;
    }

    private BigDecimal prorateAmount(BigDecimal value, Integer units, Integer originUnits) {
        return amount(value)
                .multiply(ratio(units, originUnits))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal prorateQuantity(BigDecimal value, Integer units, Integer originUnits) {
        return valueOrZero(value)
                .multiply(ratio(units, originUnits))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(Integer units, Integer originUnits) {
        if (units == null || originUnits == null || originUnits <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(units).divide(BigDecimal.valueOf(originUnits), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(BigDecimal value) {
        return valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateCreditNoteRegisterDto(CreditNoteRegisterDto creditNoteRegister) throws SaleException {
        if(creditNoteRegister.Headboard == null){
            throw new SaleException("No existe cabecera en la nota de crédito");
        }
        if(creditNoteRegister.DetailList == null || creditNoteRegister.DetailList.isEmpty()){
            throw new SaleException("El detalle de la nota de crédito esta vació");
        }
        if(creditNoteRegister.Headboard.CreditNoteStatus.equals(SaleConstants.CONFIRMED)){
            throw new SaleException("Nota de crédito ya fue confirmada no se puede editar");
        }
        List<SaleDetEntity> saleDetList = this.saleDetRepository.findBySaleCod(creditNoteRegister.Headboard.SaleCod);
        for(var product : creditNoteRegister.DetailList){
            if(saleDetList.stream().noneMatch(e -> e.ItemNumber == product.ItemNumber
                    && e.ProductCod.equals(product.ProductCod)
                    && e.Variant.equals(product.Variant))){
                throw new SaleException(" producto no existe en la compra de origen  "+ product.ProductCod);
            }
        }

        if(creditNoteRegister.Document == null || creditNoteRegister.Document.DocumentCod.isEmpty()){
            CreditNoteHeadEntity creditNoteHead = this.creditNoteHeadRepository.findBySaleCod(creditNoteRegister.Headboard.SaleCod);
            if(creditNoteHead != null && !creditNoteHead.CreditNoteCod.equals(creditNoteRegister.Headboard.CreditNoteCod)){
                throw new SaleException("Venta ya tiene asociada una nota de crédito");
            }
        }
    }

    private SalePaymentDto findOriginalPayment(List<SalePaymentDto> salePaymentList, Long trxPaymentId) throws SalePaymentException {
        return salePaymentList.stream()
                .filter(payment -> payment.TrxPayment.TrxPaymentId.equals(trxPaymentId))
                .filter(payment -> !"E".equals(payment.TrxPayment.TypeMovement))
                .findFirst()
                .orElseThrow(() -> new SalePaymentException("Pago original no existe en la venta de origen"));
    }

    private BigDecimal totalReturned(List<SalePaymentDto> salePaymentList) {
        return salePaymentList.stream()
                .filter(payment -> "E".equals(payment.TrxPayment.TypeMovement))
                .map(payment -> payment.SalePayment.NumAmountPaid.negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean existsSalePayment(List<SalePaymentDto> salePaymentList, Long trxPaymentId) {
        return salePaymentList.stream()
                .anyMatch(payment -> payment.TrxPayment.TrxPaymentId.equals(trxPaymentId));
    }

    private String stockKey(String productCod, String variant, String storeCod, String warehouseCod) {
        return productCod + "|" + variant + "|" + storeCod + "|" + warehouseCod;
    }

    private List<KardexEntity> createCreditNoteKardexList(
            List<CreditNoteDetEntity> detailList,
            WarehouseEntity warehouseDefault,
            String typeOperation,
            boolean onlyRejected
    ) {
        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (var detail : detailList) {
            int numStockMoved = detail.NumUnit;
            if (onlyRejected) {
                int returned = detail.NumUnitStockReturned == null ? 0 : detail.NumUnitStockReturned;
                numStockMoved = detail.NumUnit - returned;
            }
            if (numStockMoved <= 0) {
                continue;
            }

            String key = this.stockKey(detail.ProductCod, detail.Variant, warehouseDefault.StoreCod, warehouseDefault.WarehouseCod);
            KardexEntity kardexLast = lastMovementByStock.computeIfAbsent(
                    key,
                    ignored -> this.kardexShared.findLastMovement(detail.ProductCod, detail.Variant, warehouseDefault.WarehouseCod, warehouseDefault.StoreCod)
            );
            KardexEntity kardex = new KardexEntity(
                    kardexLast,
                    detail,
                    warehouseDefault.StoreCod,
                    warehouseDefault.WarehouseCod,
                    numStockMoved,
                    typeOperation
            ).session(getUserCod());
            kardexList.add(kardex);
            lastMovementByStock.put(key, kardex);
        }

        return kardexList;
    }

    private void applyReturnedUnits(List<CreditNoteDetEntity> creditNoteDetList, List<CreditNoteDetEntity> requestDetailList) throws SaleException {
        if(requestDetailList == null || requestDetailList.isEmpty()){
            throw new SaleException("Detalle de retorno de stock esta vacio");
        }

        for(var detail : creditNoteDetList){
            CreditNoteDetEntity detailRequest = requestDetailList.stream()
                    .filter(e -> e.ItemNumber == detail.ItemNumber)
                    .findFirst()
                    .orElseThrow(() -> new SaleException("Detalle de nota de credito incompleto para retorno de stock"));

            int returned = detailRequest.NumUnitStockReturned == null ? 0 : detailRequest.NumUnitStockReturned;
            if(returned < 0 || returned > detail.NumUnit){
                throw new SaleException("Cantidad de retorno invalida para el producto " + detail.ProductCod);
            }
            detail.NumUnitStockReturned = returned;
            detail.addSessionModify(getUserCod());
        }
    }

    private void saveReversalPayment(CreditNoteHeadEntity creditNoteHead) throws SalePaymentException{

        List<SalePaymentDto> salePaymentList = salePaymentSearchService.findBySaleCod(creditNoteHead.SaleCod);
        int PaymentNumber = salePaymentList.size();

        log.info("MONTO_PENDIENTE_POR_DEVOLER -->> {}",creditNoteHead.NumTotalPrice);
        log.info("NUMERO_PAGOS_ORIGEN -->> {}",PaymentNumber);

        if(creditNoteHead.TypeCreditNote.equals("T")){
            for(var salePaymentDto : salePaymentList){
                PaymentNumber++;
                TrxPaymentEntity trxPayment = TrxPaymentEntity.buildReversal(salePaymentDto.TrxPayment,getUserCod());
                trxPayment = this.trxPaymentShared.save(trxPayment);
                SalePaymentEntity salePayment = SalePaymentEntity.buildReversal(salePaymentDto.SalePayment,trxPayment,getUserCod(),PaymentNumber);
                salePayment = salePaymentCreateService.save(salePayment);
            }
        }else if(creditNoteHead.TypeCreditNote.equals("P")){
            BigDecimal NumTotalReturn = BigDecimal.ZERO;
            BigDecimal NumTotalPending = creditNoteHead.NumTotalPrice;
            TrxPaymentEntity trxPayment = null;

            for(var salePaymentDto : salePaymentList){

                BigDecimal NumAmountPaidOrigin = salePaymentDto.SalePayment.NumAmountPaid
                                            .subtract(salePaymentDto.SalePayment.NumAmountReturned);

                if(NumTotalPending.doubleValue() == 0){
                    break;
                }

                if(NumTotalPending.subtract(NumAmountPaidOrigin).doubleValue() > 0 ){                   
                    trxPayment = TrxPaymentEntity.buildReversal(salePaymentDto.TrxPayment,getUserCod());
                }else if(NumTotalPending.subtract(NumAmountPaidOrigin).doubleValue() == 0 ){                   
                    trxPayment = TrxPaymentEntity.buildReversal(salePaymentDto.TrxPayment,getUserCod());
                }else if(NumTotalPending.subtract(NumAmountPaidOrigin).doubleValue() < 0 ){  
                    BigDecimal AmountPaidJust = NumTotalPending;
                    trxPayment = TrxPaymentEntity.buildPartialReversal(salePaymentDto.TrxPayment,AmountPaidJust,getUserCod());
                }

                PaymentNumber++;
                trxPayment = this.trxPaymentShared.save(trxPayment);
                SalePaymentEntity salePayment = SalePaymentEntity.buildReversal(salePaymentDto.SalePayment,trxPayment,getUserCod(),PaymentNumber);
                salePayment = salePaymentCreateService.save(salePayment);

                NumTotalReturn = NumTotalReturn.add(trxPayment.AmountPaid.negate());
                NumTotalPending = NumTotalPending.subtract(trxPayment.AmountPaid.negate());

                log.info("MONTO_DEVUELTO -->> {}",trxPayment.AmountPaid);
                log.info("MONTO_TOTAL_DEVUELTO -->> {}",NumTotalReturn);
                log.info("MONTO_PENDIENTE -->> {}",NumTotalPending);
                log.info("REVERSION_PAGO_OPERACION -->> {}",trxPayment.toString());
                log.info("REVERSION_PAGO_VENTA -->> {}",salePayment.toString());
            }

        }
    }
}
