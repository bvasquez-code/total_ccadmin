package com.ccadmin.app.sunat.identity.provider.dni.eldni;

import com.ccadmin.app.sunat.identity.provider.dni.DniIdentityFallbackException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ElDniLookupClient {

    private static final String ACCEPT_HEADER =
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

    private final ElDniIdentityProperties elDniIdentityProperties;

    public ElDniLookupClient(ElDniIdentityProperties elDniIdentityProperties) {
        this.elDniIdentityProperties = elDniIdentityProperties;
    }

    public String findByDniHtml(String dni) throws IOException, InterruptedException {
        HttpClient httpClient = createSessionClient();
        HttpResponse<String> initialResponse = executeInitialRequest(httpClient);
        validateHttpResponse(initialResponse, "obtener la página de consulta alternativa de DNI");

        String csrfToken = extractCsrfToken(initialResponse.body());
        String boundary = "----JavaFormBoundary" + UUID.randomUUID().toString().replace("-", "");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("_token", csrfToken);
        fields.put("dni", dni);

        HttpRequest queryRequest = HttpRequest.newBuilder()
                .uri(this.elDniIdentityProperties.getQueryUrl())
                .timeout(this.elDniIdentityProperties.getRequestTimeout())
                .header("User-Agent", this.elDniIdentityProperties.getUserAgent())
                .header("Accept", ACCEPT_HEADER)
                .header("Accept-Language", "es-PE,es;q=0.9")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Origin", this.elDniIdentityProperties.origin())
                .header("Referer", this.elDniIdentityProperties.getQueryUrl().toString())
                .POST(HttpRequest.BodyPublishers.ofByteArray(buildMultipart(boundary, fields)))
                .build();

        HttpResponse<String> queryResponse = httpClient.send(
                queryRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        validateHttpResponse(queryResponse, "consultar el DNI en la fuente alternativa");
        return queryResponse.body();
    }

    private HttpClient createSessionClient() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(this.elDniIdentityProperties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    private HttpResponse<String> executeInitialRequest(HttpClient httpClient)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(this.elDniIdentityProperties.getQueryUrl())
                .timeout(this.elDniIdentityProperties.getRequestTimeout())
                .header("User-Agent", this.elDniIdentityProperties.getUserAgent())
                .header("Accept", ACCEPT_HEADER)
                .header("Accept-Language", "es-PE,es;q=0.9")
                .GET()
                .build();

        return httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private String extractCsrfToken(String html) {
        Element tokenInput = Jsoup.parse(
                html == null ? "" : html,
                this.elDniIdentityProperties.getQueryUrl().toString()
        ).selectFirst("input[name=_token]");

        if (tokenInput == null || tokenInput.attr("value").isBlank()) {
            throw new DniIdentityFallbackException(
                    "La fuente alternativa de DNI no devolvió un token CSRF válido."
            );
        }
        return tokenInput.attr("value").trim();
    }

    private byte[] buildMultipart(String boundary, Map<String, String> fields) throws IOException {
        String lineBreak = "\r\n";
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                write(output, "--" + boundary + lineBreak);
                write(
                        output,
                        "Content-Disposition: form-data; name=\""
                                + escapeFieldName(field.getKey())
                                + "\""
                                + lineBreak
                );
                write(output, lineBreak);
                write(output, field.getValue());
                write(output, lineBreak);
            }
            write(output, "--" + boundary + "--" + lineBreak);
            return output.toByteArray();
        }
    }

    private void write(ByteArrayOutputStream output, String content) throws IOException {
        output.write(content.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeFieldName(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void validateHttpResponse(HttpResponse<?> response, String operation) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }

        String detail = switch (status) {
            case 403 -> "La fuente alternativa rechazó la solicitud.";
            case 419 -> "La sesión o el token CSRF de la fuente alternativa expiraron.";
            case 429 -> "Se realizaron demasiadas consultas a la fuente alternativa.";
            case 503 -> "La fuente alternativa de DNI no está disponible.";
            default -> "La fuente alternativa devolvió una respuesta HTTP inesperada.";
        };
        throw new DniIdentityFallbackException(
                "No fue posible " + operation + ". HTTP " + status + ". " + detail
        );
    }
}
