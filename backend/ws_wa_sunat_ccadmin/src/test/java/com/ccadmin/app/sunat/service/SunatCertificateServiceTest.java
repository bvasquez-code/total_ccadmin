package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatConfigDefaults;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SunatCertificateServiceTest {
    @TempDir static Path temporaryDirectory;
    private static byte[] certificateContent;
    private final SunatCertificateService sunatCertificateService = new SunatCertificateService();

    @BeforeAll
    static void generateProductionFixture() throws Exception {
        Path certificate = temporaryDirectory.resolve("fixture.pfx");
        Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
        Process process = new ProcessBuilder(keytool.toString(), "-genkeypair", "-alias", "test", "-keyalg", "RSA",
                "-keysize", "2048", "-dname", "CN=TEST FIXTURE", "-validity", "2", "-storetype", "PKCS12",
                "-storepass", "test-password", "-keypass", "test-password", "-keystore", certificate.toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        certificateContent = Files.readAllBytes(certificate);
    }

    @Test
    void bundledRelativeResourceLoadsWithoutSourceTreeAbsolutePath() {
        var config = SunatConfigDefaults.create("BETA");
        assertNotNull(sunatCertificateService.load(config).privateKey());
    }

    @Test
    void signatureUsesTheSamePortableCertificateLoader() {
        SunatXmlSignatureService signature = new SunatXmlSignatureService();
        ReflectionTestUtils.setField(signature, "sunatCertificateService", sunatCertificateService);
        String xml = "<Invoice xmlns:ext=\"urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2\">"
                + "<ext:UBLExtensions><ext:UBLExtension><ext:ExtensionContent/></ext:UBLExtension></ext:UBLExtensions></Invoice>";
        String signed = signature.sign(SunatConfigDefaults.create("BETA"), xml);
        assertTrue(signed.contains("SignatureValue"));
    }

    @Test
    void productionUploadIsStoredWithRelativeGeneratedNameAndCanBeLoaded() throws Exception {
        String stored = sunatCertificateService.storeProduction(upload("production.pfx", certificateContent), "test-password");
        try {
            assertFalse(Path.of(stored).isAbsolute());
            assertTrue(stored.startsWith("storage/sunat/certificates/"));
            var config = SunatConfigDefaults.create("PRODUCCION");
            config.CertificatePath = stored;
            config.CertificatePassword = "test-password";
            assertNotNull(sunatCertificateService.load(config).certificate());
        } finally { sunatCertificateService.delete(stored); }
    }

    @Test
    void rollbackRemovesUploadedFile() {
        TransactionSynchronizationManager.initSynchronization();
        String stored = null;
        try {
            stored = sunatCertificateService.storeProduction(upload("production.pfx", certificateContent), "test-password");
            assertTrue(Files.exists(Path.of(stored)));
            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization ->
                    synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            assertFalse(Files.exists(Path.of(stored)));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            if (stored != null) sunatCertificateService.delete(stored);
        }
    }

    @Test
    void invalidPasswordOrPayloadOrExtensionAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> sunatCertificateService.storeProduction(upload("valid.pfx", certificateContent), "wrong"));
        assertThrows(IllegalArgumentException.class, () -> sunatCertificateService.storeProduction(upload("invalid.pfx", new byte[]{1, 2}), "test-password"));
        assertThrows(IllegalArgumentException.class, () -> sunatCertificateService.storeProduction(upload("invalid.exe", certificateContent), "test-password"));
        assertThrows(IllegalArgumentException.class, () -> sunatCertificateService.storeProduction(upload("large.pfx", new byte[2 * 1024 * 1024 + 1]), "test-password"));
    }

    private MockMultipartFile upload(String name, byte[] bytes) {
        return new MockMultipartFile("certificate", name, "application/octet-stream", bytes);
    }
}
