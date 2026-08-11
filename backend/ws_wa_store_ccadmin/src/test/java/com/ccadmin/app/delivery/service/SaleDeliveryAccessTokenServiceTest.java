package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.SaleDeliveryAccessTokenPayloadDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaleDeliveryAccessTokenServiceTest {

    private static final String AES_256_KEY =
            "w3DkJspH5VRSP8YhKQVcRfvuzMZ/TwKVXHcBJeDFO6Y=";
    private static final long EXPIRATION_MILLISECONDS = 60_000L;
    private static final Instant ISSUED_AT = Instant.parse("2026-08-11T16:00:00Z");

    @Test
    void springCreatesTheServiceUsingTheConfiguredConstructor() {
        new ApplicationContextRunner()
                .withBean(ObjectMapper.class)
                .withBean(SaleDeliveryAccessTokenService.class)
                .withPropertyValues(
                        "delivery.sale-access-token.key=" + AES_256_KEY,
                        "delivery.sale-access-token.expiration-milliseconds="
                                + EXPIRATION_MILLISECONDS
                )
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertEquals(
                            1,
                            context.getBeansOfType(SaleDeliveryAccessTokenService.class).size()
                    );
                });
    }

    @Test
    void encryptsAndResolvesOrderDataWithoutExposingTheSaleCode() {
        SaleDeliveryAccessTokenService service = serviceAt(ISSUED_AT);

        String firstToken = service.issue("ST00100010000270", "CL001");
        String secondToken = service.issue("ST00100010000270", "CL001");
        SaleDeliveryAccessTokenPayloadDto payload = service.resolve(firstToken, "CL001");

        assertTrue(firstToken.startsWith("v1."));
        assertFalse(firstToken.contains("ST00100010000270"));
        assertNotEquals(firstToken, secondToken);
        assertEquals("ST00100010000270", payload.SaleCod);
        assertEquals("CL001", payload.ClientCod);
        assertEquals("WEB_CHECKOUT", payload.Purpose);
    }

    @Test
    void rejectsAnyTokenManipulation() {
        SaleDeliveryAccessTokenService service = serviceAt(ISSUED_AT);
        String token = service.issue("ST00100010000270", "CL001");
        char replacement = token.endsWith("A") ? 'B' : 'A';
        String manipulatedToken = token.substring(0, token.length() - 1) + replacement;

        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolve(manipulatedToken, "CL001")
        );
    }

    @Test
    void rejectsTokenForAnotherAuthenticatedClient() {
        SaleDeliveryAccessTokenService service = serviceAt(ISSUED_AT);
        String token = service.issue("ST00100010000270", "CL001");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolve(token, "CL999")
        );
    }

    @Test
    void rejectsExpiredToken() {
        String token = serviceAt(ISSUED_AT).issue("ST00100010000270", "CL001");
        SaleDeliveryAccessTokenService expiredTokenResolver = serviceAt(
                ISSUED_AT.plusMillis(EXPIRATION_MILLISECONDS + 1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> expiredTokenResolver.resolve(token, "CL001")
        );
    }

    private SaleDeliveryAccessTokenService serviceAt(Instant instant) {
        return new SaleDeliveryAccessTokenService(
                new ObjectMapper(),
                AES_256_KEY,
                EXPIRATION_MILLISECONDS,
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }
}
