package com.ccadmin.app.sunat.identity.provider.dni.eldni;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElDniLookupClientTest {

    private HttpServer httpServer;
    private ElDniLookupClient client;
    private final AtomicReference<String> queryBody = new AtomicReference<>();
    private final AtomicReference<String> queryCookie = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        this.httpServer.createContext("/buscar", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(
                        exchange,
                        "<html><input type='hidden' name='_token' value='csrf-test'></html>",
                        true
                );
                return;
            }

            this.queryBody.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            ));
            this.queryCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            respond(exchange, "<html><body>RESULTADO</body></html>", false);
        });
        this.httpServer.start();

        ElDniIdentityProperties properties = new ElDniIdentityProperties();
        properties.setQueryUrl(URI.create(
                "http://localhost:" + this.httpServer.getAddress().getPort() + "/buscar"
        ));
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setRequestTimeout(Duration.ofSeconds(2));
        properties.setUserAgent("ElDniIdentityTest/1.0");
        this.client = new ElDniLookupClient(properties);
    }

    @AfterEach
    void tearDown() {
        this.httpServer.stop(0);
    }

    @Test
    void keepsSessionAndSendsCsrfTokenAndDniAsMultipart() throws Exception {
        String html = this.client.findByDniHtml("12345678");

        assertTrue(html.contains("RESULTADO"));
        assertTrue(this.queryBody.get().contains("name=\"_token\"\r\n\r\ncsrf-test"));
        assertTrue(this.queryBody.get().contains("name=\"dni\"\r\n\r\n12345678"));
        assertEquals("ELDNISESSION=test", this.queryCookie.get());
    }

    private void respond(HttpExchange exchange, String body, boolean includeCookie)
            throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        if (includeCookie) {
            exchange.getResponseHeaders().set("Set-Cookie", "ELDNISESSION=test; Path=/");
        }
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
