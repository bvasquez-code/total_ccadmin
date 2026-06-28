package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.ProductInfoEntity;
import com.ccadmin.app.product.model.entity.ProductInfoWarehouseEntity;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.ProductInfoShared;
import com.ccadmin.app.product.shared.ProductInfoWarehouseShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.pucharse.exception.PucharseException;
import com.ccadmin.app.pucharse.model.dto.PucharseDetConfirmDto;
import com.ccadmin.app.pucharse.model.dto.PucharseDetailsDto;
import com.ccadmin.app.pucharse.model.dto.PucharseRegisterDto;
import com.ccadmin.app.pucharse.model.entity.*;
import com.ccadmin.app.pucharse.repository.*;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.store.shared.WarehouseShared;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PucharseService extends SessionService {

    public static Logger log = LogManager.getLogger(PucharseService.class);
    @Autowired
    private PucharseHeadRepository pucharseHeadRepository;
    @Autowired
    private PucharseDetRepository pucharseDetRepository;
    @Autowired
    private PucharseRequestHeadRepository pucharseRequestHeadRepository;
    @Autowired
    private PucharseRequestDetRepository pucharseRequestDetRepository;
    @Autowired
    private PucharseDetDeliveryRepository pucharseDetDeliveryRepository;
    @Autowired
    private KardexShared kardexShared;
    @Autowired
    private ProductInfoWarehouseShared productInfoWarehouseShared;
    @Autowired
    private ProductInfoShared productInfoShared;
    @Autowired
    private WarehouseShared warehouseShared;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private StoreShared storeShared;
    private SearchService searchService;

    @Transactional
    public PucharseDetailsDto save(PucharseRegisterDto pucharseRegister) throws Exception {

        PucharseRequestHeadEntity headRequest = pucharseRequestHeadRepository.findById(pucharseRegister.PucharseReqCod).get();
        List<PucharseRequestDetEntity> detailRequestList = pucharseRequestDetRepository.findAllActive(pucharseRegister.PucharseReqCod);

        if( !headRequest.PurchaseStatus.equals(StatusConst.PENDING) )
        {
            throw new PucharseException("¡Request is Confirmed!");
        }

        PucharseHeadEntity head = new PucharseHeadEntity(headRequest);
        head.addSession(getUserCod(),true);
        head.PucharseCod = this.pucharseHeadRepository.getPucharseCod(getStoreCod());
        head.PurchaseStatus = StatusConst.PENDING;
        head.PucharseReqCod = pucharseRegister.PucharseReqCod;
        List<PucharseDetEntity> detailList = new ArrayList<>();

        int itemNumber = 1;
        for (var item : detailRequestList)
        {
            PucharseDetEntity pucharseDet = new PucharseDetEntity(item);
            pucharseDet.addSession(getUserCod(),true);
            pucharseDet.PucharseCod = head.PucharseCod;
            pucharseDet.ItemNumber = itemNumber++;
            detailList.add(pucharseDet);
        }

        this.pucharseHeadRepository.save(head);
        this.pucharseDetRepository.saveAll(detailList);

        headRequest.PurchaseStatus = StatusConst.FINALIZED;
        this.pucharseRequestHeadRepository.save(headRequest);

        return findById(head.PucharseCod);
    }

    public PucharseDetailsDto findById(String PucharseCod)
    {
        PucharseDetailsDto pucharseDetails = new PucharseDetailsDto();

        pucharseDetails.Headboard = this.pucharseHeadRepository.findById(PucharseCod).get();
        pucharseDetails.DetailList = this.pucharseDetRepository.findAllActive(PucharseCod);

        return pucharseDetails;
    }

    @Transactional
    public PucharseDetailsDto confirm(PucharseRegisterDto pucharseRegister) throws Exception {

        PucharseHeadEntity Headboard = this.pucharseHeadRepository.findById(pucharseRegister.PucharseCod).get();
        List<PucharseDetEntity> DetailList = this.pucharseDetRepository.findAllActive(pucharseRegister.PucharseCod);
        List<KardexEntity> kardexList = new ArrayList<>();
        List<ProductInfoEntity> productInfoList = new ArrayList<>();
        List<ProductInfoWarehouseEntity> productWarehouseList = new ArrayList<>();
        List<PucharseDetDeliveryEntity> DeliveryList = new ArrayList<>();
        Map<String, KardexEntity> lastMovementByStock = new HashMap<>();
        WarehouseEntity warehouseUnit = new WarehouseEntity();

        boolean IsMultipleWarehouse = warehouseShared.IsMultipleWarehouse(Headboard.StoreCod);

        if (!IsMultipleWarehouse)
        {
            warehouseUnit = this.warehouseShared.findByStore(Headboard.StoreCod).get(0);
        }

        if( !Headboard.PurchaseStatus.equals(StatusConst.PENDING) )
        {
            throw new PucharseException("purchase has already been delivered");
        }

        for(var item : DetailList)
        {

            List<PucharseDetDeliveryEntity> detailWarehouseCod = new ArrayList<>();

            if( IsMultipleWarehouse )
            {
                detailWarehouseCod = pucharseRegister.DeliveryList.stream().filter(
                        e-> e.ItemNumber == item.ItemNumber
                ).toList();
            }
            else
            {
                PucharseDetDeliveryEntity detDelivery = new PucharseDetDeliveryEntity();
                detDelivery.PucharseCod = pucharseRegister.PucharseCod;
                detDelivery.ItemNumber = item.ItemNumber;
                detDelivery.ProductCod = item.ProductCod;
                detDelivery.Variant = item.Variant;
                detDelivery.NumUnit = item.NumUnit;
                detDelivery.WarehouseCod = warehouseUnit.WarehouseCod;
                detDelivery.LotNumber = item.LotNumber;
                detDelivery.ExpirationDate = item.ExpirationDate;
                detailWarehouseCod.add(detDelivery);
            }

            for(var itemWarehouse : detailWarehouseCod )
            {
                int NumStockBefore = 0;

                itemWarehouse.PucharseCod = pucharseRegister.PucharseCod;
                itemWarehouse.ItemNumber = item.ItemNumber;
                itemWarehouse.ProductCod = item.ProductCod;
                itemWarehouse.Variant = item.Variant;
                itemWarehouse.LotNumber = item.LotNumber;
                itemWarehouse.ExpirationDate = item.ExpirationDate;
                itemWarehouse.addSession(getUserCod(),true);

                String stockKey = this.stockKey(item.ProductCod, item.Variant, Headboard.StoreCod, itemWarehouse.WarehouseCod);
                KardexEntity kardexLast = lastMovementByStock.computeIfAbsent(
                        stockKey,
                        ignored -> this.kardexShared.findLastMovement(item.ProductCod,item.Variant,itemWarehouse.WarehouseCod,Headboard.StoreCod)
                );

                if( kardexLast != null ) NumStockBefore = kardexLast.NumStockAfter;

                KardexEntity kardex = new KardexEntity(kardexLast,itemWarehouse,Headboard.StoreCod);
                kardex.addSession(getUserCod(),true);
                kardexList.add(kardex);
                lastMovementByStock.put(stockKey, kardex);

                ProductInfoWarehouseEntity productInfoWarehouse = this.productInfoWarehouseShared.findById(
                        new ProductInfoWarehouseId(item.ProductCod,item.Variant,itemWarehouse.WarehouseCod)
                );
                productInfoWarehouse.addStock(itemWarehouse.NumUnit);
                productInfoWarehouse.addSession(getUserCod(),false);
                productWarehouseList.add(productInfoWarehouse);

                DeliveryList.add(itemWarehouse);
            }

            ProductInfoEntity productInfo = this.productInfoShared.findById(
                    new ProductInfoId(item.ProductCod,item.Variant,Headboard.StoreCod)
            );
            productInfo.addStock(item.NumUnit);
            productInfo.addSession(getUserCod(),false);
            productInfoList.add(productInfo);
        }

        Headboard.PurchaseStatus = StatusConst.FINALIZED;
        Headboard.addSession(getUserCod(),false);
        this.pucharseHeadRepository.save(Headboard);
        this.pucharseDetDeliveryRepository.saveAll(DeliveryList);
        this.kardexShared.saveAll(kardexList);
        this.productInfoShared.saveAll(productInfoList);
        this.productInfoWarehouseShared.saveAll(productWarehouseList);

        return findById(Headboard.PucharseCod);
    }

    public ResponseWsDto findDataForm(String PucharseCod)
    {
        ResponseWsDto rpt = new ResponseWsDto();

        PucharseDetailsDto pucharseDetails = findById(PucharseCod);

        for(var item : pucharseDetails.DetailList){
            item.Product = this.productShared.findById(item.ProductCod);
        }

        List<WarehouseEntity> warehouseList = this.warehouseShared.findByStore(getStoreCod());
        StoreEntity store = this.storeShared.findById(getStoreCod());

        rpt.AddResponseAdditional("PucharseDetails",pucharseDetails);
        rpt.AddResponseAdditional("WarehouseList",warehouseList);
        rpt.AddResponseAdditional("Store",store);

        return rpt;
    }

    @Transactional
    public PucharseHeadEntity endReception(PucharseHeadEntity pucharseHead) throws PucharseException {

        PucharseHeadEntity pucharseHeadDB = this.pucharseHeadRepository.findById(pucharseHead.PucharseCod).get();

        if( !pucharseHeadDB.PurchaseStatus.equals(StatusConst.PENDING) )
        {
            throw new PucharseException("purchase has already been delivered");
        }
        pucharseHeadDB.PurchaseStatus = StatusConst.FINALIZED;
        pucharseHeadDB.addSession(getUserCod(),false);
        return this.pucharseHeadRepository.save(pucharseHeadDB);
    }

    public ResponsePageSearch findAll(String Query, int Page, String StoreCod)
    {
        SearchDto search = new SearchDto(Query,Page,StoreCod);
        this.searchService = new SearchService(this.pucharseHeadRepository);
        return this.searchService.findAllStore(search,10);
    }

    private String stockKey(String productCod, String variant, String storeCod, String warehouseCod) {
        return productCod + "|" + variant + "|" + storeCod + "|" + warehouseCod;
    }
}
