package com.ccadmin.app.sunat.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentFileEntity;
import com.ccadmin.app.sunat.repository.SunatDocumentFileRepository;
import com.ccadmin.app.sunat.utility.SunatCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class SunatFileStorageService extends SessionService {

    @Autowired
    private SunatDocumentFileRepository sunatDocumentFileRepository;

    public SunatDocumentFileEntity saveXml(SunatConfigEntity config, SunatDocumentEntity document,
                                           String fileType, String fileName, String xmlContent) {
        return this.save(config, document, fileType, fileName, "application/xml",
                xmlContent.getBytes(StandardCharsets.UTF_8));
    }

    public SunatDocumentFileEntity saveZip(SunatConfigEntity config, SunatDocumentEntity document,
                                           String fileType, String fileName, byte[] content) {
        return this.save(config, document, fileType, fileName, "application/zip", content);
    }

    public byte[] read(SunatDocumentFileEntity file) {
        try {
            return Files.readAllBytes(Path.of(file.FilePath));
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer archivo SUNAT: " + file.FilePath, ex);
        }
    }

    private SunatDocumentFileEntity save(SunatConfigEntity config, SunatDocumentEntity document, String fileType,
                                         String fileName, String contentType, byte[] content) {
        if (config.StorageBasePath == null || config.StorageBasePath.isBlank()) {
            throw new IllegalArgumentException("Ruta base de almacenamiento SUNAT requerida");
        }
        try {
            Path directory = buildDirectory(config, document);
            Files.createDirectories(directory);
            Path filePath = directory.resolve(fileName);
            Files.write(filePath, content);

            SunatDocumentFileEntity entity = new SunatDocumentFileEntity();
            entity.SunatFileCod = SunatCodeUtil.newCode("SF");
            entity.SunatDocumentCod = document.SunatDocumentCod;
            entity.FileType = fileType;
            entity.FileName = fileName;
            entity.FilePath = filePath.toAbsolutePath().toString();
            entity.ContentType = contentType;
            entity.SizeBytes = (long) content.length;
            entity.Sha256Hash = sha256(content);
            entity.addSession(this.getUserCod(), true);
            entity.validate();
            return this.sunatDocumentFileRepository.save(entity);
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo guardar archivo SUNAT: " + ex.getMessage(), ex);
        }
    }

    private Path buildDirectory(SunatConfigEntity config, SunatDocumentEntity document) {
        LocalDate date = document.IssueDate == null
                ? LocalDate.now()
                : document.IssueDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Path.of(
                config.StorageBasePath,
                document.IssuerRuc,
                String.valueOf(date.getYear()),
                String.format("%02d", date.getMonthValue()),
                String.format("%02d", date.getDayOfMonth()),
                document.SunatDocumentType,
                document.FullDocumentNumber
        );
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return String.format("%064x", new BigInteger(1, hash));
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo calcular hash SHA-256", ex);
        }
    }
}
