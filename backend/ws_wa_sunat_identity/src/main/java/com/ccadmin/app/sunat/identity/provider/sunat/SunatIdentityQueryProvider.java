package com.ccadmin.app.sunat.identity.provider.sunat;

import com.ccadmin.app.sunat.identity.exception.SunatIdentityException;
import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.sunat.identity.provider.IdentityQueryProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@ConditionalOnProperty(
        prefix = "identity.query",
        name = "provider",
        havingValue = "sunat",
        matchIfMissing = true
)
public class SunatIdentityQueryProvider implements IdentityQueryProvider {

    private final SunatPublicLookupClient sunatPublicLookupClient;
    private final SunatRucHtmlParser sunatRucHtmlParser;
    private final SunatPersonDocumentHtmlParser sunatPersonDocumentHtmlParser;

    public SunatIdentityQueryProvider(
            SunatPublicLookupClient sunatPublicLookupClient,
            SunatRucHtmlParser sunatRucHtmlParser,
            SunatPersonDocumentHtmlParser sunatPersonDocumentHtmlParser
    ) {
        this.sunatPublicLookupClient = sunatPublicLookupClient;
        this.sunatRucHtmlParser = sunatRucHtmlParser;
        this.sunatPersonDocumentHtmlParser = sunatPersonDocumentHtmlParser;
    }

    @Override
    public CompanyIdentityResponseDto findCompanyByRuc(String ruc) {
        try {
            String html = this.sunatPublicLookupClient.findCompanyByRucHtml(ruc);
            return this.sunatRucHtmlParser.parse(ruc, html);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SunatIdentityException("La consulta del RUC fue interrumpida.", exception);
        } catch (IOException exception) {
            throw new SunatIdentityException("No fue posible comunicarse con SUNAT para consultar el RUC.", exception);
        }
    }

    @Override
    public PersonIdentityResponseDto findPersonByDocument(
            IdentityDocumentType documentType,
            String documentNumber
    ) {
        try {
            String html = this.sunatPublicLookupClient.findPersonByDocumentHtml(
                    documentType,
                    documentNumber
            );
            return this.sunatPersonDocumentHtmlParser.parse(documentType, documentNumber, html);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SunatIdentityException("La consulta del documento fue interrumpida.", exception);
        } catch (IOException exception) {
            throw new SunatIdentityException(
                    "No fue posible comunicarse con SUNAT para consultar el documento.",
                    exception
            );
        }
    }
}
