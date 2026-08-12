package com.ccadmin.app.sale.service;

import com.ccadmin.app.client.model.entity.ClientEntity;
import com.ccadmin.app.client.shared.ClientShared;
import com.ccadmin.app.person.model.entity.PersonEntity;
import com.ccadmin.app.person.shared.PersonShared;
import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.constants.SaleConstants;
import com.ccadmin.app.sale.model.entity.SaleBillingEntity;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleBillingRepository;
import com.ccadmin.app.sale.repository.SaleDocumentRepository;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class SaleBillingCreateService extends SessionService {

    private final SaleBillingRepository saleBillingRepository;
    private final SaleHeadRepository saleHeadRepository;
    private final SaleDocumentRepository saleDocumentRepository;
    private final ClientShared clientShared;
    private final PersonShared personShared;
    private final SaleSunatPayloadBuildService saleSunatPayloadBuildService;

    public SaleBillingCreateService(
            SaleBillingRepository saleBillingRepository,
            SaleHeadRepository saleHeadRepository,
            SaleDocumentRepository saleDocumentRepository,
            ClientShared clientShared,
            PersonShared personShared,
            SaleSunatPayloadBuildService saleSunatPayloadBuildService
    ) {
        this.saleBillingRepository = saleBillingRepository;
        this.saleHeadRepository = saleHeadRepository;
        this.saleDocumentRepository = saleDocumentRepository;
        this.clientShared = clientShared;
        this.personShared = personShared;
        this.saleSunatPayloadBuildService = saleSunatPayloadBuildService;
    }

    public SaleBillingEntity createForSale(
            SaleHeadEntity saleHead,
            SaleBillingEntity requestedBilling,
            String userCod
    ) throws SaleException {
        if (saleHead == null || isBlank(saleHead.SaleCod)) {
            throw new SaleException("La venta es obligatoria para registrar los datos de facturacion");
        }
        SaleBillingEntity saleBilling = buildBilling(saleHead, requestedBilling, userCod);
        saleBilling.SaleCod = saleHead.SaleCod;
        saleBilling.addSession(userCod);
        return this.saleBillingRepository.save(saleBilling);
    }

    @Transactional(rollbackOn = Exception.class)
    public SaleBillingEntity save(SaleBillingEntity requestedBilling) throws SaleException {
        if (requestedBilling == null || isBlank(requestedBilling.SaleCod)) {
            throw new SaleException("La venta es obligatoria para registrar los datos de facturacion");
        }
        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(requestedBilling.SaleCod)
                .orElseThrow(() -> new SaleException("No existe la venta " + requestedBilling.SaleCod));
        ensureBillingCanChange(saleHead);

        SaleBillingEntity current = this.saleBillingRepository
                .findActiveBySaleCodForUpdate(saleHead.SaleCod)
                .orElse(null);
        SaleBillingEntity saleBilling = buildBilling(saleHead, requestedBilling, getUserCod());
        saleBilling.SaleCod = saleHead.SaleCod;
        if (current != null) {
            saleBilling.CreationUser = current.CreationUser;
            saleBilling.CreationDate = current.CreationDate;
        }
        saleBilling.addSession(getUserCod(), current == null);
        return this.saleBillingRepository.save(saleBilling);
    }

    public SaleBillingEntity prepareForDocument(
            SaleHeadEntity saleHead,
            String requestedDocumentType
    ) throws SaleException {
        SaleBillingEntity saleBilling = this.saleBillingRepository
                .findActiveBySaleCodForUpdate(saleHead.SaleCod)
                .orElse(null);
        if (saleBilling == null) {
            saleBilling = createForSale(saleHead, null, getUserCod());
        }

        if (SaleConstants.DOCUMENT_TYPE_PROFORMA.equals(requestedDocumentType)) {
            return saleBilling;
        }

        String normalizedDocumentType = normalizeFiscalDocumentType(requestedDocumentType);
        if (isBlank(saleBilling.DocumentTypeRequest)) {
            SaleBillingEntity request = new SaleBillingEntity();
            request.SaleCod = saleHead.SaleCod;
            request.DocumentTypeRequest = normalizedDocumentType;
            request.PersonCod = saleBilling.PersonCod;
            request.Person = saleBilling.Person;
            saleBilling = buildBilling(saleHead, request, getUserCod());
            saleBilling.SaleCod = saleHead.SaleCod;
            saleBilling.addSession(getUserCod(), false);
            saleBilling = this.saleBillingRepository.save(saleBilling);
        } else if (!normalizedDocumentType.equals(saleBilling.DocumentTypeRequest)) {
            throw new SaleException(
                    "El documento seleccionado no coincide con el comprobante solicitado para la venta"
            );
        }

        try {
            this.saleSunatPayloadBuildService.validateCustomerForDocument(
                    saleBilling,
                    normalizedDocumentType,
                    saleHead.NumTotalPrice
            );
        } catch (IllegalArgumentException ex) {
            throw new SaleException(ex.getMessage(), ex);
        }
        return saleBilling;
    }

    @Transactional(rollbackOn = Exception.class)
    public SaleBillingEntity synchronizeBuyer(String saleCod) throws SaleException {
        SaleHeadEntity saleHead = this.saleHeadRepository.findByIdForUpdate(saleCod)
                .orElseThrow(() -> new SaleException("No existe la venta " + saleCod));
        ensureBillingCanChange(saleHead);
        SaleBillingEntity current = this.saleBillingRepository.findActiveBySaleCodForUpdate(saleCod)
                .orElse(null);
        if (current != null
                && SaleConstants.DOCUMENT_TYPE_INVOICE.equals(current.DocumentTypeRequest)) {
            return current;
        }
        SaleBillingEntity request = new SaleBillingEntity();
        request.SaleCod = saleCod;
        request.DocumentTypeRequest = current == null ? null : current.DocumentTypeRequest;
        SaleBillingEntity saleBilling = buildBilling(saleHead, request, getUserCod());
        saleBilling.SaleCod = saleCod;
        if (current != null) {
            saleBilling.CreationUser = current.CreationUser;
            saleBilling.CreationDate = current.CreationDate;
        }
        saleBilling.addSession(getUserCod(), current == null);
        return this.saleBillingRepository.save(saleBilling);
    }

    private SaleBillingEntity buildBilling(
            SaleHeadEntity saleHead,
            SaleBillingEntity request,
            String userCod
    ) throws SaleException {
        String documentTypeRequest = request == null
                ? null
                : normalizeOptionalCode(request.DocumentTypeRequest);
        if (documentTypeRequest != null) {
            documentTypeRequest = normalizeFiscalDocumentType(documentTypeRequest);
        }

        PersonEntity person;
        if (SaleConstants.DOCUMENT_TYPE_RECEIPT.equals(documentTypeRequest)
                || documentTypeRequest == null) {
            person = findBuyerPerson(saleHead.ClientCod);
            if (SaleConstants.DOCUMENT_TYPE_RECEIPT.equals(documentTypeRequest)
                    && request != null
                    && !isBlank(request.PersonCod)
                    && (person == null || !request.PersonCod.trim().equals(person.PersonCod))) {
                throw new SaleException("La boleta debe emitirse a la misma persona asociada al comprador");
            }
        } else {
            person = resolveRequestedPerson(request, userCod);
        }

        SaleBillingEntity saleBilling = new SaleBillingEntity();
        saleBilling.DocumentTypeRequest = documentTypeRequest;
        applyPersonSnapshot(saleBilling, person);
        saleBilling.Status = StatusConst.ACTIVE;
        return saleBilling;
    }

    private PersonEntity resolveRequestedPerson(SaleBillingEntity request, String userCod) throws SaleException {
        if (request != null && !isBlank(request.PersonCod)) {
            try {
                PersonEntity existing = this.personShared.findById(request.PersonCod.trim());
                return mergeSelectedIdentity(existing, request.Person);
            } catch (RuntimeException ex) {
                throw new SaleException("No existe la persona de facturacion " + request.PersonCod, ex);
            }
        }
        PersonEntity requestedPerson = request == null ? null : request.Person;
        if (requestedPerson == null || isBlank(requestedPerson.DocumentType)
                || isBlank(requestedPerson.DocumentNum)) {
            throw new SaleException("Debe seleccionar una persona para emitir la factura");
        }
        PersonEntity existing = this.personShared.findByDocumentNum(
                requestedPerson.DocumentType.trim(),
                requestedPerson.DocumentNum.trim()
        );
        if (existing != null) {
            return mergeSelectedIdentity(existing, requestedPerson);
        }
        requestedPerson.PersonCod = requestedPerson.DocumentNum.trim();
        return this.personShared.saveWeb(requestedPerson, userCod);
    }

    private PersonEntity mergeSelectedIdentity(
            PersonEntity existing,
            PersonEntity selectedIdentity
    ) {
        if (existing == null || selectedIdentity == null
                || !sameDocument(existing, selectedIdentity)) {
            return existing;
        }
        PersonEntity snapshot = new PersonEntity();
        snapshot.PersonCod = existing.PersonCod;
        snapshot.PersonType = firstNotBlank(selectedIdentity.PersonType, existing.PersonType);
        snapshot.DocumentType = firstNotBlank(selectedIdentity.DocumentType, existing.DocumentType);
        snapshot.DocumentNum = firstNotBlank(selectedIdentity.DocumentNum, existing.DocumentNum);
        snapshot.Names = firstNotBlank(selectedIdentity.Names, existing.Names);
        snapshot.LastNames = firstNotBlank(selectedIdentity.LastNames, existing.LastNames);
        snapshot.BusinessName = firstNotBlank(selectedIdentity.BusinessName, existing.BusinessName);
        snapshot.CommercialName = firstNotBlank(selectedIdentity.CommercialName, existing.CommercialName);
        snapshot.Address = firstNotBlank(selectedIdentity.Address, existing.Address);
        snapshot.UbigeoCod = firstNotBlank(selectedIdentity.UbigeoCod, existing.UbigeoCod);
        return snapshot;
    }

    private boolean sameDocument(PersonEntity current, PersonEntity selected) {
        return !isBlank(current.DocumentType)
                && !isBlank(current.DocumentNum)
                && current.DocumentType.trim().equals(selected.DocumentType == null
                        ? ""
                        : selected.DocumentType.trim())
                && current.DocumentNum.trim().equals(selected.DocumentNum == null
                        ? ""
                        : selected.DocumentNum.trim());
    }

    private PersonEntity findBuyerPerson(String clientCod) throws SaleException {
        if (isBlank(clientCod)) {
            return null;
        }
        try {
            ClientEntity client = this.clientShared.findById(clientCod.trim());
            return client == null ? null : client.Person;
        } catch (RuntimeException ex) {
            throw new SaleException("No existe el comprador " + clientCod, ex);
        }
    }

    private void applyPersonSnapshot(SaleBillingEntity saleBilling, PersonEntity person) {
        saleBilling.Person = person;
        if (person == null) {
            return;
        }
        saleBilling.PersonCod = normalizeOptionalCode(person.PersonCod);
        saleBilling.DocumentType = normalizeOptionalCode(person.DocumentType);
        saleBilling.DocumentNum = normalizeOptionalCode(person.DocumentNum);
        saleBilling.LegalName = firstNotBlank(
                person.BusinessName,
                fullName(person),
                person.CommercialName
        );
        saleBilling.CommercialName = normalizeOptionalCode(person.CommercialName);
        saleBilling.Address = normalizeOptionalCode(person.Address);
        saleBilling.UbigeoCod = normalizeOptionalCode(person.UbigeoCod);
    }

    private void ensureBillingCanChange(SaleHeadEntity saleHead) throws SaleException {
        if ("S".equals(saleHead.HasFiscalDocument)
                || this.saleDocumentRepository.findFiscalBySaleCod(saleHead.SaleCod) != null) {
            throw new SaleException("Los datos de facturacion no pueden cambiar despues de emitir el comprobante");
        }
    }

    private String normalizeFiscalDocumentType(String value) throws SaleException {
        String documentType = normalizeOptionalCode(value);
        if (!SaleConstants.DOCUMENT_TYPE_INVOICE.equals(documentType)
                && !SaleConstants.DOCUMENT_TYPE_RECEIPT.equals(documentType)) {
            throw new SaleException("Tipo de comprobante solicitado no permitido: " + documentType);
        }
        return documentType;
    }

    private String fullName(PersonEntity person) {
        return firstNotBlank(
                String.join(" ",
                        person.Names == null ? "" : person.Names.trim(),
                        person.LastNames == null ? "" : person.LastNames.trim()
                ).trim()
        );
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value) && !"-".equals(value.trim())) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeOptionalCode(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
