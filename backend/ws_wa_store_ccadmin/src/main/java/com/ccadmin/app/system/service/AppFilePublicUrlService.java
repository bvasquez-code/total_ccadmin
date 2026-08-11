package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.AppFileEntity;
import com.ccadmin.app.system.model.entity.PaymentMethodEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
public class AppFilePublicUrlService {

    private static final String PUBLIC_FILE_PATH = "/api/v1/public/appFile/";

    private final String publicBaseUrl;

    public AppFilePublicUrlService(@Value("${app.file.public-base-url:}") String publicBaseUrl) {
        this.publicBaseUrl = removeTrailingSlash(publicBaseUrl == null ? "" : publicBaseUrl.trim());
    }

    public String buildPublicRoute(String fileCod) {
        if (fileCod == null || fileCod.isBlank()) {
            return "";
        }
        return resolvePublicBaseUrl() + PUBLIC_FILE_PATH
                + UriUtils.encodePathSegment(fileCod, StandardCharsets.UTF_8);
    }

    public AppFileEntity toPublicEntity(AppFileEntity storedFile) {
        if (storedFile == null) {
            return null;
        }
        AppFileEntity publicFile = new AppFileEntity();
        publicFile.FileCod = storedFile.FileCod;
        publicFile.Name = storedFile.Name;
        publicFile.Description = storedFile.Description;
        publicFile.Route = buildPublicRoute(storedFile.FileCod);
        publicFile.FileType = storedFile.FileType;
        publicFile.CreationUser = storedFile.CreationUser;
        publicFile.CreationDate = storedFile.CreationDate;
        publicFile.ModifyUser = storedFile.ModifyUser;
        publicFile.ModifyDate = storedFile.ModifyDate;
        publicFile.Status = storedFile.Status;
        return publicFile;
    }

    public PaymentMethodEntity applyPublicRoute(PaymentMethodEntity paymentMethod) {
        if (paymentMethod != null) {
            paymentMethod.Route = buildPublicRoute(paymentMethod.FileCod);
        }
        return paymentMethod;
    }

    private String removeTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String resolvePublicBaseUrl() {
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl;
        }
        try {
            return removeTrailingSlash(
                    ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
            );
        } catch (IllegalStateException ex) {
            return "";
        }
    }
}
