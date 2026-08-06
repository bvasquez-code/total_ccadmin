package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.dto.PucharseDetailsDto;
import com.ccadmin.app.pucharse.model.dto.PucharseRequestDetailsDto;
import com.ccadmin.app.pucharse.model.entity.PucharseDetDeliveryEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestHeadEntity;
import com.ccadmin.app.pucharse.model.entity.id.PucharseRequestDetId;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PucharseModelFactoryTest {

    @Test
    void createsPurchaseHeadFromRequest() {
        PucharseRequestHeadEntity request = new PucharseRequestHeadEntity();
        request.StoreCod = "T001";
        request.ExternalCod = "OC-100";
        request.DealerCod = "PRV001";
        request.Commenter = "Compra de prueba";
        request.CurrencyCod = "PEN";
        request.CurrencyCodSys = "PEN";
        request.NumExchangevalue = new BigDecimal("1.0000");
        request.NumTotalPrice = new BigDecimal("125.50");

        PucharseHeadEntity result = PucharseHeadEntityFactory.fromRequest(
                request, "CO0001", "SC0001"
        );

        assertEquals("CO0001", result.PucharseCod);
        assertEquals("SC0001", result.PucharseReqCod);
        assertEquals(request.StoreCod, result.StoreCod);
        assertEquals(request.ExternalCod, result.ExternalCod);
        assertEquals(request.DealerCod, result.DealerCod);
        assertEquals(request.Commenter, result.Commenter);
        assertEquals(StatusConst.PENDING, result.PurchaseStatus);
        assertEquals(request.CurrencyCod, result.CurrencyCod);
        assertEquals(request.CurrencyCodSys, result.CurrencyCodSys);
        assertEquals(request.NumExchangevalue, result.NumExchangevalue);
        assertEquals(request.NumTotalPrice, result.NumTotalPrice);
    }

    @Test
    void createsPurchaseDetailFromRequest() {
        PucharseRequestDetEntity request = requestDetail();

        PucharseDetEntity result = PucharseDetEntityFactory.fromRequest(
                request, "CO0001", 2
        );

        assertEquals("CO0001", result.PucharseCod);
        assertEquals(2, result.ItemNumber);
        assertEquals(request.ProductCod, result.ProductCod);
        assertEquals(request.Variant, result.Variant);
        assertEquals(request.NumUnit, result.NumUnit);
        assertEquals(request.NumUnitPrice, result.NumUnitPrice);
        assertEquals(request.NumTotalPrice, result.NumTotalPrice);
        assertEquals(request.ProductUnitName, result.ProductUnitName);
        assertEquals(request.ProductUnitFactor, result.ProductUnitFactor);
        assertNull(result.IsKardexAffected);
    }

    @Test
    void createsAdditionalLotDetail() {
        PucharseDetEntity origin = purchaseDetail();
        PucharseDetEntity lot = new PucharseDetEntity();
        lot.NumUnit = 4;
        lot.NumUnitDelivered = 3;
        lot.LotNumber = "L-001";
        lot.ExpirationDate = new Date(1_800_000_000_000L);

        PucharseDetEntity result = PucharseDetEntityFactory.fromLotDetail(
                origin, lot, 5, false
        );

        assertEquals(origin.PucharseCod, result.PucharseCod);
        assertEquals(5, result.ItemNumber);
        assertEquals(origin.ProductCod, result.ProductCod);
        assertEquals(3, result.NumUnit);
        assertEquals(3, result.NumUnitDelivered);
        assertEquals(new BigDecimal("7.50"), result.NumTotalPrice);
        assertEquals("S", result.IsKardexAffected);
        assertEquals(lot.LotNumber, result.LotNumber);
        assertEquals(lot.ExpirationDate, result.ExpirationDate);
        assertEquals(StatusConst.ACTIVE, result.Status);
    }

    @Test
    void reusesOriginForFirstLotDetail() {
        PucharseDetEntity origin = purchaseDetail();
        PucharseDetEntity lot = new PucharseDetEntity();
        lot.NumUnit = 2;
        lot.LotNumber = "L-002";

        PucharseDetEntity result = PucharseDetEntityFactory.fromLotDetail(
                origin, lot, origin.ItemNumber, true
        );

        assertSame(origin, result);
        assertEquals(2, result.NumUnit);
        assertEquals(2, result.NumUnitDelivered);
        assertEquals(new BigDecimal("5.00"), result.NumTotalPrice);
    }

    @Test
    void createsDeliveryFromReceiptAndLotDetail() {
        PucharseDetEntity detail = purchaseDetail();
        detail.NumUnit = 8;
        detail.NumUnitDelivered = 3;
        detail.LotNumber = "L-003";

        PucharseDetDeliveryEntity receipt =
                PucharseDetDeliveryEntityFactory.fromReceipt(
                        detail, "CO0001", "ALM01", 5
                );
        PucharseDetDeliveryEntity fullReceipt =
                PucharseDetDeliveryEntityFactory.fromFullReceipt(
                        detail, "CO0001", "ALM02"
                );
        PucharseDetDeliveryEntity lotReceipt =
                PucharseDetDeliveryEntityFactory.fromLotDetail(
                        detail, "ALM03"
                );

        assertEquals(5, receipt.NumUnit);
        assertEquals("ALM01", receipt.WarehouseCod);
        assertEquals(detail.ProductCod, receipt.ProductCod);
        assertEquals(detail.LotNumber, receipt.LotNumber);
        assertEquals(8, fullReceipt.NumUnit);
        assertEquals("ALM02", fullReceipt.WarehouseCod);
        assertEquals(3, lotReceipt.NumUnit);
        assertEquals("ALM03", lotReceipt.WarehouseCod);
    }

    @Test
    void mapsSaveRequestOverCurrentDetail() {
        PucharseRequestDetEntity source = requestDetail();
        PucharseRequestDetEntity current = new PucharseRequestDetEntity();
        current.CreationUser = "ORIGINAL";
        current.Status = StatusConst.INACTIVE;

        PucharseRequestDetEntity result =
                PucharseRequestDetEntityFactory.fromSaveRequest(
                        source, current
                );

        assertSame(current, result);
        assertEquals(source.PucharseReqCod, result.PucharseReqCod);
        assertEquals(source.ProductCod, result.ProductCod);
        assertEquals(source.Variant, result.Variant);
        assertEquals(source.NumUnit, result.NumUnit);
        assertEquals(source.NumUnitPrice, result.NumUnitPrice);
        assertEquals(source.NumTotalPrice, result.NumTotalPrice);
        assertEquals(StatusConst.ACTIVE, result.Status);
        assertEquals("ORIGINAL", result.CreationUser);
    }

    @Test
    void createsRequestDetailIdWithDefaultVariant() {
        PucharseRequestDetEntity detail = requestDetail();
        detail.Variant = " ";

        PucharseRequestDetId result =
                PucharseRequestDetIdFactory.fromEntity(detail);

        assertEquals(detail.PucharseReqCod, result.PucharseReqCod);
        assertEquals(detail.ProductCod, result.ProductCod);
        assertEquals("0000", result.Variant);
    }

    @Test
    void assemblesPurchaseDetailDtos() {
        PucharseHeadEntity head = new PucharseHeadEntity();
        List<PucharseDetEntity> details = List.of(new PucharseDetEntity());
        PucharseRequestHeadEntity requestHead =
                new PucharseRequestHeadEntity();
        List<PucharseRequestDetEntity> requestDetails =
                List.of(new PucharseRequestDetEntity());

        PucharseDetailsDto purchase =
                PucharseDetailsDtoFactory.fromEntities(head, details);
        PucharseRequestDetailsDto request =
                PucharseRequestDetailsDtoFactory.fromEntities(
                        requestHead, requestDetails
                );

        assertSame(head, purchase.Headboard);
        assertSame(details, purchase.DetailList);
        assertSame(requestHead, request.Headboard);
        assertSame(requestDetails, request.DetailList);
    }

    private PucharseRequestDetEntity requestDetail() {
        PucharseRequestDetEntity detail = new PucharseRequestDetEntity();
        detail.PucharseReqCod = "SC0001";
        detail.ProductCod = "PROD01";
        detail.Variant = "0000";
        detail.NumUnit = 4;
        detail.NumUnitPrice = new BigDecimal("2.50");
        detail.NumTotalPrice = new BigDecimal("10.00");
        detail.ProductUnitName = "NIU";
        detail.ProductUnitFactor = 1;
        return detail;
    }

    private PucharseDetEntity purchaseDetail() {
        PucharseDetEntity detail = new PucharseDetEntity();
        detail.PucharseCod = "CO0001";
        detail.ItemNumber = 1;
        detail.ProductCod = "PROD01";
        detail.Variant = "0000";
        detail.NumUnitPrice = new BigDecimal("2.50");
        detail.ProductUnitName = "NIU";
        detail.ProductUnitFactor = 1;
        return detail;
    }
}
