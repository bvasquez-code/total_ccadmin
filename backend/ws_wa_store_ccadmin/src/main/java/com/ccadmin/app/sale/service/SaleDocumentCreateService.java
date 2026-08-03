package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.dto.SaleDetailDto;
import com.ccadmin.app.sale.model.dto.SaleDocumentIssueDto;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.model.constants.BusinessConfigConstants;
import com.ccadmin.app.shared.service.GenericQueuedService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.system.shared.CounterfoilShared;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;

@Service
public class SaleDocumentCreateService extends SessionService {

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    @Autowired
    private SaleDocumentRepository saleDocumentRepository;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private CounterfoilShared counterfoilShared;
    @Autowired
    private ClientShared clientShared;
    @Autowired
    private GenericQueuedService genericQueuedService;
    @Autowired
    private SaleSunatEmissionService saleSunatEmissionService;
    @Autowired
    private SaleSunatPayloadBuildService saleSunatPayloadBuildService;
    @Autowired
    private CatalogSearchShared catalogSearchShared;

    /**
     * Nucleo unico para crear cualquier documento de venta. El llamador debe
     * mantener bloqueada la cabecera de la venta dentro de su transaccion.
     */
    public SaleDocumentEntity createDocument(
            SaleHeadEntity saleHead,
            String requestedDocumentType
    ) throws SaleException {
        if (saleHead == null || saleHead.SaleCod == null || saleHead.SaleCod.isBlank()) {
            throw new SaleException("La venta es obligatoria para generar el documento");
        }

        String documentType = normalizeDocumentType(requestedDocumentType);
        if (SaleConstants.DOCUMENT_TYPE_PROFORMA.equals(documentType)
                && !this.catalogSearchShared.isIndicatorSystemEnabled(
                        BusinessConfigConstants.ConfigCod.IND_PROFORMA_SALES
                )) {
            throw new SaleException("La emision de proformas no esta habilitada para esta empresa");
        }
        String documentRole = resolveDocumentRole(documentType);
        if (this.saleDocumentRepository.countBySaleCodAndDocumentRoleAndStatus(
                saleHead.SaleCod,
                documentRole,
                StatusConst.ACTIVE
        ) > 0) {
            throw new SaleException(existingDocumentMessage(documentRole));
        }

        if (SaleConstants.DOCUMENT_ROLE_FISCAL.equals(documentRole)) {
            this.validateFiscalClient(saleHead, documentType);
        }

        SaleDocumentEntity document;
        try {
            document = this.counterfoilShared.generateDocumentSale(
                    saleHead.StoreCod,
                    documentType,
                    saleHead.SaleCod
            );
        } catch (RuntimeException ex) {
            throw new SaleException(ex.getMessage(), ex);
        }
        document.DocumentType = documentType;
        document.DocumentRole = documentRole;
        document.ClientCod = normalizeOptionalCode(saleHead.ClientCod);
        document.IssueDate = new Date();
        document.addSession(getUserCod());

        if (SaleConstants.DOCUMENT_ROLE_FISCAL.equals(documentRole)) {
            saleHead.HasFiscalDocument = "S";
        } else if (saleHead.HasFiscalDocument == null || saleHead.HasFiscalDocument.isBlank()) {
            saleHead.HasFiscalDocument = "N";
        }

        return this.saleDocumentRepository.save(document);
    }

    @Transactional(rollbackOn = Exception.class)
    public SaleDetailDto issueFiscalDocument(SaleDocumentIssueDto request) throws SaleException {
        if (request == null || request.SaleCod == null || request.SaleCod.isBlank()) {
            throw new SaleException("El codigo de venta es obligatorio");
        }

        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(request.SaleCod)
                .orElseThrow(() -> new SaleException("No existe la venta " + request.SaleCod));

        if (!SaleConstants.CONFIRMED.equals(saleHead.SaleStatus)) {
            throw new SaleException("Solo se puede facturar una venta confirmada");
        }
        if (!"S".equals(saleHead.IsPaid)) {
            throw new SaleException("La venta debe estar completamente pagada antes de facturarla");
        }
        if ("S".equals(saleHead.HasFiscalDocument)
                || this.saleDocumentRepository.findFiscalBySaleCod(saleHead.SaleCod) != null) {
            throw new SaleException("La venta ya tiene una boleta o factura emitida");
        }
        if (this.saleDocumentRepository.findProformaBySaleCod(saleHead.SaleCod) == null) {
            throw new SaleException("La venta no tiene una proforma activa para convertir");
        }

        String requestedClientCod = normalizeOptionalCode(request.ClientCod);
        if (requestedClientCod != null) {
            saleHead.ClientCod = requestedClientCod;
        }

        SaleDocumentEntity document = this.createDocument(saleHead, request.DocumentType);
        saleHead.addSession(getUserCod());
        this.saleHeadRepository.save(saleHead);
        this.emitSunatAfterCommit(saleHead.SaleCod, document.DocumentCod);

        return this.saleSearchService.findById(saleHead.SaleCod);
    }

    public void emitSunatAfterCommit(String saleCod, String documentCod) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    queueSunatEmission(saleCod, documentCod);
                }
            });
            return;
        }
        queueSunatEmission(saleCod, documentCod);
    }

    private void queueSunatEmission(String saleCod, String documentCod) {
        this.genericQueuedService.addQueued(new SaleSunatEmissionTaskService(
                this.saleSunatEmissionService,
                saleCod,
                documentCod
        ));
    }

    private String normalizeDocumentType(String requestedDocumentType) throws SaleException {
        String documentType = requestedDocumentType == null ? "" : requestedDocumentType.trim();
        if (!SaleConstants.DOCUMENT_TYPE_PROFORMA.equals(documentType)
                && !SaleConstants.DOCUMENT_TYPE_INVOICE.equals(documentType)
                && !SaleConstants.DOCUMENT_TYPE_RECEIPT.equals(documentType)) {
            throw new SaleException("Tipo de documento de venta no permitido: " + documentType);
        }
        return documentType;
    }

    private String resolveDocumentRole(String documentType) {
        return SaleConstants.DOCUMENT_TYPE_PROFORMA.equals(documentType)
                ? SaleConstants.DOCUMENT_ROLE_INTERNAL
                : SaleConstants.DOCUMENT_ROLE_FISCAL;
    }

    private String existingDocumentMessage(String documentRole) {
        return SaleConstants.DOCUMENT_ROLE_FISCAL.equals(documentRole)
                ? "La venta ya tiene una boleta o factura emitida"
                : "La venta ya tiene una proforma emitida";
    }

    private void validateFiscalClient(SaleHeadEntity saleHead, String documentType) throws SaleException {
        ClientEntity client = findClient(saleHead.ClientCod);
        try {
            this.saleSunatPayloadBuildService.validateCustomerForDocument(
                    client,
                    documentType,
                    saleHead.NumTotalPrice
            );
        } catch (IllegalArgumentException ex) {
            throw new SaleException(ex.getMessage(), ex);
        }
    }

    private ClientEntity findClient(String clientCod) throws SaleException {
        String normalizedClientCod = normalizeOptionalCode(clientCod);
        if (normalizedClientCod == null) {
            return null;
        }
        try {
            return this.clientShared.findById(normalizedClientCod);
        } catch (RuntimeException ex) {
            throw new SaleException("No existe el cliente " + normalizedClientCod, ex);
        }
    }

    private String normalizeOptionalCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
