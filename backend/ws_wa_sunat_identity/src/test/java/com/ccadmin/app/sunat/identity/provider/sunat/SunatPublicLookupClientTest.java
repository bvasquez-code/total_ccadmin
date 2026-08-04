package com.ccadmin.app.sunat.identity.provider.sunat;

import com.ccadmin.app.sunat.identity.config.SunatIdentityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class SunatPublicLookupClientTest {

    private HttpServer httpServer;
    private SunatPublicLookupClient client;
    private final AtomicReference<String> queryBody = new AtomicReference<>();
    private final AtomicReference<String> queryCookie = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        this.httpServer.createContext(
                "/cl-ti-itmrconsruc/FrameCriterioBusquedaWeb.jsp",
                exchange -> respond(exchange, 200, "text/html; charset=UTF-8", "<html></html>", true)
        );
        this.httpServer.createContext(
                "/cl-ti-itmrconsruc/captcha",
                exchange -> respond(exchange, 200, "application/json; charset=UTF-8", "{\"token\":\"abcde123\"}", false)
        );
        this.httpServer.createContext(
                "/cl-ti-itmrconsruc/jcrS00Alias",
                exchange -> {
                    this.queryBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    this.queryCookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
                    respond(exchange, 200, "text/html; charset=UTF-8", "<html><body>RESULTADO</body></html>", false);
                }
        );
        this.httpServer.start();

        SunatIdentityProperties properties = new SunatIdentityProperties();
        properties.setBaseUrl(URI.create("http://localhost:" + this.httpServer.getAddress().getPort()));
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setRequestTimeout(Duration.ofSeconds(2));
        properties.setUserAgent("SunatIdentityTest/1.0");
        this.client = new SunatPublicLookupClient(properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        this.httpServer.stop(0);
    }

    @Test
    void initializesIsolatedSessionAndSendsRucFormWithToken() throws Exception {
        String html = this.client.findCompanyByRucHtml("20123456789");

        assertTrue(html.contains("RESULTADO"));
        assertTrue(this.queryBody.get().contains("accion=consPorRuc"));
        assertTrue(this.queryBody.get().contains("nroRuc=20123456789"));
        assertTrue(this.queryBody.get().contains("token=abcde123"));
        assertEquals("ITMRSESSION=test", this.queryCookie.get());
    }

    private void respond(
            HttpExchange exchange,
            int status,
            String contentType,
            String body,
            boolean includeSessionCookie
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (includeSessionCookie) {
            exchange.getResponseHeaders().set("Set-Cookie", "ITMRSESSION=test; Path=/");
        }
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
