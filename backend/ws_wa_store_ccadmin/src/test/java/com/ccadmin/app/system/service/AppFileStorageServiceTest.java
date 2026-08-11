package com.ccadmin.app.system.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppFileStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAndReadsAFileInsideTheConfiguredDirectory() throws Exception {
        AppFileStorageService service = new AppFileStorageService(temporaryDirectory.toString());
        service.initialize();
        byte[] content = "ccadmin-image".getBytes();

        service.store("image/IMG001.jpg", content);
        Resource resource = service.findResource("image/IMG001.jpg");

        assertArrayEquals(content, Files.readAllBytes(resource.getFile().toPath()));
    }

    @Test
    void normalizesLegacyFrontendRoutes() {
        AppFileStorageService service = new AppFileStorageService(temporaryDirectory.toString());

        String result = service.normalizeStorageRoute(
                "http://localhost:4200/assets/public/imagesystem/IMG001.png"
        );

        assertEquals("imagesystem/IMG001.png", result);
    }

    @Test
    void rejectsPathsOutsideTheStorageRoot() {
        AppFileStorageService service = new AppFileStorageService(temporaryDirectory.toString());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findResource("../../confidential.txt")
        );
    }
}
