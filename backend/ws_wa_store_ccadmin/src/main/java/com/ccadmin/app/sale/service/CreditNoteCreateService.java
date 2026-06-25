package com.ccadmin.app.sale.service;


import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.exception.SalePaymentException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.CreditNoteDetailDto;
import com.ccadmin.app.sale.model.dto.CreditNoteRegisterDto;
import com.ccadmin.app.sale.model.dto.CreditNoteReturnPaymentRegisterDto;
import com.ccadmin.app.sale.model.dto.SalePaymentDto;
import com.ccadmin.app.sale.model.entity.*;
import com.ccadmin.app.sale.repository.*;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.shared.WarehouseShared;
import com.ccadmin.app.system.shared.CounterfoilShared;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CreditNoteCreateService extends SessionService {

    @Autowired
    private CreditNoteHeadRepository creditNoteHeadRepository;
    @Autowired
    private CreditNoteDetRepository creditNoteDetRepository;
    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private SaleDetRepository saleDetRepository;
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
    private TrxPaymentShared trxPaymentShared;

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
        for (var product : creditNoteRegister.DetailList) {
            product.CreditNoteCod = creditNoteRegister.Headboard.CreditNoteCod;
            if (product.ItemNumber <= 0) {
                product.ItemNumber = itemNumber;
            }
            product.NumTotalPrice = product.NumUnitPriceSale.multiply(BigDecimal.valueOf(product.NumUnit));
            product.validate().session(getUserCod());
            itemNumber++;
        }

        creditNoteRegister.Headboard.NumTotalPrice = creditNoteRegister.DetailList
                .stream()
                .map( product -> product.NumTotalPrice )
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        creditNoteRegister.Headboard
                .build(saleHead, SaleConstants.PENDING)
                .validate()
                .session(getUserCod());

        this.creditNoteDetRepository.updateStatusAll(creditNoteRegister.Headboard.CreditNoteCod,"I");
        this.creditNoteHeadRepository.save(creditNoteRegister.Headboard);
        this.creditNoteDetRepository.saveAll(creditNoteRegister.DetailList);

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
        CreditNoteDocumentEntity creditNoteDocument = this.creditNoteDocumentRepository.findByCreditNoteCod(creditNoteHead.CreditNoteCod);

        if (creditNoteDocument == null) {
            SaleDocumentEntity saleDocument = this.saleDocumentRepository.findBySaleCod(creditNoteHead.SaleCod);
            String GroupDocument = (saleDocument.DocumentCod.startsWith("B")) ? "B" : "F";
            creditNoteDocument = this.counterfoilShared.generateDocumentCreditNote(getStoreCod(),"07",creditNoteHead.CreditNoteCod,GroupDocument);
            log.info("DOCUMENTO_NOTA_CREDITO -->> {}",creditNoteDocument.DocumentCod);
            this.creditNoteDocumentRepository.save(creditNoteDocument);
        }

        this.creditNoteHeadRepository.save(creditNoteHead);
        this.saleHeadRepository.updateHasCreditNote(creditNoteRegister.Headboard.SaleCod,"S");

        return this.creditNoteSearchService.findById(creditNoteRegister.Headboard.CreditNoteCod);
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
        WarehouseEntity warehouseDefault = this.warehouseShared.findByStore(getStoreCod()).get(0);

        creditNoteHead.IsStockReturned = "S";
        creditNoteHead.addSessionModify(getUserCod());

        List<CreditNoteDetWarehouseEntity> creditNoteDetWarehouseList = creditNoteRegister.DetailList.stream()
                .map( e -> {
                   return new CreditNoteDetWarehouseEntity(
                            e.CreditNoteCod
                           ,e.ItemNumber
                           ,e.ProductCod
                           ,e.Variant
                           ,warehouseDefault.WarehouseCod
                           ,e.NumUnitStockReturned
                           ,e.LotNumber
                           ,e.ExpirationDate
                   ).session(getUserCod());
                }).toList();

        List<KardexEntity> KardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();
        for(var item : creditNoteDetWarehouseList){
            String key = this.stockKey(item.ProductCod, item.Variant, warehouseDefault.StoreCod, item.WarehouseCod);
            KardexEntity kardexLast = lastMovementByStock.computeIfAbsent(
                    key,
                    ignored -> this.kardexShared.findLastMovement(item.ProductCod,item.Variant,item.WarehouseCod,warehouseDefault.StoreCod)
            );
            KardexEntity kardexNoteDetWarehouse = new KardexEntity(kardexLast,item,warehouseDefault.StoreCod);
            kardexNoteDetWarehouse.addSession(getUserCod());
            KardexList.add(kardexNoteDetWarehouse);
            lastMovementByStock.put(key, kardexNoteDetWarehouse);
        }

        this.creditNoteDetRepository.saveAll(creditNoteRegister.DetailList);
        this.creditNoteHeadRepository.save(creditNoteHead);
        this.creditNoteDetWarehouseRepository.saveAll(creditNoteDetWarehouseList);
        this.kardexShared.saveAll(KardexList);

        return this.creditNoteSearchService.findById(creditNoteRegister.Headboard.CreditNoteCod);
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
