package com.ccadmin.app.system.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Service
public class AppFileStorageService {

    private static final String LEGACY_PUBLIC_SEGMENT = "assets/public/";
    private static final Map<Integer, String> GROUP_DIRECTORIES = Map.of(
            1, "image",
            2, "document",
            3, "imagesystem"
    );

    private final Path storageRoot;

    public AppFileStorageService(@Value("${app.file.storage-root:./data/uploads}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void initialize() throws IOException {
        Files.createDirectories(storageRoot);
    }

    public String buildStorageRoute(int groupTypeFile, String fileName) {
        String directory = GROUP_DIRECTORIES.getOrDefault(groupTypeFile, "other");
        return directory + "/" + fileName;
    }

    public void store(String storageRoute, byte[] content) throws IOException {
        Path destination = resolveStoragePath(storageRoute);
        Files.createDirectories(destination.getParent());
        Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    public Resource findResource(String storageRoute) {
        Path storagePath = resolveStoragePath(storageRoute);
        if (!Files.isRegularFile(storagePath) || !Files.isReadable(storagePath)) {
            throw new IllegalArgumentException("El archivo solicitado no existe en el almacenamiento");
        }
        try {
            return new UrlResource(storagePath.toUri());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("No se pudo obtener el archivo solicitado", ex);
        }
    }

    public String findContentType(Resource resource) {
        try {
            String contentType = Files.probeContentType(resource.getFile().toPath());
            return contentType == null ? "application/octet-stream" : contentType;
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    public void deleteIfExists(String storageRoute) {
        try {
            Files.deleteIfExists(resolveStoragePath(storageRoute));
        } catch (IOException ignored) {
            // El registro no se guardó; el archivo huérfano podrá limpiarse posteriormente.
        }
    }

    public String normalizeStorageRoute(String route) {
        if (route == null || route.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo no está configurada");
        }

        String normalizedRoute = route.trim().replace('\\', '/');
        int legacyIndex = normalizedRoute.toLowerCase().indexOf(LEGACY_PUBLIC_SEGMENT);
        if (legacyIndex >= 0) {
            normalizedRoute = normalizedRoute.substring(legacyIndex + LEGACY_PUBLIC_SEGMENT.length());
        }
        while (normalizedRoute.startsWith("/")) {
            normalizedRoute = normalizedRoute.substring(1);
        }

        Path relativePath = Path.of(normalizedRoute).normalize();
        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            throw new IllegalArgumentException("La ruta del archivo no es válida");
        }
        return relativePath.toString().replace('\\', '/');
    }

    public Path getStorageRoot() {
        return storageRoot;
    }

    private Path resolveStoragePath(String storageRoute) {
        String normalizedRoute = normalizeStorageRoute(storageRoute);
        Path storagePath = storageRoot.resolve(normalizedRoute).normalize();
        if (!storagePath.startsWith(storageRoot)) {
            throw new IllegalArgumentException("La ruta del archivo está fuera del almacenamiento permitido");
        }
        return storagePath;
    }
}
