package com.local.app.pinpad.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.config.PinpadAgentProperties;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class PinpadIdentityCreateService {
    private final PinpadAgentProperties pinpadAgentProperties;
    private final ObjectMapper objectMapper;
    private KeyPair identity;
    public PinpadIdentityCreateService(PinpadAgentProperties pinpadAgentProperties, ObjectMapper objectMapper) {
        this.pinpadAgentProperties = pinpadAgentProperties; this.objectMapper = objectMapper;
    }
    private synchronized KeyPair identity() {
        if (identity != null) return identity;
        try {
            Path file = Path.of(pinpadAgentProperties.getStoragePath()).resolve("identity-rsa.json");
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (!Files.exists(file)) {
                var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
                var generated = generator.generateKeyPair();
                String json = objectMapper.writeValueAsString(Map.of(
                        "privateKey", Base64.getEncoder().encodeToString(generated.getPrivate().getEncoded()),
                        "publicKey", Base64.getEncoder().encodeToString(generated.getPublic().getEncoded())));
                try { Files.writeString(file, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW); }
                catch (FileAlreadyExistsException ignored) { }
            }
            var stored = objectMapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
            var factory = KeyFactory.getInstance("RSA");
            identity = new KeyPair(factory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(stored.path("publicKey").asText()))),
                    factory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(stored.path("privateKey").asText()))));
            return identity;
        } catch (Exception exception) { throw new IllegalStateException("No se pudo acceder a la identidad local del pinpad"); }
    }
    public String publicKey() { return Base64.getEncoder().encodeToString(identity().getPublic().getEncoded()); }
    public String signResult(String payload) {
        try {
            var signature = Signature.getInstance("SHA256withRSA"); signature.initSign(identity().getPrivate());
            signature.update(("CCADMIN-PINPAD-V2:RESULT:" + payload).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        } catch (Exception exception) { throw new IllegalStateException("No se pudo firmar el comprobante pinpad"); }
    }
}
