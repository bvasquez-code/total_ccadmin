package com.ccadmin.app.sunat.identity.provider.dni.eldni;

import com.ccadmin.app.sunat.identity.model.dto.DniIdentityData;
import com.ccadmin.app.sunat.identity.provider.dni.DniIdentityFallbackException;
import com.ccadmin.app.sunat.identity.provider.dni.DniIdentityFallbackProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class ElDniIdentityFallbackProvider implements DniIdentityFallbackProvider {

    private final ElDniLookupClient elDniLookupClient;
    private final ElDniHtmlParser elDniHtmlParser;

    public ElDniIdentityFallbackProvider(
            ElDniLookupClient elDniLookupClient,
            ElDniHtmlParser elDniHtmlParser
    ) {
        this.elDniLookupClient = elDniLookupClient;
        this.elDniHtmlParser = elDniHtmlParser;
    }

    @Override
    public Optional<DniIdentityData> findByDni(String dni) {
        try {
            String html = this.elDniLookupClient.findByDniHtml(dni);
            return this.elDniHtmlParser.parse(dni, html);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DniIdentityFallbackException(
                    "La consulta alternativa del DNI fue interrumpida.",
                    exception
            );
        } catch (IOException exception) {
            throw new DniIdentityFallbackException(
                    "No fue posible comunicarse con la fuente alternativa de DNI.",
                    exception
            );
        }
    }
}
