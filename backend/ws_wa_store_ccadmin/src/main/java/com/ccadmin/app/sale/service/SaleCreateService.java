package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.service.ProductRankingService;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.*;
import com.ccadmin.app.sale.repository.*;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SaleCreateService extends SessionService {

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private SaleDetRepository saleDetRepository;
    @Autowired
    private SaleDetWarehouseRepository saleDetWarehouseRepository;
    @Autowired
    private SaleAppliedTaxRepository saleAppliedTaxRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private TaxRepository taxRepository;
    @Autowired
    private SaleDocumentRepository saleDocumentRepository;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private GenericQueuedService genericQueuedService;
    @Autowired
    private ProductRankingService productRankingService;
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private CounterfoilShared counterfoilShared;
    @Autowired
    private SaleSunatEmissionService saleSunatEmissionService;

    @Transactional
    public SaleDetailDto save(PresaleDetailDto presaleDetail) throws SaleException, SaleBuildException {

        if(presaleDetail.Headboard == null){
            throw new SaleException("No existe cabecera de venta.");
        }
        if(presaleDetail.DetailList == null || presaleDetail.DetailList.size() == 0){
            throw new SaleException("Detalle de venta esta vacío.");
        }

        SaleHeadEntity saleHead = this.createSaleHead(presaleDetail);
        List<SaleDetEntity> detailSale = this.createSaleDetEntities(presaleDetail,saleHead);
        List<SaleDetWarehouseEntity> detailSaleWarehouse = this.createSaleDetWarehouseEntities(presaleDetail,saleHead);
        List<SaleAppliedTaxEntity> SaleAppliedTaxList = this.createSaleAppliedTaxEntities(saleHead);

        this.saleHeadRepository.save(saleHead);
        this.saleDetRepository.saveAll(detailSale);
        this.saleDetWarehouseRepository.saveAll(detailSaleWarehouse);
        this.saleAppliedTaxRepository.saveAll(SaleAppliedTaxList);

        return this.saleSearchService.findById(saleHead.SaleCod);
    }

    private BigDecimal calculateBaseTax(List<TaxEntity> taxList, BigDecimal total)
    {
        BigDecimal taxTotal = taxList.stream()
                .map( e-> e.TaxRateValue )
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        BigDecimal numDivisor = ( taxTotal.add(BigDecimal.valueOf(100)) ).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);

        return total.divide(numDivisor,2, RoundingMode.HALF_UP);
    }

    public SaleHeadEntity createSaleHead(PresaleDetailDto presaleDetail) throws SaleBuildException {

        List<TaxEntity> taxList = this.taxRepository.findAllActive();

        String SaleCod = this.saleHeadRepository.getSaleCod(getStoreCod());
        PeriodEntity period = this.periodRepository.findPeriodActuality();

        BigDecimal NumTotalPriceNoTax = calculateBaseTax(taxList,presaleDetail.Headboard.NumTotalPrice);
        BigDecimal NumTotalTax = presaleDetail.Headboard.NumTotalPrice.subtract(NumTotalPriceNoTax);

        SaleHeadEntity saleHead = new SaleHeadEntity()
                .build(presaleDetail.Headboard,period,SaleCod,StatusConst.PENDING)
                .tax(NumTotalPriceNoTax,NumTotalTax)
                .session(getUserCod())
                .validate();

        return saleHead;
    }

    public List<SaleDetEntity> createSaleDetEntities(PresaleDetailDto presaleDetail,SaleHeadEntity saleHead) throws SaleBuildException {
        List<SaleDetEntity> detailSale = new ArrayList<>();
        for( var item : presaleDetail.DetailList )
        {
            SaleDetEntity saleDet = new SaleDetEntity()
                    .build(item,saleHead.SaleCod)
                    .session(getUserCod())
                    .validate();

            detailSale.add(saleDet);
        }
        return detailSale;
    }

    public List<SaleDetWarehouseEntity> createSaleDetWarehouseEntities(PresaleDetailDto presaleDetail,SaleHeadEntity saleHead) {
        List<SaleDetWarehouseEntity> detailSaleWarehouse = new ArrayList<>();
        for( var item : presaleDetail.DetailList )
        {
            if( item.DetailWarehouse != null && item.DetailWarehouse.size() >0 )
            {
                List<SaleDetWarehouseEntity> detailSaleWarehouseSub = item.DetailWarehouse.stream()
                        .map(  itemWarehouse -> new SaleDetWarehouseEntity()
                                .build(itemWarehouse,saleHead.SaleCod)
                                .session(getUserCod())
                                .validate()
                        )
                        .toList();

                detailSaleWarehouse.addAll(detailSaleWarehouseSub);
            }
        }
        return detailSaleWarehouse;
    }

    public List<SaleAppliedTaxEntity> createSaleAppliedTaxEntities(SaleHeadEntity saleHead){
        List<TaxEntity> taxList = this.taxRepository.findAllActive();
        List<SaleAppliedTaxEntity> SaleAppliedTaxList = taxList.stream()
                .map( e -> new SaleAppliedTaxEntity()
                        .build(e.TaxCod,saleHead.SaleCod,e.TaxRateValue)
                        .session(getUserCod())
                        .validate() )
                .toList();
        return SaleAppliedTaxList;
    }

    @Transactional
    public SaleDetailDto confirm(String SaleCod,String DocumentType,String CounterfoilCod) throws SaleException {
        log.info("INI - CONFIRMACION DE VENTA : {}",SaleCod);

        SaleHeadEntity saleHead = this.saleHeadRepository.findById(SaleCod).get();
        List<SaleDetWarehouseEntity> saleDetWarehouseList = this.saleDetWarehouseRepository.findBySaleCod(SaleCod);

        if(saleHead.SaleStatus.equals(SaleConstants.CONFIRMED)){
            throw new SaleException("Sale has already been completed");
        }

        List<KardexEntity> kardexList = this.createkardexList(saleDetWarehouseList,saleHead);

        saleHead.SaleStatus = SaleConstants.CONFIRMED;
        saleHead.addSession(getUserCod());

        SaleDocumentEntity saleDocument = counterfoilShared.generateDocumentSale(saleHead.StoreCod,DocumentType,saleHead.SaleCod);

        this.saleHeadRepository.save(saleHead);
        this.kardexShared.saveAll(kardexList);
        this.saleDocumentRepository.save(saleDocument);

        SaleDetailDto saleDetail = this.saleSearchService.findById(saleHead.SaleCod);

        this.rankingProduct(saleDetail);
        this.emitSunatAfterCommit(saleHead.SaleCod);

        log.info("FIN - CONFIRMACION DE VENTA : {}",SaleCod);

        return saleDetail;
    }

    private void rankingProduct(SaleDetailDto saleDetail){
        SaleRankingService saleRankingService = new SaleRankingService(
                productRankingService,saleDetail
        );
        this.genericQueuedService.addQueued(saleRankingService);
    }

    private void emitSunatAfterCommit(String saleCod) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    queueSunatEmission(saleCod);
                }
            });
            return;
        }
        queueSunatEmission(saleCod);
    }

    private void queueSunatEmission(String saleCod) {
        this.genericQueuedService.addQueued(new SaleSunatEmissionTaskService(this.saleSunatEmissionService, saleCod));
    }

    private List<KardexEntity> createkardexList(List<SaleDetWarehouseEntity> saleDetWarehouseList,SaleHeadEntity saleHead){
        List<KardexEntity> kardexList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();

        for (var item : saleDetWarehouseList) {
            String key = this.stockKey(item.ProductCod, item.Variant, saleHead.StoreCod, item.WarehouseCod);
            KardexEntity kardexLast = lastMovementByStock.computeIfAbsent(
                    key,
                    ignored -> this.kardexShared.findLastMovement(item.ProductCod,item.Variant,item.WarehouseCod,saleHead.StoreCod)
            );
            KardexEntity kardex = new KardexEntity(kardexLast,item,saleHead.StoreCod)
                    .session(getUserCod());
            kardexList.add(kardex);
            lastMovementByStock.put(key, kardex);
        }
        return kardexList;
    }

    private String stockKey(String productCod, String variant, String storeCod, String warehouseCod) {
        return productCod + "|" + variant + "|" + storeCod + "|" + warehouseCod;
    }


    public SaleHeadEntity saveClientSale(String SaleCod, String ClientCod) throws SaleException {
        SaleHeadEntity saleHead = this.saleHeadRepository.findById(SaleCod).get();
        if(saleHead == null){
            throw new SaleException("No existe la venta.");
        }
        saleHead.ClientCod = ClientCod;
        saleHead.addSession(getUserCod());
        return this.saleHeadRepository.save(saleHead);
    }
}
