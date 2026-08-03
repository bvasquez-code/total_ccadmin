package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.service.ProductRankingService;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SaleTaxCalculationResultDto;
import com.ccadmin.app.sale.model.entity.*;
import com.ccadmin.app.sale.repository.*;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private SaleDetTaxRepository saleDetTaxRepository;
    @Autowired
    private SaleAppliedTaxRepository saleAppliedTaxRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private GenericQueuedService genericQueuedService;
    @Autowired
    private ProductRankingService productRankingService;
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private SaleTaxCalculationService saleTaxCalculationService;
    @Autowired
    private SaleDocumentCreateService saleDocumentCreateService;

    @Transactional
    public SaleDetailDto save(PresaleDetailDto presaleDetail) throws SaleException, SaleBuildException {

        if(presaleDetail.Headboard == null){
            throw new SaleException("No existe cabecera de venta.");
        }
        if(presaleDetail.DetailList == null || presaleDetail.DetailList.size() == 0){
            throw new SaleException("Detalle de venta esta vacío.");
        }

        SaleHeadEntity saleHead = this.createSaleHead(presaleDetail);
        SaleTaxCalculationResultDto taxCalculation = this.saleTaxCalculationService.buildSaleDetails(
                presaleDetail.DetailList,
                saleHead.SaleCod,
                saleHead.StoreCod,
                getUserCod()
        );
        saleHead.NumTotalPrice = taxCalculation.NumTotalPrice;
        saleHead.tax(taxCalculation.NumTotalPriceNoTax, taxCalculation.NumTotalTax)
                .session(getUserCod())
                .validate();
        List<SaleDetEntity> detailSale = taxCalculation.DetailList;
        List<SaleDetTaxEntity> detailTaxSale = taxCalculation.TaxDetailList;
        List<SaleDetWarehouseEntity> detailSaleWarehouse = this.createSaleDetWarehouseEntities(presaleDetail,saleHead);
        List<SaleAppliedTaxEntity> SaleAppliedTaxList = this.createSaleAppliedTaxEntities(saleHead, detailTaxSale);

        this.saleHeadRepository.save(saleHead);
        this.saleDetRepository.saveAll(detailSale);
        this.saleDetWarehouseRepository.saveAll(detailSaleWarehouse);
        this.saleDetTaxRepository.saveAll(detailTaxSale);
        this.saleAppliedTaxRepository.saveAll(SaleAppliedTaxList);

        return this.saleSearchService.findById(saleHead.SaleCod);
    }

    public SaleHeadEntity createSaleHead(PresaleDetailDto presaleDetail) throws SaleBuildException {
        String SaleCod = this.saleHeadRepository.getSaleCod(getStoreCod());
        PeriodEntity period = this.periodRepository.findPeriodActuality();

        SaleHeadEntity saleHead = new SaleHeadEntity()
                .build(presaleDetail.Headboard,period,SaleCod,StatusConst.PENDING)
                .session(getUserCod())
                .validate();

        return saleHead;
    }

    public SaleHeadEntity createSaleHead(PresaleDetailDto presaleDetail, List<TaxEntity> taxList) throws SaleBuildException {
        return createSaleHead(presaleDetail);
    }

    public List<SaleDetEntity> createSaleDetEntities(PresaleDetailDto presaleDetail,SaleHeadEntity saleHead) throws SaleBuildException {
        return this.saleTaxCalculationService.buildSaleDetails(
                presaleDetail.DetailList,
                saleHead.SaleCod,
                saleHead.StoreCod,
                getUserCod()
        ).DetailList;
    }

    public List<SaleDetEntity> createSaleDetEntities(PresaleDetailDto presaleDetail,SaleHeadEntity saleHead, List<TaxEntity> taxList) throws SaleBuildException {
        return this.createSaleDetEntities(presaleDetail, saleHead);
    }

    public List<SaleDetWarehouseEntity> createSaleDetWarehouseEntities(PresaleDetailDto presaleDetail,SaleHeadEntity saleHead) {
        List<SaleDetWarehouseEntity> detailSaleWarehouse = new ArrayList<>();
        for( var item : presaleDetail.DetailList )
        {
            if( item.DetailWarehouse != null && item.DetailWarehouse.size() >0 )
            {
                if (item.DetailWarehouse.size() != 1) {
                    throw new SaleBuildException(
                            "Cada item de preventa debe tener una unica asignacion de almacen"
                    );
                }
                SaleDetWarehouseEntity detailWarehouse = new SaleDetWarehouseEntity()
                        .build(item.DetailWarehouse.get(0), saleHead.SaleCod)
                        .session(getUserCod())
                        .validate();
                detailSaleWarehouse.add(detailWarehouse);
            }
        }
        return detailSaleWarehouse;
    }

    public List<SaleAppliedTaxEntity> createSaleAppliedTaxEntities(SaleHeadEntity saleHead){
        return createSaleAppliedTaxEntities(saleHead, this.saleDetTaxRepository.findBySaleCod(saleHead.SaleCod));
    }

    public List<SaleAppliedTaxEntity> createSaleAppliedTaxEntitiesFromCatalog(SaleHeadEntity saleHead, List<TaxEntity> taxList){
        List<SaleAppliedTaxEntity> SaleAppliedTaxList = taxList.stream()
                .map( e -> new SaleAppliedTaxEntity()
                        .build(e.TaxCod,saleHead.SaleCod,e.TaxRateValue)
                        .session(getUserCod())
                        .validate() )
                .toList();
        return SaleAppliedTaxList;
    }

    public List<SaleAppliedTaxEntity> createSaleAppliedTaxEntities(SaleHeadEntity saleHead, List<SaleDetTaxEntity> taxLineList){
        Map<String, BigDecimal> taxRateByTaxCod = new LinkedHashMap<>();
        for (SaleDetTaxEntity taxLine : taxLineList) {
            if ("S".equals(taxLine.IsInformative) || amount(taxLine.TaxAmount).compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            taxRateByTaxCod.putIfAbsent(taxLine.TaxCod, taxLine.TaxRateValue == null ? BigDecimal.ZERO : taxLine.TaxRateValue);
        }
        return taxRateByTaxCod.entrySet().stream()
                .map(entry -> new SaleAppliedTaxEntity()
                        .build(entry.getKey(), saleHead.SaleCod, entry.getValue())
                        .session(getUserCod())
                        .validate())
                .toList();
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(rollbackOn = Exception.class)
    public SaleDetailDto confirm(String SaleCod,String DocumentType,String CounterfoilCod) throws SaleException {
        log.info("INI - CONFIRMACION DE VENTA : {}",SaleCod);

        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(SaleCod)
                .orElseThrow(() -> new SaleException("No existe la venta " + SaleCod));
        List<SaleDetWarehouseEntity> saleDetWarehouseList = this.saleDetWarehouseRepository.findBySaleCod(SaleCod);

        if(!SaleConstants.PENDING.equals(saleHead.SaleStatus)){
            throw new SaleException("La venta ya no se encuentra pendiente");
        }
        if(saleDetWarehouseList.isEmpty()){
            throw new SaleException("La venta no tiene stock asignado por almacen");
        }

        List<KardexEntity> kardexList = this.kardexShared.buildSaleConfirmation(
                saleHead, saleDetWarehouseList, getUserCod()
        );
        List<KardexZoneEntity> kardexZoneList = this.kardexShared.buildZoneSaleConfirmation(
                saleHead, saleDetWarehouseList, getUserCod()
        );

        saleHead.SaleStatus = SaleConstants.CONFIRMED;
        saleHead.addSession(getUserCod());

        SaleDocumentEntity saleDocument = this.saleDocumentCreateService.createDocument(saleHead, DocumentType);

        this.saleHeadRepository.save(saleHead);
        this.kardexShared.saveAll(kardexList, kardexZoneList);

        SaleDetailDto saleDetail = this.saleSearchService.findById(saleHead.SaleCod);

        this.rankingProduct(saleDetail);
        if (SaleConstants.DOCUMENT_ROLE_FISCAL.equals(saleDocument.DocumentRole)) {
            this.saleDocumentCreateService.emitSunatAfterCommit(
                    saleHead.SaleCod,
                    saleDocument.DocumentCod
            );
        }

        log.info("FIN - CONFIRMACION DE VENTA : {}",SaleCod);

        return saleDetail;
    }

    private void rankingProduct(SaleDetailDto saleDetail){
        SaleRankingService saleRankingService = new SaleRankingService(
                productRankingService,saleDetail
        );
        this.genericQueuedService.addQueued(saleRankingService);
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
