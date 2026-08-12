package com.ccadmin.app.sale.service;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.service.ProductRankingService;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.sale.exception.PresaleBuildException;
import com.ccadmin.app.sale.exception.PresaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SalesContextDto;
import com.ccadmin.app.sale.model.entity.PeriodEntity;
import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;
import com.ccadmin.app.sale.model.factory.PresaleDetWarehouseEntityFactory;
import com.ccadmin.app.sale.model.factory.PresaleDetWarehouseIdFactory;
import com.ccadmin.app.sale.model.factory.PresaleHeadEntityFactory;
import com.ccadmin.app.sale.repository.CommercialChannelRepository;
import com.ccadmin.app.sale.repository.PeriodRepository;
import com.ccadmin.app.sale.repository.PresaleChannelRepository;
import com.ccadmin.app.sale.repository.PresaleDetRepository;
import com.ccadmin.app.sale.repository.PresaleDetWarehouseRepository;
import com.ccadmin.app.sale.repository.PresaleHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
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
    private CommercialChannelRepository commercialChannelRepository;
    @Autowired
    private PresaleChannelRepository presaleChannelRepository;
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
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private CreditNoteApplicationCreateService creditNoteApplicationCreateService;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private CatalogSearchShared catalogSearchShared;
    @Autowired
    private ManualDiscountValidationService manualDiscountValidationService;
    @Autowired
    private SalesContextService salesContextService;

    public String createCode(){
        String PresaleCod = presaleHeadRepository.getPresaleCod(getStoreCod());
        log.info("CREATE_CODE_PRESALE -->> {}",PresaleCod);
        return PresaleCod;
    }

    public String createCodeWeb(String storeCod) {
        SalesContextDto salesContext = salesContextService.getWebContext(storeCod);
        String presaleCod = presaleHeadRepository.getPresaleCod(salesContext.StoreCod);
        log.info("CREATE_CODE_WEB_PRESALE -->> {}", presaleCod);
        return presaleCod;
    }

    @Transactional
    public PresaleDetailDto save(PresaleRegisterDto presaleRegister) throws PresaleBuildException {

        SalesContextDto salesContext = salesContextService.getInternalContext();
        return this.save(presaleRegister, salesContext);
    }

    @Transactional
    public PresaleDetailDto saveWeb(PresaleRegisterDto presaleRegister, String storeCod)
            throws PresaleBuildException {
        return this.save(presaleRegister, salesContextService.getWebContext(storeCod));
    }

    private PresaleDetailDto save(PresaleRegisterDto presaleRegister, SalesContextDto salesContext) throws PresaleBuildException {
        log.info("INI - CREACION DE PREVENTA : {}",presaleRegister.Headboard.PresaleCod);
        presaleRegister.Headboard = this.createPresaleHead(presaleRegister,salesContext);
        presaleRegister.DetailList = this.recalculateAmountPresaleDet(presaleRegister,salesContext);
        presaleRegister.Headboard = this.recalculateAmountPresaleHead(presaleRegister);
        List<PresaleDetWarehouseEntity> presaleDetWarehouseList = this.createDetailWarehouseDefault(presaleRegister,salesContext);
        presaleRegister.PresaleChannel = this.createPresaleChannel(presaleRegister, salesContext);

        this.presaleHeadRepository.save(presaleRegister.Headboard);
        this.presaleChannelRepository.save(presaleRegister.PresaleChannel);
        this.presaleDetRepository.saveAll(presaleRegister.DetailList);
        this.presaleDetWarehouseRepository.saveAll(presaleDetWarehouseList);
        PresaleDetailDto  presaleDetail = presaleSearchService.findById(presaleRegister.Headboard.PresaleCod);
        this.rankingProduct(presaleDetail);
        log.info("FIN - CREACION DE VENTA : {}",presaleRegister.Headboard.PresaleCod);
        return presaleDetail;
    }

    @Transactional
    public SaleDetailDto confirm(PresaleRegisterDto presaleRegister) throws Exception {

        return this.confirm(
                presaleRegister,
                salesContextService.getInternalContext(),
                null,
                false
        );
    }

    @Transactional
    public SaleDetailDto confirmWeb(
            PresaleRegisterDto presaleRegister,
            String storeCod,
            String clientCod
    ) throws Exception {
        return this.confirm(
                presaleRegister,
                salesContextService.getWebContext(storeCod),
                clientCod,
                true
        );
    }

    private SaleDetailDto confirm(
            PresaleRegisterDto presaleRegister,
            SalesContextDto salesContext,
            String expectedClientCod,
            boolean webSale
    ) throws Exception {

        Optional<PresaleHeadEntity> presaleOptional = this.presaleHeadRepository.findByIdForUpdate(
                presaleRegister.Headboard.PresaleCod
        );

        if(presaleOptional.isEmpty()){
            throw new PresaleException("There is no sales code");
        }
        PresaleHeadEntity presale = presaleOptional.get();

        if (webSale) {
            validateWebPresale(presaleRegister, presale, salesContext, expectedClientCod);
        }

        if(presale.SaleStatus.equals(StatusConst.CONFIRMED)){
            throw new PresaleException("Pre-sale has already been confirmed");
        }
        presale.SaleStatus = StatusConst.CONFIRMED;
        presale.addSession(salesContext.UserCod);
        this.presaleHeadRepository.save(presale);

        PresaleDetailDto presaleDetail = this.presaleSearchService.findById(presale.PresaleCod);
        SaleDetailDto saleDetail = webSale
                ? this.saleCreateService.saveWeb(
                        presaleDetail,
                        presaleRegister.SaleBilling,
                        salesContext.StoreCod
                )
                : this.saleCreateService.save(presaleDetail, presaleRegister.SaleBilling);
        if (!StatusConst.PENDING.equals(saleDetail.Headboard.SaleStatus)
                || !presale.PresaleCod.equals(saleDetail.Headboard.PresaleCod)) {
            throw new PresaleException("La venta pendiente no corresponde a la preventa confirmada");
        }
        if (!webSale && presaleRegister.CreditNoteCod != null && !presaleRegister.CreditNoteCod.isBlank()) {
            this.creditNoteApplicationCreateService.applyAvailableBalance(
                    presaleRegister.CreditNoteCod,
                    saleDetail.Headboard
            );
            saleDetail = this.saleSearchService.findById(saleDetail.Headboard.SaleCod);
        }
        List<PresaleDetWarehouseEntity> detailList =
                this.presaleDetWarehouseRepository.findActiveByPresaleCod(presale.PresaleCod);
        if (detailList.isEmpty()) {
            throw new PresaleException("La preventa no tiene stock asignado por almacen");
        }
        List<KardexZoneEntity> kardexZoneList = this.kardexShared.buildPresaleReservation(
                presale, detailList, salesContext.UserCod
        );
        this.kardexShared.saveAll(List.of(), kardexZoneList);

        return saleDetail;
    }

    private void validateWebPresale(
            PresaleRegisterDto request,
            PresaleHeadEntity presale,
            SalesContextDto salesContext,
            String expectedClientCod
    ) throws PresaleException {
        if (expectedClientCod == null || expectedClientCod.isBlank()
                || !expectedClientCod.equals(presale.ClientCod)) {
            throw new PresaleException("La preventa no pertenece al cliente autenticado");
        }
        if (!salesContext.StoreCod.equals(presale.StoreCod)) {
            throw new PresaleException("La preventa no pertenece a la tienda indicada");
        }
        if (request.CreditNoteCod != null && !request.CreditNoteCod.isBlank()) {
            throw new PresaleException("La tienda virtual no admite aplicaciones manuales de nota de crédito");
        }
        PresaleChannelEntity channel = this.presaleChannelRepository
                .findByPresaleCod(presale.PresaleCod)
                .orElseThrow(() -> new PresaleException("La preventa no tiene canal de venta"));
        if (!SaleConstants.COMMERCIAL_CHANNEL_WEB.equals(channel.ChannelCod)) {
            throw new PresaleException("La preventa no corresponde al canal WEB");
        }
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

    public List<PresaleDetEntity> recalculateAmountPresaleDet(PresaleRegisterDto presaleRegister,SalesContextDto salesContext) throws PresaleBuildException {
        int itemNumber = 1;
        boolean manualDiscountEnabled = this.catalogSearchShared.isIndicatorSystemEnabled(
                BusinessConfigConstants.ConfigCod.IND_MANUAL_DISCOUNT
        );
        for(var product : presaleRegister.DetailList)
        {
            product.PresaleCod = presaleRegister.Headboard.PresaleCod;
            if (product.ItemNumber <= 0) {
                product.ItemNumber = itemNumber;
            }
            ProductConfigEntity config = this.productOperationConfigShared.findByProduct(product.ProductCod, salesContext.StoreCod);
            product.IsDigital = config.IsDigital;
            if (product.ProductUnitName == null || product.ProductUnitName.trim().isEmpty()) {
                product.ProductUnitName = config.ProductUnitName;
            }
            if (product.ProductUnitFactor <= 0) {
                product.ProductUnitFactor = config.ProductUnitFactor;
            }
            this.productOperationConfigShared.validateInternalQuantity(product.ProductCod, product.NumUnit, product.ProductUnitFactor);
            product.NumDiscount = this.manualDiscountValidationService.validate(
                    product.ProductCod,
                    product.NumUnitPrice,
                    product.NumDiscount,
                    config,
                    manualDiscountEnabled
            );
            product.NumUnitPriceSale = product.NumUnitPrice.subtract( product.NumDiscount );
            product.NumTotalPrice = product.NumUnitPriceSale.multiply(new BigDecimal(product.NumUnit));
            product.addSession(salesContext.UserCod);
            product.validate();
            itemNumber++;
        }
        return presaleRegister.DetailList;
    }

    private List<PresaleDetWarehouseEntity> createDetailWarehouseDefault(PresaleRegisterDto presaleRegister,SalesContextDto salesContext) throws PresaleBuildException {
        List<PresaleDetWarehouseEntity> presaleDetWarehouseList = new ArrayList<>();
        WarehouseEntity warehouseDefault = this.warehouseShared.findByStore(salesContext.StoreCod).get(0);

        for(var product : presaleRegister.DetailList)
        {
            Optional<PresaleDetWarehouseEntity> detWarehouseOp = this.presaleDetWarehouseRepository.findById(
                    PresaleDetWarehouseIdFactory.fromDetail(product)
            );

            PresaleDetWarehouseEntity detWarehouse = PresaleDetWarehouseEntityFactory.fromDetail(
                            product,
                            warehouseDefault,
                            detWarehouseOp.orElse(null)
                    )
                    .session(salesContext.UserCod)
                    .validate();

            presaleDetWarehouseList.add(detWarehouse);
        }

        return presaleDetWarehouseList;
    }

    private PresaleHeadEntity createPresaleHead(PresaleRegisterDto presaleRegister,SalesContextDto salesContext) throws PresaleBuildException {

        PresaleHeadEntity presaleHead = presaleRegister.Headboard;

        CurrencyEntity currencySystem = this.currencyShared.findCurrencySystem();
        CurrencyEntity currencyPucharse = this.currencyShared.findById(presaleRegister.Headboard.CurrencyCod);
        PeriodEntity period = this.periodRepository.findPeriodActuality();

        if(!presaleHead.isEmptyPresaleCod() && this.presaleHeadRepository.existsById(presaleHead.PresaleCod)){
            this.inactiveStatusDetailPresale(presaleHead.PresaleCod);
        }else if(presaleHead.isEmptyPresaleCod()){
            presaleHead.PresaleCod = presaleHeadRepository.getPresaleCod(salesContext.StoreCod);
        }

        PresaleHeadEntityFactory.fromSaveRequest(
                        presaleHead,
                        period,
                        currencySystem,
                        currencyPucharse,
                        salesContext.StoreCod,
                        StatusConst.PENDING
                )
                .session(salesContext.UserCod)
                .validate();
        presaleHead.CashSessionID = salesContext.CashSessionID;

        return presaleHead;
    }

    private PresaleChannelEntity createPresaleChannel(
            PresaleRegisterDto presaleRegister,
            SalesContextDto salesContext
    ) {
        String channelCod = presaleRegister.PresaleChannel == null
                ? null
                : presaleRegister.PresaleChannel.ChannelCod;

        if (channelCod == null || channelCod.isBlank()
                || this.commercialChannelRepository.findByChannelCod(channelCod).isEmpty()) {
            throw new PresaleBuildException("El canal de venta indicado no existe");
        }

        PresaleChannelEntity presaleChannel = this.presaleChannelRepository
                .findByPresaleCod(presaleRegister.Headboard.PresaleCod)
                .orElseGet(PresaleChannelEntity::new);
        presaleChannel.PresaleCod = presaleRegister.Headboard.PresaleCod;
        presaleChannel.ChannelCod = channelCod;
        presaleChannel.Status = "A";
        presaleChannel.addSession(salesContext.UserCod);

        return presaleChannel;
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
