package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.UUID;

@Service
public class SunatCertificateService {
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    public CertificateData load(SunatConfigEntity config) {
        try (InputStream input = open(config.CertificatePath)) {
            return read(input, config.CertificatePassword, config.CertificateType,
                    "PRODUCCION".equals(config.Environment));
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo validar el certificado; compruebe el archivo, su vigencia y su contraseña", exception);
        }
    }

    private InputStream open(String certificatePath) throws Exception {
        if (certificatePath == null || certificatePath.isBlank()) {
            throw new IllegalArgumentException("Certificado requerido");
        }
        if (certificatePath.startsWith("classpath:")) {
            return new ClassPathResource(certificatePath.substring("classpath:".length())).getInputStream();
        }
        // Las rutas relativas se resuelven desde el directorio de ejecución del servicio SUNAT.
        // Se conservan las rutas absolutas de instalaciones anteriores.
        return Files.newInputStream(Path.of(certificatePath).normalize());
    }

    public String storeProduction(MultipartFile certificate, String password) {
        String fileName = certificate == null ? "" : certificate.getOriginalFilename();
        if (certificate == null || certificate.isEmpty() || certificate.getSize() > MAX_SIZE
                || fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".pfx")) {
            throw new IllegalArgumentException("Seleccione un certificado .pfx de hasta 2 MB");
        }
        Path destination = null;
        try {
            byte[] content = certificate.getBytes();
            read(new ByteArrayInputStream(content), password, "PFX", true);
            Path directory = Path.of("storage", "sunat", "certificates");
            Files.createDirectories(directory);
            destination = directory.resolve(UUID.randomUUID() + ".pfx");
            Files.write(destination, content, StandardOpenOption.CREATE_NEW);
            Path storedCertificate = destination;
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) delete(storedCertificate.toString());
                    }
                });
            }
            return destination.toString().replace('\\', '/');
        } catch (Exception exception) {
            if (destination != null) delete(destination.toString());
            throw new IllegalArgumentException("El certificado .pfx debe tener una clave privada RSA, estar vigente y usar la contraseña indicada", exception);
        }
    }

    public void delete(String certificatePath) {
        try { Files.deleteIfExists(Path.of(certificatePath)); }
        catch (Exception exception) { throw new IllegalStateException("No se pudo retirar el certificado de una configuración fallida", exception); }
    }

    private CertificateData read(InputStream input, String password, String type, boolean production) throws Exception {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Contraseña de certificado requerida");
        KeyStore keyStore = KeyStore.getInstance("JKS".equalsIgnoreCase(type) ? "JKS" : "PKCS12");
        char[] characters = password.toCharArray();
        keyStore.load(input, characters);
        var aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias) && keyStore.getKey(alias, characters) instanceof PrivateKey privateKey
                    && keyStore.getCertificate(alias) instanceof X509Certificate certificate
                    && "RSA".equalsIgnoreCase(privateKey.getAlgorithm())) {
                if (production) certificate.checkValidity();
                return new CertificateData(privateKey, certificate);
            }
        }
        throw new IllegalArgumentException("El certificado no contiene una clave privada RSA");
    }

    public record CertificateData(PrivateKey privateKey, X509Certificate certificate) {}
}
