package com.ccadmin.app.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Identidad del backend y registro automatico de las claves publicas de sus agentes. */
@Service
public class PinpadTrustCreateService {
    private final Path directory;
    public PinpadTrustCreateService(@Value("${pinpad.security.storage-path:./data/pinpad-security}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    public synchronized String commandSigningKey() {
        byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
        return readOrCreate(directory.resolve("backend.key"), Base64.getUrlEncoder().withoutPadding().encodeToString(key));
    }
    public synchronized void registerAgent(String agentId, String publicKey) {
        validateAgentId(agentId);
        try {
            var parsed = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey)));
            if (!(parsed instanceof RSAPublicKey rsa) || rsa.getModulus().bitLength() < 2048) throw new IllegalArgumentException();
        } catch (Exception exception) { throw new IllegalArgumentException("Identidad del agente pinpad invalida"); }
        String registered = readOrCreate(directory.resolve(agentId + ".public-key"), publicKey);
        if (!registered.equals(publicKey)) throw new IllegalArgumentException(
                "La identidad de esta caja cambio. Conserve la carpeta de datos del agente al reinstalar");
    }
    public String agentPublicKey(String agentId) {
        validateAgentId(agentId);
        try { return Files.readString(directory.resolve(agentId + ".public-key"), StandardCharsets.UTF_8); }
        catch (Exception exception) { throw new IllegalArgumentException("El agente debe iniciar sesion antes de confirmar el pago"); }
    }
    private void validateAgentId(String agentId) {
        if (agentId == null || !agentId.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("Identificador de agente invalido");
    }
    private String readOrCreate(Path file, String value) {
        try {
            Files.createDirectories(directory);
            try { Files.writeString(file, value, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW); }
            catch (FileAlreadyExistsException ignored) { }
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (Exception exception) { throw new IllegalStateException("No se pudo acceder a la identidad del servicio pinpad"); }
    }
}
