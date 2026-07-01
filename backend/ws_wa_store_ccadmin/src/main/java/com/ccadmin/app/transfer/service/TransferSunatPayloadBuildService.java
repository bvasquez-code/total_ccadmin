package com.ccadmin.app.transfer.service;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.sale.model.dto.sunat.SunatDocumentLineDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatDocumentTotalsDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatElectronicDocumentDto;
import com.ccadmin.app.sale.model.dto.sunat.SunatPartyDto;
import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.utility.StringUtil;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.constants.TransferConstants;
import com.ccadmin.app.transfer.model.entity.TransferDetEntity;
import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import com.ccadmin.app.transfer.model.entity.TransferHeadEntity;
import com.ccadmin.app.transfer.repository.TransferDetRepository;
import com.ccadmin.app.transfer.repository.TransferDocumentRepository;
import com.ccadmin.app.transfer.repository.TransferHeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransferSunatPayloadBuildService {

    private static final String SUNAT_GUIA_REMISION_REMITENTE = "09";

    @Autowired
    private TransferHeadRepository transferHeadRepository;

    @Autowired
    private TransferDetRepository transferDetRepository;

    @Autowired
    private TransferDocumentRepository transferDocumentRepository;

    @Autowired
    private StoreShared storeShared;

    @Autowired
    private ProductShared productShared;

    public SunatElectronicDocumentDto build(String transferCod) throws Exception {
        if (StringUtil.isEmpty(transferCod)) {
            throw new TransferException("TransferCod requerido para guia SUNAT");
        }

        TransferHeadEntity head = this.transferHeadRepository.findByTransferCodAndTypeOperation(
                transferCod,
                TransferConstants.TYPE_OPERATION_SEND
        );
        if (head == null) {
            throw new TransferException("Transferencia despachada no encontrada para guia SUNAT");
        }

        TransferDocumentEntity document = this.transferDocumentRepository.findByTransferCodAndTypeOperation(
                transferCod,
                TransferConstants.TYPE_OPERATION_SEND
        ).stream().findFirst().orElseThrow(() -> new TransferException("Documento de guia no encontrado para SUNAT"));

        List<TransferDetEntity> detailList = this.transferDetRepository.findByTransferCodAndTypeOperation(
                        transferCod,
                        TransferConstants.TYPE_OPERATION_SEND
                ).stream()
                .filter(detail -> detail.NumUnitDispatch > 0)
                .sorted(Comparator.comparingInt(detail -> detail.ItemNumber))
                .toList();
        if (detailList.isEmpty()) {
            throw new TransferException("Detalle de guia SUNAT no encontrado");
        }

        StoreInfoDto originInfo = this.storeShared.findStoreInfo(head.StoreCodOrigin);
        StoreInfoDto destinationInfo = this.storeShared.findStoreInfo(head.StoreCodDest);
        DocumentNumber documentNumber = parseDocumentNumber(document.DocumentCod);

        SunatElectronicDocumentDto dto = new SunatElectronicDocumentDto();
        dto.SourceModule = "TRANSFER";
        dto.SourceDocumentCod = head.TransferCod;
        dto.SourceDocumentType = "TRANSFER";
        dto.SunatDocumentType = SUNAT_GUIA_REMISION_REMITENTE;
        dto.Series = documentNumber.series;
        dto.Correlative = documentNumber.correlative;
        dto.IssueDate = head.DispatchDate == null ? new Date() : head.DispatchDate;
        dto.CurrencyCod = "PEN";
        dto.Supplier = buildParty(originInfo, true);
        dto.Customer = buildParty(destinationInfo, false);
        dto.Totals = new SunatDocumentTotalsDto();
        dto.ReasonTransferCode = document.ReasonTransferCod;
        dto.ReasonTransferDescription = firstNotBlank(document.ReasonTransferDesc, head.Observation, "TRASLADO ENTRE ESTABLECIMIENTOS");
        dto.TransportModeCode = document.TransportModeCod;
        dto.DepartureUbigeo = document.DepartureUbigeo;
        dto.DepartureAddress = document.DepartureAddress;
        dto.ArrivalUbigeo = document.ArrivalUbigeo;
        dto.ArrivalAddress = document.ArrivalAddress;
        dto.TotalWeightKg = normalizeWeight(document.TotalWeightKg);
        dto.NumPackages = document.NumPackages == null || document.NumPackages <= 0 ? detailList.size() : document.NumPackages;
        dto.CarrierRuc = document.CarrierRuc;
        dto.CarrierName = document.CarrierName;
        dto.VehiclePlate = document.VehiclePlate;
        dto.DriverDocType = normalizeDocumentType(document.DriverDocType);
        dto.DriverDocNumber = document.DriverDocNumber;
        dto.DriverLicenseNumber = document.DriverLicenseNumber;
        dto.Lines = buildLines(detailList);
        return dto;
    }

    private SunatPartyDto buildParty(StoreInfoDto storeInfo, boolean supplier) {
        if (storeInfo == null || storeInfo.Company == null || storeInfo.Store == null) {
            throw new IllegalArgumentException("Datos de local/empresa requeridos para guia SUNAT");
        }
        CompanyEntity company = storeInfo.Company;
        StoreEntity store = storeInfo.Store;
        SunatPartyDto dto = new SunatPartyDto();
        dto.DocumentType = "6";
        dto.DocumentNumber = company.TaxId;
        dto.LegalName = company.LegalName;
        dto.TradeName = company.TradeName;
        dto.Address = supplier
                ? firstNotBlank(company.FiscalAddress, company.Address, store.Address)
                : firstNotBlank(store.Address, company.FiscalAddress, company.Address);
        dto.UbigeoCod = firstNotBlank(company.UbigeoCod, store.UbigeoCod);
        dto.AddressTypeCode = supplier ? normalizeAddressTypeCode(store.SunatAddressTypeCode) : null;
        dto.Department = company.Department;
        dto.Province = company.Province;
        dto.District = company.District;
        dto.CountryCode = company.CountryCode == null ? "PE" : company.CountryCode;
        return dto;
    }

    private List<SunatDocumentLineDto> buildLines(List<TransferDetEntity> detailList) {
        List<String> productCodes = detailList.stream()
                .map(detail -> detail.ProductCod)
                .distinct()
                .toList();
        Map<String, ProductEntity> productByCode = this.productShared.findAllById(productCodes).stream()
                .collect(Collectors.toMap(product -> product.ProductCod, product -> product));

        List<SunatDocumentLineDto> lines = new ArrayList<>();
        for (TransferDetEntity detail : detailList) {
            ProductEntity product = productByCode.get(detail.ProductCod);
            SunatDocumentLineDto line = new SunatDocumentLineDto();
            line.ItemNumber = detail.ItemNumber;
            line.ProductCode = detail.ProductCod;
            line.Description = product == null ? detail.ProductCod : product.ProductName;
            line.UnitCode = normalizeSunatUnitCode(detail.ProductUnitName);
            line.Quantity = BigDecimal.valueOf(detail.NumUnitDispatch);
            lines.add(line);
        }
        return lines;
    }

    private DocumentNumber parseDocumentNumber(String documentCod) {
        if (StringUtil.isEmpty(documentCod) || !documentCod.contains("-")) {
            throw new IllegalArgumentException("Documento de guia invalido para SUNAT");
        }
        String[] parts = documentCod.split("-");
        return new DocumentNumber(parts[0], Integer.parseInt(parts[1]));
    }

    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        }
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private String normalizeSunatUnitCode(String unitCode) {
        if (unitCode == null || unitCode.isBlank()) {
            return "NIU";
        }
        return "NIU".equalsIgnoreCase(unitCode.trim()) ? "NIU" : "BX";
    }

    private String normalizeAddressTypeCode(String value) {
        if (value == null || value.isBlank()) {
            return "0000";
        }
        String code = value.trim();
        if (!code.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("Codigo SUNAT de local anexo invalido: " + value);
        }
        return code;
    }

    private String normalizeDocumentType(String documentType) {
        if (documentType == null) return null;
        return switch (documentType.trim().toUpperCase()) {
            case "DNI", "01", "1" -> "1";
            case "RUC", "06", "6" -> "6";
            case "CE", "04", "4" -> "4";
            case "PAS", "PASAPORTE", "07", "7" -> "7";
            default -> documentType.trim();
        };
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private record DocumentNumber(String series, int correlative) {
    }
}
