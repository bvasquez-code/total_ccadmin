package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.dto.AppFileDto;
import com.ccadmin.app.system.model.dto.AppFileResourceDto;
import com.ccadmin.app.system.model.entity.AppFileEntity;
import com.ccadmin.app.system.repository.AppFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AppFileService extends SessionService {

    private final AppFileRepository appFileRepository;
    private final AppFileStorageService appFileStorageService;
    private final AppFilePublicUrlService appFilePublicUrlService;
    private final long maximumFileSizeBytes;

    public AppFileService(
            AppFileRepository appFileRepository,
            AppFileStorageService appFileStorageService,
            AppFilePublicUrlService appFilePublicUrlService,
            @Value("${app.file.maximum-size-bytes:10485760}") long maximumFileSizeBytes
    ) {
        this.appFileRepository = appFileRepository;
        this.appFileStorageService = appFileStorageService;
        this.appFilePublicUrlService = appFilePublicUrlService;
        this.maximumFileSizeBytes = maximumFileSizeBytes;
    }

    public AppFileEntity findById(String FileCod) {
        return appFilePublicUrlService.toPublicEntity(findStoredById(FileCod));
    }

    public AppFileResourceDto findResource(String FileCod) {
        AppFileEntity storedFile = findStoredById(FileCod);
        AppFileResourceDto result = new AppFileResourceDto();
        result.Resource = appFileStorageService.findResource(storedFile.Route);
        result.ContentType = appFileStorageService.findContentType(result.Resource);
        result.FileName = storedFile.Name;
        return result;
    }

    public ResponseWsDto save(AppFileDto appFileDto) throws IOException {
        validateRequest(appFileDto);

        String extension = normalizeExtension(appFileDto.extension);
        byte[] content = decodeContent(appFileDto.base64);
        if (content.length > maximumFileSizeBytes) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido");
        }

        int groupTypeFile = appFileDto.groupTypeFile == 0 ? 1 : appFileDto.groupTypeFile;
        AppFileEntity storedFile = new AppFileEntity();
        storedFile.FileType = getTypeFile(extension);
        storedFile.FileCod = generateCodFile(storedFile.FileType);
        storedFile.Name = storedFile.FileCod + "." + extension;
        storedFile.Route = appFileStorageService.buildStorageRoute(groupTypeFile, storedFile.Name);
        storedFile.Description = "no Description";
        storedFile.addSession(getUserCod());

        appFileStorageService.store(storedFile.Route, content);
        try {
            AppFileEntity savedFile = appFileRepository.save(storedFile);
            return new ResponseWsDto(appFilePublicUrlService.toPublicEntity(savedFile));
        } catch (RuntimeException ex) {
            appFileStorageService.deleteIfExists(storedFile.Route);
            throw ex;
        }
    }

    public String getTypeFile(String extension) {
        String[] imageExtensions = {
                "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "ico", "heif", "svg", "avif"
        };
        boolean isImage = Arrays.stream(imageExtensions)
                .anyMatch(value -> value.equalsIgnoreCase(extension));
        return isImage ? "IMAGE" : "OTHER";
    }

    private AppFileEntity findStoredById(String FileCod) {
        if (FileCod == null || FileCod.isBlank()) {
            throw new IllegalArgumentException("El código del archivo es obligatorio");
        }
        return appFileRepository.findById(FileCod)
                .orElseThrow(() -> new IllegalArgumentException("El archivo solicitado no existe"));
    }

    private void validateRequest(AppFileDto appFileDto) {
        if (appFileDto == null) {
            throw new IllegalArgumentException("Los datos del archivo son obligatorios");
        }
        if (appFileDto.base64 == null || appFileDto.base64.isBlank()) {
            throw new IllegalArgumentException("El contenido del archivo es obligatorio");
        }
        if (appFileDto.extension == null || appFileDto.extension.isBlank()) {
            throw new IllegalArgumentException("La extensión del archivo es obligatoria");
        }
    }

    private byte[] decodeContent(String base64) {
        int separatorIndex = base64.indexOf(',');
        String encodedContent = separatorIndex >= 0 ? base64.substring(separatorIndex + 1) : base64;
        try {
            return Base64.getDecoder().decode(encodedContent);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El contenido del archivo no tiene un formato Base64 válido", ex);
        }
    }

    private String normalizeExtension(String extension) {
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("[a-z0-9]{1,10}")) {
            throw new IllegalArgumentException("La extensión del archivo no es válida");
        }
        return normalized;
    }

    private String generateCodFile(String typeFile) {
        Map<String, String> typeMapping = new HashMap<>();
        typeMapping.put("IMAGE", "IMG");
        typeMapping.put("OTHER", "OTR");
        typeMapping.put("DOCUMENT", "DOC");
        typeMapping.put("VIDEO", "VID");

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String formattedDate = today.format(formatter);
        String suffix = typeMapping.getOrDefault(typeFile, "OTR");
        String baseCode = formattedDate + UUID.randomUUID().toString().replace("-", "");

        if (baseCode.length() > 17) {
            baseCode = baseCode.substring(0, 17);
        }
        while (baseCode.length() < 17) {
            baseCode = "0" + baseCode;
        }
        return suffix + baseCode;
    }
}
