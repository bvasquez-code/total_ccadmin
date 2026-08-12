package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.AddressGeocodingResultDto;
import com.ccadmin.app.shared.model.entity.CountryEntity;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AddressGeocodingSearchService {

    private static final int MAXIMUM_CACHE_ENTRIES = 200;
    private static final int MAXIMUM_RESULTS = 5;
    private static final long MINIMUM_PROVIDER_INTERVAL_MILLISECONDS = 1_000L;

    private final ObjectMapper objectMapper;
    private final LocationRepository locationRepository;
    private final HttpClient httpClient;
    private final String providerUrl;
    private final String providerUserAgent;
    private final Object providerRateLock = new Object();
    private final Map<String, List<AddressGeocodingResultDto>> resultCache =
            new LinkedHashMap<>(MAXIMUM_CACHE_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, List<AddressGeocodingResultDto>> eldest
                ) {
                    return size() > MAXIMUM_CACHE_ENTRIES;
                }
            };
    private long lastProviderRequestTimestamp;

    @Autowired
    public AddressGeocodingSearchService(
            ObjectMapper objectMapper,
            LocationRepository locationRepository,
            @Value("${delivery.geocoding.url:https://nominatim.openstreetmap.org/search}")
            String providerUrl,
            @Value("${delivery.geocoding.user-agent:CcAdmin-Ecommerce/1.0}")
            String providerUserAgent
    ) {
        this(
                objectMapper,
                locationRepository,
                providerUrl,
                providerUserAgent,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build()
        );
    }

    AddressGeocodingSearchService(
            ObjectMapper objectMapper,
            LocationRepository locationRepository,
            String providerUrl,
            String providerUserAgent,
            HttpClient httpClient
    ) {
        this.objectMapper = objectMapper;
        this.locationRepository = locationRepository;
        this.providerUrl = providerUrl;
        this.providerUserAgent = providerUserAgent;
        this.httpClient = httpClient;
    }

    public List<AddressGeocodingResultDto> search(String query, String countryCod) {
        String normalizedQuery = validateAndNormalizeQuery(query);
        CountryEntity country = findCountry(countryCod);
        String countryIso2 = validateCountryIso2(country);
        String cacheKey = countryIso2 + '|' + normalizedQuery.toUpperCase(Locale.ROOT);

        synchronized (resultCache) {
            List<AddressGeocodingResultDto> cachedResult = resultCache.get(cacheKey);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        List<AddressGeocodingResultDto> result = searchProvider(normalizedQuery, countryIso2);
        synchronized (resultCache) {
            resultCache.put(cacheKey, result);
        }
        return result;
    }

    private List<AddressGeocodingResultDto> searchProvider(
            String query,
            String countryIso2
    ) {
        waitForProviderCapacity();
        try {
            String requestUrl = providerUrl
                    + "?format=jsonv2&addressdetails=1&limit=" + MAXIMUM_RESULTS
                    + "&q=" + encode(query)
                    + "&countrycodes=" + encode(countryIso2.toLowerCase(Locale.ROOT));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", providerUserAgent)
                    .header("Accept-Language", "es")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("El buscador de direcciones no respondió correctamente");
            }
            return parseResults(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La búsqueda de la dirección fue interrumpida", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException(
                    "No se pudo consultar el buscador de direcciones. Marca el punto manualmente",
                    ex
            );
        }
    }

    private List<AddressGeocodingResultDto> parseResults(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (!root.isArray()) {
            throw new IllegalStateException("El buscador de direcciones devolvió una respuesta inválida");
        }

        List<AddressGeocodingResultDto> resultList = new ArrayList<>();
        for (JsonNode item : root) {
            if (resultList.size() >= MAXIMUM_RESULTS
                    || !item.hasNonNull("display_name")
                    || !item.hasNonNull("lat")
                    || !item.hasNonNull("lon")) {
                continue;
            }
            try {
                AddressGeocodingResultDto result = new AddressGeocodingResultDto();
                result.DisplayName = item.path("display_name").asText();
                result.PostalCode = item.path("address").path("postcode").asText("");
                result.Latitude = new BigDecimal(item.path("lat").asText());
                result.Longitude = new BigDecimal(item.path("lon").asText());
                if (result.Latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                        || result.Latitude.compareTo(BigDecimal.valueOf(90)) > 0
                        || result.Longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                        || result.Longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
                    continue;
                }
                resultList.add(result);
            } catch (NumberFormatException ignored) {
                // Una coincidencia sin coordenadas numericas no debe impedir mostrar las demas.
            }
        }
        return List.copyOf(resultList);
    }

    private void waitForProviderCapacity() {
        synchronized (providerRateLock) {
            long elapsed = System.currentTimeMillis() - lastProviderRequestTimestamp;
            long waitMilliseconds = MINIMUM_PROVIDER_INTERVAL_MILLISECONDS - elapsed;
            if (waitMilliseconds > 0) {
                try {
                    Thread.sleep(waitMilliseconds);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("La búsqueda de la dirección fue interrumpida", ex);
                }
            }
            lastProviderRequestTimestamp = System.currentTimeMillis();
        }
    }

    private CountryEntity findCountry(String countryCod) {
        if (countryCod == null || !countryCod.matches("^[A-Za-z]{3}$")) {
            throw new IllegalArgumentException("Selecciona un país válido para buscar la dirección");
        }
        return locationRepository.findActiveCountryByCode(countryCod.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException(
                        "El país seleccionado no está disponible"
                ));
    }

    private String validateCountryIso2(CountryEntity country) {
        if (country.CountryIso2 == null || !country.CountryIso2.matches("^[A-Za-z]{2}$")) {
            throw new IllegalArgumentException(
                    "El país seleccionado no tiene configurado su código ISO2"
            );
        }
        return country.CountryIso2;
    }

    private String validateAndNormalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Escribe la dirección que deseas buscar");
        }
        String normalizedQuery = query.trim().replaceAll("\\s+", " ");
        if (normalizedQuery.length() < 3 || normalizedQuery.length() > 512) {
            throw new IllegalArgumentException("La búsqueda debe tener entre 3 y 512 caracteres");
        }
        return normalizedQuery;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
