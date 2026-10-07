package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.dto.PinpadSignedMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

@Service
public class PinpadSignatureService {
    private final ObjectMapper objectMapper;
    public PinpadSignatureService(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public PinpadSignedMessageDto sign(Object data, String key) {
        try {
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(data));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(mac("COMMAND", payload, key));
            return new PinpadSignedMessageDto(payload, signature);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo autorizar el comando pinpad");
        }
    }

    public JsonNode verifyCommand(PinpadSignedMessageDto message, String key) {
        try {
            if (message == null || message.payload() == null || message.payload().length() > 16384
                    || message.signature() == null || message.signature().length() > 128
                    || !MessageDigest.isEqual(mac("COMMAND", message.payload(), key),
                    Base64.getUrlDecoder().decode(message.signature()))) throw new IllegalArgumentException();
            return objectMapper.readTree(Base64.getUrlDecoder().decode(message.payload()));
        } catch (Exception exception) {
            throw new IllegalArgumentException("La autorizacion pinpad no tiene una firma valida");
        }
    }

    public JsonNode verifyResult(PinpadSignedMessageDto message, String publicKey) {
        try {
            if (message == null || message.payload() == null || message.payload().length() > 32768
                    || message.signature() == null || message.signature().length() > 1024) throw new IllegalArgumentException();
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))));
            verifier.update(("CCADMIN-PINPAD-V2:RESULT:" + message.payload()).getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getUrlDecoder().decode(message.signature()))) throw new IllegalArgumentException();
            return objectMapper.readTree(Base64.getUrlDecoder().decode(message.payload()));
        } catch (Exception exception) {
            throw new IllegalArgumentException("La respuesta del agente pinpad no tiene una firma valida");
        }
    }

    private byte[] mac(String purpose, String payload, String key) throws Exception {
        if (key == null || key.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(("CCADMIN-PINPAD-V1:" + purpose + ":" + payload).getBytes(StandardCharsets.UTF_8));
    }
}
