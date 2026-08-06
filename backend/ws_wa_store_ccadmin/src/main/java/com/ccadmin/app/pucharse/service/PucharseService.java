package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.shared.KardexShared;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.product.shared.ProductOperationConfigShared;
import com.ccadmin.app.pucharse.exception.PucharseException;
import com.ccadmin.app.pucharse.model.dto.PucharseDetailsDto;
import com.ccadmin.app.pucharse.model.dto.PucharseRegisterDto;
import com.ccadmin.app.pucharse.model.entity.*;
import com.ccadmin.app.pucharse.model.factory.PucharseDetailsDtoFactory;
import com.ccadmin.app.pucharse.model.factory.PucharseDetDeliveryEntityFactory;
import com.ccadmin.app.pucharse.model.factory.PucharseDetEntityFactory;
import com.ccadmin.app.pucharse.model.factory.PucharseHeadEntityFactory;
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
import java.util.List;

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
    private WarehouseShared warehouseShared;
    @Autowired
    private ProductShared productShared;
    @Autowired
    private ProductOperationConfigShared productOperationConfigShared;
    @Autowired
    private StoreShared storeShared;
    private SearchService searchService;

    @Transactional
    public PucharseDetailsDto save(PucharseRegisterDto pucharseRegister) throws Exception {

        PucharseRequestHeadEntity headRequest = pucharseRequestHeadRepository.findById(pucharseRegister.PucharseReqCod).get();
        List<PucharseRequestDetEntity> detailRequestList = pucharseRequestDetRepository.findAllActive(pucharseRegister.PucharseReqCod);
        this.validateNonDigitalProducts(
                detailRequestList.stream().map(item -> item.ProductCod).toList(),
                headRequest.StoreCod
        );

        if( !headRequest.PurchaseStatus.equals(StatusConst.PENDING) )
        {
            throw new PucharseException("¡Request is Confirmed!");
        }

        String pucharseCod = this.pucharseHeadRepository.getPucharseCod(
                getStoreCod()
        );
        PucharseHeadEntity head = PucharseHeadEntityFactory.fromRequest(
                headRequest,
                pucharseCod,
                pucharseRegister.PucharseReqCod
        );
        head.addSession(getUserCod(),true);
        List<PucharseDetEntity> detailList = new ArrayList<>();

        int itemNumber = 1;
        for (var item : detailRequestList)
        {
            PucharseDetEntity pucharseDet =
                    PucharseDetEntityFactory.fromRequest(
                            item, head.PucharseCod, itemNumber++
                    );
            pucharseDet.addSession(getUserCod(),true);
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
        PucharseHeadEntity head =
                this.pucharseHeadRepository.findById(PucharseCod).get();
        List<PucharseDetEntity> details =
                this.pucharseDetRepository.findAllActive(PucharseCod);
        return PucharseDetailsDtoFactory.fromEntities(head, details);
    }

    @Transactional
    public PucharseDetailsDto confirm(PucharseRegisterDto pucharseRegister) throws Exception {

        PucharseHeadEntity Headboard = this.pucharseHeadRepository.findByIdForUpdate(pucharseRegister.PucharseCod)
                .orElseThrow(() -> new PucharseException("No existe la compra " + pucharseRegister.PucharseCod));
        List<PucharseDetEntity> DetailList = this.pucharseDetRepository.findAllActive(pucharseRegister.PucharseCod);
        this.validateNonDigitalProducts(
                DetailList.stream().map(item -> item.ProductCod).toList(),
                Headboard.StoreCod
        );
        List<PucharseDetDeliveryEntity> DeliveryList = new ArrayList<>();
        WarehouseEntity warehouseUnit = null;

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

            List<PucharseDetDeliveryEntity> detailWarehouseCod;

            if( IsMultipleWarehouse )
            {
                detailWarehouseCod = pucharseRegister.DeliveryList.stream()
                        .filter(e -> e.ItemNumber == item.ItemNumber)
                        .map(receipt ->
                                PucharseDetDeliveryEntityFactory.fromReceipt(
                                        item,
                                        pucharseRegister.PucharseCod,
                                        receipt.WarehouseCod,
                                        receipt.NumUnit
                                )
                        )
                        .toList();
            }
            else
            {
                detailWarehouseCod = List.of(
                        PucharseDetDeliveryEntityFactory.fromFullReceipt(
                                item,
                                pucharseRegister.PucharseCod,
                                warehouseUnit.WarehouseCod
                        )
                );
            }

            for(var itemWarehouse : detailWarehouseCod )
            {
                itemWarehouse.addSession(getUserCod(),true);

                DeliveryList.add(itemWarehouse);
            }

            int receivedQuantity = detailWarehouseCod.stream().mapToInt(e -> e.NumUnit).sum();
            if (receivedQuantity <= 0) {
                throw new PucharseException("La cantidad recibida debe ser mayor que cero para el item " + item.ItemNumber);
            }
            item.NumUnitDelivered = receivedQuantity;
            item.IsKardexAffected = "S";
            item.addSession(getUserCod(), false);
        }

        List<KardexEntity> kardexList = this.kardexShared.buildPurchaseReceipt(
                Headboard, DeliveryList, getUserCod()
        );
        List<KardexZoneEntity> kardexZoneList = this.kardexShared.buildZonePurchaseReceipt(
                Headboard, DeliveryList, getUserCod()
        );
        Headboard.PurchaseStatus = StatusConst.FINALIZED;
        Headboard.addSession(getUserCod(),false);
        this.pucharseHeadRepository.save(Headboard);
        this.pucharseDetRepository.saveAll(DetailList);
        this.pucharseDetDeliveryRepository.saveAll(DeliveryList);
        this.kardexShared.saveAll(kardexList, kardexZoneList);

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

        PucharseHeadEntity pucharseHeadDB = this.pucharseHeadRepository.findByIdForUpdate(pucharseHead.PucharseCod)
                .orElseThrow(() -> new PucharseException("No existe la compra " + pucharseHead.PucharseCod));

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

    private void validateNonDigitalProducts(List<String> productCodList, String storeCod)
            throws PucharseException {
        for (String productCod : productCodList) {
            if (this.productOperationConfigShared.isDigital(productCod, storeCod)) {
                throw new PucharseException(
                        "El producto " + productCod + " es digital y no puede utilizarse en compras"
                );
            }
        }
    }
}
