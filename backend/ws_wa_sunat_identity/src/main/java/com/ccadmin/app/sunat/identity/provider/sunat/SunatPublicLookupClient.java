package com.ccadmin.app.sunat.identity.provider.sunat;

import com.ccadmin.app.sunat.identity.config.SunatIdentityProperties;
import com.ccadmin.app.sunat.identity.exception.SunatIdentityException;
import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class SunatPublicLookupClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SunatPublicLookupClient.class);

    private static final String INITIAL_PATH =
            "/cl-ti-itmrconsruc/FrameCriterioBusquedaWeb.jsp";
    private static final String QUERY_PATH =
            "/cl-ti-itmrconsruc/jcrS00Alias";
    private static final String PRIMARY_TOKEN_PATH =
            "/cl-ti-itmrconsruc/captcha?accion=random";
    private static final String ALTERNATIVE_TOKEN_PATH =
            "/cl-ti-itmrconsmulruc/captcha?accion=random";

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{5,300}$");
    private static final Pattern CHARSET_PATTERN = Pattern.compile(
            "(?i)charset\\s*=\\s*[\"']?([^;\"']+)"
    );

    private final SunatIdentityProperties sunatIdentityProperties;
    private final ObjectMapper objectMapper;

    public SunatPublicLookupClient(
            SunatIdentityProperties sunatIdentityProperties,
            ObjectMapper objectMapper
    ) {
        this.sunatIdentityProperties = sunatIdentityProperties;
        this.objectMapper = objectMapper;
    }

    public String findCompanyByRucHtml(String ruc) throws IOException, InterruptedException {
        Map<String, String> form = baseForm();
        form.put("accion", "consPorRuc");
        form.put("nroRuc", ruc);
        form.put("rbtnTipo", "1");
        form.put("search1", ruc);
        form.put("tipdoc", "1");
        return executeQuery(form, "consultar el RUC");
    }

    public String findPersonByDocumentHtml(
            IdentityDocumentType documentType,
            String documentNumber
    ) throws IOException, InterruptedException {
        Map<String, String> form = baseForm();
        form.put("accion", "consPorTipdoc");
        form.put("nrodoc", documentNumber);
        form.put("rbtnTipo", "2");
        form.put("tipdoc", documentType.sunatCode());
        form.put("search2", documentNumber);
        return executeQuery(form, "consultar el documento");
    }

    private String executeQuery(
            Map<String, String> form,
            String operation
    ) throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= 2; attempt++) {
            HttpClient httpClient = createSessionClient();
            initializeSession(httpClient);
            String token = obtainToken(httpClient);

            Map<String, String> requestForm = new LinkedHashMap<>(form);
            requestForm.put("token", token);

            HttpResponse<byte[]> response = sendQuery(httpClient, requestForm);
            validateHttpResponse(response, operation);
            String html = decodeResponse(response);

            if (attempt == 1 && isRejectedTokenPage(html)) {
                LOGGER.warn("SUNAT rechazó el primer token de consulta; se renovará la sesión una vez.");
                continue;
            }

            return html;
        }

        throw new SunatIdentityException("SUNAT rechazó los tokens generados para la consulta.");
    }

    private Map<String, String> baseForm() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("accion", "");
        form.put("razSoc", "");
        form.put("nroRuc", "");
        form.put("nrodoc", "");
        form.put("token", "");
        form.put("contexto", "ti-it");
        form.put("modo", "1");
        form.put("rbtnTipo", "");
        form.put("search1", "");
        form.put("tipdoc", "");
        form.put("search2", "");
        form.put("search3", "");
        form.put("codigo", "");
        return form;
    }

    private HttpClient createSessionClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(this.sunatIdentityProperties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    private void initializeSession(HttpClient httpClient) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.sunatIdentityProperties.resolve(INITIAL_PATH))
                .timeout(this.sunatIdentityProperties.getRequestTimeout())
                .header("User-Agent", userAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-PE,es;q=0.9")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );
        validateHttpResponse(response, "inicializar la sesión de SUNAT");
    }

    private String obtainToken(HttpClient httpClient) throws IOException, InterruptedException {
        Optional<String> primaryToken = requestToken(
                httpClient,
                this.sunatIdentityProperties.resolve(PRIMARY_TOKEN_PATH)
        );
        if (primaryToken.isPresent()) {
            return primaryToken.get();
        }

        Optional<String> alternativeToken = requestToken(
                httpClient,
                this.sunatIdentityProperties.resolve(ALTERNATIVE_TOKEN_PATH)
        );
        return alternativeToken.orElseThrow(
                () -> new SunatIdentityException("SUNAT no devolvió un token válido.")
        );
    }

    private Optional<String> requestToken(
            HttpClient httpClient,
            URI endpoint
    ) throws IOException, InterruptedException {
        URI endpointWithoutCache = URI.create(endpoint + "&_=" + System.currentTimeMillis());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpointWithoutCache)
                .timeout(this.sunatIdentityProperties.getRequestTimeout())
                .header("User-Agent", userAgent())
                .header("Accept", "text/plain,application/json,*/*;q=0.8")
                .header("Accept-Language", "es-PE,es;q=0.9")
                .header("Referer", this.sunatIdentityProperties.resolve(INITIAL_PATH).toString())
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return Optional.empty();
        }

        return extractToken(decodeResponse(response));
    }

    private Optional<String> extractToken(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }

        String response = content.trim();
        String lowerCaseResponse = response.toLowerCase(Locale.ROOT);
        if (lowerCaseResponse.startsWith("<!doctype")
                || lowerCaseResponse.startsWith("<html")
                || lowerCaseResponse.contains("<body")
                || lowerCaseResponse.contains("<head")) {
            return Optional.empty();
        }

        if (response.startsWith("{") || response.startsWith("[")) {
            Optional<String> jsonToken = extractTokenFromJson(response);
            if (jsonToken.isPresent()) {
                return jsonToken;
            }
        }

        if (response.length() >= 2 && response.startsWith("\"") && response.endsWith("\"")) {
            response = response.substring(1, response.length() - 1).trim();
        }

        return isValidToken(response) ? Optional.of(response) : Optional.empty();
    }

    private Optional<String> extractTokenFromJson(String json) {
        try {
            JsonNode node = this.objectMapper.readTree(json);
            if (node.isTextual()) {
                String token = node.asText().trim();
                return isValidToken(token) ? Optional.of(token) : Optional.empty();
            }

            for (String field : new String[]{"token", "codigo", "code", "numRnd", "random"}) {
                JsonNode value = node.get(field);
                if (value != null && !value.isNull()) {
                    String token = value.asText().trim();
                    if (isValidToken(token)) {
                        return Optional.of(token);
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.debug("SUNAT devolvió un token con formato JSON no reconocido.", exception);
        }
        return Optional.empty();
    }

    private boolean isValidToken(String token) {
        return token != null && TOKEN_PATTERN.matcher(token).matches();
    }

    private HttpResponse<byte[]> sendQuery(
            HttpClient httpClient,
            Map<String, String> form
    ) throws IOException, InterruptedException {
        String body = form.entrySet()
                .stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.sunatIdentityProperties.resolve(QUERY_PATH))
                .timeout(this.sunatIdentityProperties.getRequestTimeout())
                .header("User-Agent", userAgent())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-PE,es;q=0.9")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Origin", origin())
                .header("Referer", this.sunatIdentityProperties.resolve(INITIAL_PATH).toString())
                .header("Cache-Control", "max-age=0")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String decodeResponse(HttpResponse<byte[]> response) {
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        Matcher matcher = CHARSET_PATTERN.matcher(contentType);
        if (matcher.find()) {
            try {
                return new String(response.body(), Charset.forName(matcher.group(1).trim()));
            } catch (Exception exception) {
                LOGGER.debug("SUNAT devolvió un charset no reconocido.", exception);
            }
        }
        return new String(response.body(), StandardCharsets.ISO_8859_1);
    }

    private void validateHttpResponse(HttpResponse<?> response, String operation) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }

        String detail = switch (status) {
            case 403 -> "SUNAT rechazó la solicitud.";
            case 404 -> "No se encontró el endpoint de SUNAT.";
            case 429 -> "Se realizaron demasiadas consultas.";
            case 500 -> "SUNAT presentó un error interno.";
            case 503 -> "El servicio de SUNAT no está disponible.";
            default -> "Respuesta HTTP inesperada.";
        };

        throw new SunatIdentityException(
                "No fue posible " + operation + ". HTTP " + status + ". " + detail
        );
    }

    private boolean isRejectedTokenPage(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }

        String pageText = Jsoup.parse(html).text();
        String normalized = Normalizer.normalize(pageText, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.contains("ingrese el codigo mostrado");
    }

    private String userAgent() {
        String configuredUserAgent = this.sunatIdentityProperties.getUserAgent();
        if (configuredUserAgent == null || configuredUserAgent.isBlank()) {
            throw new IllegalStateException("El user-agent para SUNAT es obligatorio.");
        }
        return configuredUserAgent;
    }

    private String origin() {
        URI baseUrl = this.sunatIdentityProperties.getBaseUrl();
        if (baseUrl == null) {
            throw new IllegalStateException("La URL base de SUNAT es obligatoria.");
        }

        int port = baseUrl.getPort();
        return baseUrl.getScheme() + "://" + baseUrl.getHost() + (port < 0 ? "" : ":" + port);
    }
}
