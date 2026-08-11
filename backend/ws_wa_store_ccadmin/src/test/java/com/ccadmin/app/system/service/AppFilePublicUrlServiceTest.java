package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.AppFileEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppFilePublicUrlServiceTest {

    @Test
    void buildsAnAbsolutePublicRouteWithoutDuplicatingSlashes() {
        AppFilePublicUrlService service = new AppFilePublicUrlService("https://api.empresa.com/");

        assertEquals(
                "https://api.empresa.com/api/v1/public/appFile/IMG001",
                service.buildPublicRoute("IMG001")
        );
    }

    @Test
    void replacesTheStoredRouteOnlyInTheResponseCopy() {
        AppFilePublicUrlService service = new AppFilePublicUrlService("http://localhost:8090");
        AppFileEntity storedFile = new AppFileEntity();
        storedFile.FileCod = "IMG001";
        storedFile.Route = "image/IMG001.jpg";

        AppFileEntity publicFile = service.toPublicEntity(storedFile);

        assertEquals("image/IMG001.jpg", storedFile.Route);
        assertEquals("http://localhost:8090/api/v1/public/appFile/IMG001", publicFile.Route);
    }

    @Test
    void usesARelativeRouteWhenThereIsNoRequestOrConfiguredBaseUrl() {
        AppFilePublicUrlService service = new AppFilePublicUrlService("");

        assertEquals("/api/v1/public/appFile/IMG001", service.buildPublicRoute("IMG001"));
    }
}
