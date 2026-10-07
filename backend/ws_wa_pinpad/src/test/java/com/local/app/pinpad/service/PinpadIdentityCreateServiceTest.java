package com.local.app.pinpad.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.config.PinpadAgentProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class PinpadIdentityCreateServiceTest {
    @TempDir Path directory;
    @Test void generatesPersistentIdentityAndSignsReceiptsWithoutConfiguredSecrets() throws Exception {
        var properties = new PinpadAgentProperties(); properties.setStoragePath(directory.toString());
        var first = new PinpadIdentityCreateService(properties, new ObjectMapper());
        var restarted = new PinpadIdentityCreateService(properties, new ObjectMapper());
        assertEquals(first.publicKey(), restarted.publicKey());
        var verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(first.publicKey()))));
        verifier.update("CCADMIN-PINPAD-V2:RESULT:receipt".getBytes(StandardCharsets.UTF_8));
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(restarted.signResult("receipt"))));
    }
}
