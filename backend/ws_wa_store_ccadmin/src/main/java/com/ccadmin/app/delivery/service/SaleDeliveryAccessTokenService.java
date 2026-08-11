package com.ccadmin.app.delivery.service;

import com.ccadmin.app.delivery.model.dto.SaleDeliveryAccessTokenPayloadDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;

@Service
public class SaleDeliveryAccessTokenService {

    private static final String TOKEN_VERSION = "v1";
    private static final int PAYLOAD_VERSION = 1;
    private static final String TOKEN_PURPOSE = "WEB_CHECKOUT";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int INITIALIZATION_VECTOR_LENGTH = 12;
    private static final int AUTHENTICATION_TAG_LENGTH_BITS = 128;
    private static final int AES_256_KEY_LENGTH = 32;
    private static final int MAXIMUM_TOKEN_LENGTH = 4096;
    private static final byte[] ADDITIONAL_AUTHENTICATED_DATA =
            "CCADMIN:WEB_CHECKOUT:v1".getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper;
    private final SecretKey encryptionKey;
    private final long expirationMilliseconds;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public SaleDeliveryAccessTokenService(
            ObjectMapper objectMapper,
            @Value("${delivery.sale-access-token.key}") String encodedKey,
            @Value("${delivery.sale-access-token.expiration-milliseconds:86400000}")
            long expirationMilliseconds
    ) {
        this(objectMapper, encodedKey, expirationMilliseconds, Clock.systemUTC());
    }

    SaleDeliveryAccessTokenService(
            ObjectMapper objectMapper,
            String encodedKey,
            long expirationMilliseconds,
            Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.encryptionKey = buildEncryptionKey(encodedKey);
        if (expirationMilliseconds <= 0) {
            throw new IllegalArgumentException(
                    "La vigencia del token de pedido debe ser mayor que cero"
            );
        }
        this.expirationMilliseconds = expirationMilliseconds;
        this.clock = clock;
    }

    public String issue(String saleCod, String clientCod) {
        if (isBlank(saleCod) || isBlank(clientCod)) {
            throw new IllegalArgumentException(
                    "La venta y el cliente son obligatorios para generar el acceso al pedido"
            );
        }

        try {
            long issuedAt = clock.millis();
            SaleDeliveryAccessTokenPayloadDto payload = new SaleDeliveryAccessTokenPayloadDto();
            payload.Version = PAYLOAD_VERSION;
            payload.Purpose = TOKEN_PURPOSE;
            payload.SaleCod = saleCod;
            payload.ClientCod = clientCod;
            payload.IssuedAt = issuedAt;
            payload.ExpiresAt = Math.addExact(issuedAt, expirationMilliseconds);

            byte[] initializationVector = new byte[INITIALIZATION_VECTOR_LENGTH];
            secureRandom.nextBytes(initializationVector);
            Cipher cipher = buildCipher(Cipher.ENCRYPT_MODE, initializationVector);
            byte[] encryptedPayload = cipher.doFinal(objectMapper.writeValueAsBytes(payload));

            return TOKEN_VERSION
                    + "." + encodeUrl(initializationVector)
                    + "." + encodeUrl(encryptedPayload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo generar el acceso seguro al pedido", ex);
        }
    }

    public SaleDeliveryAccessTokenPayloadDto resolve(
            String orderToken,
            String expectedClientCod
    ) {
        if (isBlank(orderToken) || orderToken.length() > MAXIMUM_TOKEN_LENGTH) {
            throw invalidTokenException();
        }

        try {
            String[] tokenParts = orderToken.split("\\.", -1);
            if (tokenParts.length != 3 || !TOKEN_VERSION.equals(tokenParts[0])) {
                throw invalidTokenException();
            }

            byte[] initializationVector = decodeUrl(tokenParts[1]);
            if (initializationVector.length != INITIALIZATION_VECTOR_LENGTH) {
                throw invalidTokenException();
            }
            byte[] encryptedPayload = decodeUrl(tokenParts[2]);
            Cipher cipher = buildCipher(Cipher.DECRYPT_MODE, initializationVector);
            byte[] plainPayload = cipher.doFinal(encryptedPayload);
            SaleDeliveryAccessTokenPayloadDto payload = objectMapper.readValue(
                    plainPayload,
                    SaleDeliveryAccessTokenPayloadDto.class
            );
            validatePayload(payload, expectedClientCod);
            return payload;
        } catch (Exception ex) {
            throw invalidTokenException();
        }
    }

    private void validatePayload(
            SaleDeliveryAccessTokenPayloadDto payload,
            String expectedClientCod
    ) {
        long currentTime = clock.millis();
        if (payload == null
                || payload.Version != PAYLOAD_VERSION
                || !TOKEN_PURPOSE.equals(payload.Purpose)
                || isBlank(payload.SaleCod)
                || isBlank(payload.ClientCod)
                || !payload.ClientCod.equals(expectedClientCod)
                || payload.IssuedAt > currentTime
                || payload.ExpiresAt <= currentTime
                || payload.ExpiresAt <= payload.IssuedAt) {
            throw invalidTokenException();
        }
    }

    private Cipher buildCipher(int operationMode, byte[] initializationVector)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(
                operationMode,
                encryptionKey,
                new GCMParameterSpec(AUTHENTICATION_TAG_LENGTH_BITS, initializationVector)
        );
        cipher.updateAAD(ADDITIONAL_AUTHENTICATED_DATA);
        return cipher;
    }

    private SecretKey buildEncryptionKey(String encodedKey) {
        try {
            if (isBlank(encodedKey)) {
                throw new IllegalArgumentException("La clave esta vacia");
            }
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
            if (decodedKey.length != AES_256_KEY_LENGTH) {
                throw new IllegalArgumentException(
                        "La clave del token de pedido debe contener exactamente 32 bytes"
                );
            }
            return new SecretKeySpec(decodedKey, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "delivery.sale-access-token.key debe ser una clave AES-256 valida en Base64",
                    ex
            );
        }
    }

    private String encodeUrl(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decodeUrl(String value) {
        if (isBlank(value) || value.contains("=")) {
            throw invalidTokenException();
        }
        byte[] decodedValue = Base64.getUrlDecoder().decode(value);
        if (!encodeUrl(decodedValue).equals(value)) {
            throw invalidTokenException();
        }
        return decodedValue;
    }

    private IllegalArgumentException invalidTokenException() {
        return new IllegalArgumentException("El enlace del pedido no es valido o ya vencio");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
