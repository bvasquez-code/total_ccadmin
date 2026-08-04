package com.ccadmin.app.sunat.identity.service;

import com.ccadmin.app.sunat.identity.exception.SunatIdentityException;
import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.DniIdentityData;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.RelatedTaxpayerDto;
import com.ccadmin.app.sunat.identity.provider.IdentityQueryProvider;
import com.ccadmin.app.sunat.identity.provider.dni.DniIdentityFallbackException;
import com.ccadmin.app.sunat.identity.provider.dni.DniIdentityFallbackProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class SunatIdentitySearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SunatIdentitySearchService.class);
    private static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");
    private static final DateTimeFormatter QUERY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IdentityQueryProvider identityQueryProvider;
    private final DniIdentityFallbackProvider dniIdentityFallbackProvider;

    public SunatIdentitySearchService(
            IdentityQueryProvider identityQueryProvider,
            DniIdentityFallbackProvider dniIdentityFallbackProvider
    ) {
        this.identityQueryProvider = identityQueryProvider;
        this.dniIdentityFallbackProvider = dniIdentityFallbackProvider;
    }

    public CompanyIdentityResponseDto findCompanyByRuc(String ruc) {
        String normalizedRuc = normalizeRuc(ruc);
        return this.identityQueryProvider.findCompanyByRuc(normalizedRuc);
    }

    public PersonIdentityResponseDto findPersonByDocument(
            String documentTypeReference,
            String documentNumber
    ) {
        IdentityDocumentType documentType = IdentityDocumentType.fromReference(documentTypeReference);
        String normalizedDocumentNumber = documentType.normalizeDocumentNumber(documentNumber);

        try {
            PersonIdentityResponseDto primaryResponse = this.identityQueryProvider
                    .findPersonByDocument(documentType, normalizedDocumentNumber);
            if (primaryResponse.found() || documentType != IdentityDocumentType.DNI) {
                return primaryResponse;
            }

            return findDniInFallback(normalizedDocumentNumber)
                    .map(this::toPersonIdentityResponse)
                    .orElse(primaryResponse);
        } catch (SunatIdentityException primaryException) {
            if (documentType != IdentityDocumentType.DNI || Thread.currentThread().isInterrupted()) {
                throw primaryException;
            }

            Optional<DniIdentityData> fallbackResult = findDniInFallback(normalizedDocumentNumber);
            if (fallbackResult.isPresent()) {
                return toPersonIdentityResponse(fallbackResult.get());
            }
            throw primaryException;
        }
    }

    private Optional<DniIdentityData> findDniInFallback(String dni) {
        try {
            return this.dniIdentityFallbackProvider.findByDni(dni);
        } catch (DniIdentityFallbackException exception) {
            if (Thread.currentThread().isInterrupted()) {
                throw exception;
            }
            LOGGER.warn(
                    "La fuente alternativa de DNI no estuvo disponible: {}",
                    exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private PersonIdentityResponseDto toPersonIdentityResponse(DniIdentityData identityData) {
        String legalName = Stream.of(
                        identityData.paternalSurname(),
                        identityData.maternalSurname(),
                        identityData.names()
                )
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse("");

        RelatedTaxpayerDto relatedPerson = new RelatedTaxpayerDto(
                null,
                legalName,
                null,
                null
        );

        return new PersonIdentityResponseDto(
                true,
                "Se encontró una persona asociada al DNI.",
                IdentityDocumentType.DNI.referenceCode(),
                IdentityDocumentType.DNI.displayName(),
                identityData.documentNumber(),
                1,
                List.of(relatedPerson),
                QUERY_DATE_FORMAT.format(ZonedDateTime.now(LIMA_ZONE))
        );
    }

    private String normalizeRuc(String ruc) {
        if (ruc == null || !ruc.trim().matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe contener exactamente 11 dígitos.");
        }
        return ruc.trim();
    }
}
