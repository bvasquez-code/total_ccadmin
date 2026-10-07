package com.local.app.pinpad.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Base64;

/** Prepara una identidad persistente antes de que Spring lea la configuracion externa. */
public class PinpadInstallationCreateService {
    public Path prepare(Path installationDirectory) {
        Path root = installationDirectory.toAbsolutePath().normalize();
        Path configuration = root.resolve("pinpad-cajas.yml");
        Path configuredSubdirectory = root.resolve("config/pinpad-cajas.yml");
        if (Files.exists(configuredSubdirectory)) return configuredSubdirectory;
        if (Files.exists(configuration)) return configuration;
        try (var template = getClass().getResourceAsStream("/pinpad-cajas.template.yml")) {
            if (template == null) throw new IOException("Falta la plantilla de instalacion");
            String content = new String(template.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("__AGENT_TOKEN__", randomKey());
            Files.createDirectories(root);
            Files.writeString(configuration, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return configuration;
        } catch (FileAlreadyExistsException exception) {
            return configuration;
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo preparar pinpad-cajas.yml en " + root, exception);
        }
    }

    private String randomKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
