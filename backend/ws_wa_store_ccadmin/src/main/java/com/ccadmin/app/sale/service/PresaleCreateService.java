package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.service.ProductRankingService;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.exception.PresaleBuildException;
import com.ccadmin.app.sale.exception.PresaleException;
import com.ccadmin.app.sale.exception.SaleBuildException;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.entity.PeriodEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.entity.id.PresaleDetWarehouseID;
import com.ccadmin.app.sale.repository.PeriodRepository;
import com.ccadmin.app.sale.repository.PresaleDetRepository;
import com.ccadmin.app.sale.repository.PresaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.shared.WarehouseShared;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.shared.CurrencyShared;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PresaleCreateService extends SessionService {

    @Autowired
    private PresaleHeadRepository presaleHeadRepository;
    @Autowired
    private PresaleDetRepository presaleDetRepository;
    @Autowired
    private PresaleDetWarehouseRepository presaleDetWarehouseRepository;
    @Autowired
    private PeriodRepository periodRepository;
    @Autowired
    private GenericQueuedService genericQueuedService;
    @Autowired
    private ProductRankingService productRankingService;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;
    @Autowired
    private CurrencyShared currencyShared;
    @Autowired
    private WarehouseShared warehouseShared;
    @Autowired
    private PresaleSearchService presaleSearchService;
    @Autowired
    private SaleCreateService saleCreateService;

    public String createCode(){
        String PresaleCod = presaleHeadRepository.getPresaleCod(getStoreCod());
        log.info("CREATE_CODE_PRESALE -->> {}",PresaleCod);
        return PresaleCod;
    }

    @Transactional
    public PresaleDetailDto save(PresaleRegisterDto presaleRegister) throws PresaleBuildException {

        log.info("INI - CREACION DE PREVENTA : {}",presaleRegister.Headboard.PresaleCod);
        presaleRegister.Headboard = this.createPresaleHead(presaleRegister);
        presaleRegister.DetailList = this.recalculateAmountPresaleDet(presaleRegister);
        presaleRegister.Headboard = this.recalculateAmountPresaleHead(presaleRegister);
        List<PresaleDetWarehouseEntity> presaleDetWarehouseList = this.createDetailWarehouseDefault(presaleRegister);

        this.presaleHeadRepository.save(presaleRegister.Headboard);
        this.presaleDetRepository.saveAll(presaleRegister.DetailList);
        this.presaleDetWarehouseRepository.saveAll(presaleDetWarehouseList);
        PresaleDetailDto  presaleDetail = presaleSearchService.findById(presaleRegister.Headboard.PresaleCod);
        this.rankingProduct(presaleDetail);
        log.info("FIN - CREACION DE VENTA : {}",presaleRegister.Headboard.PresaleCod);
        return presaleDetail;
    }

    @Transactional
    public SaleDetailDto confirm(PresaleRegisterDto presaleRegister) throws PresaleException, SaleException, SaleBuildException {

        Optional<PresaleHeadEntity> presaleOptional  = this.presaleHeadRepository.findById(presaleRegister.Headboard.PresaleCod);

        if(presaleOptional.isEmpty()){
            throw new PresaleException("There is no sales code");
        }
        PresaleHeadEntity presale = presaleOptional.get();

        if(presale.SaleStatus.equals(StatusConst.CONFIRMED)){
            throw new PresaleException("Pre-sale has already been confirmed");
        }
        presale.SaleStatus = StatusConst.CONFIRMED;
        presale.addSession(getUserCod());
        this.presaleHeadRepository.save(presale);

        return this.saleCreateService.save(presaleSearchService.findById(presaleRegister.Headboard.PresaleCod));
    }

    public PresaleHeadEntity recalculateAmountPresaleHead(PresaleRegisterDto presaleRegister){

        presaleRegister.Headboard.NumPriceSubTotal = presaleRegister.DetailList.stream()
                .map( e -> e.NumUnitPrice.multiply(BigDecimal.valueOf(e.NumUnit)))
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        presaleRegister.Headboard.NumDiscount = presaleRegister.DetailList.stream()
                .map( e -> e.NumDiscount.multiply(BigDecimal.valueOf(e.NumUnit)))
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        presaleRegister.Headboard.NumTotalPrice = presaleRegister.Headboard.NumPriceSubTotal.subtract( presaleRegister.Headboard.NumDiscount );
        presaleRegister.Headboard.NumTotalTax = BigDecimal.ZERO;
        presaleRegister.Headboard.NumTotalPriceNoTax = BigDecimal.ZERO;
        return presaleRegister.Headboard;
    }

    public List<PresaleDetEntity> recalculateAmountPresaleDet(PresaleRegisterDto presaleRegister) throws PresaleBuildException {
        int itemNumber = 1;
        for(var product : presaleRegister.DetailList)
        {
            product.PresaleCod = presaleRegister.Headboard.PresaleCod;
            if (product.ItemNumber <= 0) {
                product.ItemNumber = itemNumber;
            }
            ProductConfigEntity config = this.productOperationConfigShared.findByProduct(product.ProductCod, getStoreCod());
            if (product.ProductUnitName == null || product.ProductUnitName.trim().isEmpty()) {
                product.ProductUnitName = config.ProductUnitName;
            }
            if (product.ProductUnitFactor <= 0) {
                product.ProductUnitFactor = config.ProductUnitFactor;
            }
            this.productOperationConfigShared.validateInternalQuantity(product.ProductCod, product.NumUnit, product.ProductUnitFactor);
            product.NumUnitPriceSale = product.NumUnitPrice.subtract( product.NumDiscount );
            product.NumTotalPrice = product.NumUnitPriceSale.multiply(new BigDecimal(product.NumUnit));
            product.addSession(getUserCod());
            product.validate();
            itemNumber++;
        }
        return presaleRegister.DetailList;
    }

    private List<PresaleDetWarehouseEntity> createDetailWarehouseDefault(PresaleRegisterDto presaleRegister) throws PresaleBuildException {
        List<PresaleDetWarehouseEntity> presaleDetWarehouseList = new ArrayList<>();
        WarehouseEntity warehouseDefault = this.warehouseShared.findByStore(getStoreCod()).get(0);

        for(var product : presaleRegister.DetailList)
        {
            PresaleDetWarehouseEntity detWarehouse = new PresaleDetWarehouseEntity();

            Optional<PresaleDetWarehouseEntity> detWarehouseOp = this.presaleDetWarehouseRepository.findById(
                    new PresaleDetWarehouseID(
                            product.PresaleCod,
                            product.ItemNumber
                    )
            );

            if(detWarehouseOp.isPresent()){
                detWarehouse = detWarehouseOp.get();
            }

            detWarehouse.build(product,warehouseDefault)
                    .session(getUserCod())
                    .validate();

            presaleDetWarehouseList.add(detWarehouse);
        }

        return presaleDetWarehouseList;
    }

    private PresaleHeadEntity createPresaleHead(PresaleRegisterDto presaleRegister) throws PresaleBuildException {

        PresaleHeadEntity presaleHead = presaleRegister.Headboard;

        CurrencyEntity currencySystem = this.currencyShared.findCurrencySystem();
        CurrencyEntity currencyPucharse = this.currencyShared.findById(presaleRegister.Headboard.CurrencyCod);
        PeriodEntity period = this.periodRepository.findPeriodActuality();

        if(!presaleHead.isEmptyPresaleCod() && this.presaleHeadRepository.existsById(presaleHead.PresaleCod)){
            this.inactiveStatusDetailPresale(presaleHead.PresaleCod);
        }else if(presaleHead.isEmptyPresaleCod()){
            presaleHead.PresaleCod = presaleHeadRepository.getPresaleCod(getStoreCod());
        }

        presaleHead.build(period,currencySystem,currencyPucharse,getStoreCod(),StatusConst.PENDING)
                .session(getUserCod())
                .validate();

        return presaleHead;
    }

    private void inactiveStatusDetailPresale(String PresaleCod){
        this.presaleDetRepository.updateStatusAll(PresaleCod,"I");
        this.presaleDetWarehouseRepository.updateStatusAll(PresaleCod,"I");
    }

    private void rankingProduct(PresaleDetailDto presaleDetail){
        PresaleRankingService saleRankingService = new PresaleRankingService(
                productRankingService,presaleDetail
        );
        this.genericQueuedService.addQueued(saleRankingService);
    }
}
