package com.ccadmin.app.payment.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentDocumentEntity;
import com.ccadmin.app.payment.repository.TrxPaymentDocumentRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
public class TrxPaymentDocumentCreateService extends SessionService {

    public static final String PAYMENT_PROOF = "PAYMENT_PROOF";
    public static final String BASE64_ENCODING = "BASE64";
    public static final String WEB_SOURCE = "WEB";
    public static final long MAX_WEB_PROOF_SIZE_BYTES = 10L * 1024L * 1024L;

    private static final int MAX_WEB_DOCUMENTS = 5;
    private static final Set<String> SUPPORTED_ENCODINGS = Set.of("BASE64", "TEXT", "JSON");
    private static final Set<String> SUPPORTED_WEB_IMAGE_TYPES = Set.of("image/jpeg", "image/png");

    private final TrxPaymentDocumentRepository trxPaymentDocumentRepository;

    public TrxPaymentDocumentCreateService(
            TrxPaymentDocumentRepository trxPaymentDocumentRepository
    ) {
        this.trxPaymentDocumentRepository = trxPaymentDocumentRepository;
    }

    @Transactional
    public List<TrxPaymentDocumentEntity> save(
            Long trxPaymentId,
            List<TrxPaymentDocumentEntity> documentList
    ) {
        return saveAll(trxPaymentId, documentList, this.getUserCod(), null);
    }

    @Transactional
    public List<TrxPaymentDocumentEntity> saveWeb(
            Long trxPaymentId,
            List<TrxPaymentDocumentEntity> documentList
    ) {
        return saveAll(trxPaymentId, documentList, AuditUserConstants.USER_WEB, WEB_SOURCE);
    }

    public void validateWebPaymentProofs(
            List<TrxPaymentDocumentEntity> documentList,
            boolean isRequired
    ) {
        List<TrxPaymentDocumentEntity> safeDocumentList = documentList == null
                ? List.of()
                : documentList;
        if (isRequired && safeDocumentList.isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar la imagen del comprobante de pago");
        }
        if (safeDocumentList.size() > MAX_WEB_DOCUMENTS) {
            throw new IllegalArgumentException("Solo puede adjuntar hasta cinco comprobantes por pago");
        }

        safeDocumentList.forEach(document -> {
            if (document == null
                    || !PAYMENT_PROOF.equals(normalize(document.DocumentType))
                    || !BASE64_ENCODING.equals(normalize(document.ContentEncoding))) {
                throw new IllegalArgumentException("La tienda virtual solo admite comprobantes de pago en Base64");
            }
            String contentType = normalizeContentType(document.ContentType);
            if (!SUPPORTED_WEB_IMAGE_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("El comprobante debe ser una imagen JPG o PNG");
            }
            byte[] content = decodeBase64(normalizeBase64Content(document));
            if (content.length > MAX_WEB_PROOF_SIZE_BYTES) {
                throw new IllegalArgumentException("La imagen del comprobante no puede superar los 10 MB");
            }
            validateImage(content);
        });
    }

    private List<TrxPaymentDocumentEntity> saveAll(
            Long trxPaymentId,
            List<TrxPaymentDocumentEntity> documentList,
            String userCod,
            String sourceType
    ) {
        if (trxPaymentId == null || trxPaymentId <= 0) {
            throw new IllegalArgumentException("El identificador del pago es obligatorio");
        }
        if (documentList == null || documentList.isEmpty()) {
            return List.of();
        }

        documentList.forEach(document -> prepareForSave(
                document,
                trxPaymentId,
                userCod,
                sourceType
        ));
        return this.trxPaymentDocumentRepository.saveAll(documentList);
    }

    private void prepareForSave(
            TrxPaymentDocumentEntity document,
            Long trxPaymentId,
            String userCod,
            String sourceType
    ) {
        if (document == null) {
            throw new IllegalArgumentException("El documento del pago es obligatorio");
        }
        document.DocumentType = requireText(
                document.DocumentType,
                "El tipo de documento es obligatorio",
                32
        ).toUpperCase();
        document.ContentEncoding = requireText(
                document.ContentEncoding,
                "El formato del contenido es obligatorio",
                16
        ).toUpperCase();
        if (!SUPPORTED_ENCODINGS.contains(document.ContentEncoding)) {
            throw new IllegalArgumentException("El formato del contenido debe ser BASE64, TEXT o JSON");
        }
        document.SourceType = sourceType == null
                ? requireText(document.SourceType, "El origen del documento es obligatorio", 16).toUpperCase()
                : sourceType;
        document.FileName = optionalText(document.FileName, 255, "El nombre del archivo excede los 255 caracteres");
        document.ContentType = optionalText(
                document.ContentType,
                100,
                "El tipo MIME excede los 100 caracteres"
        );
        if (document.ContentType != null) {
            document.ContentType = document.ContentType.toLowerCase();
        }

        byte[] originalContent;
        if (BASE64_ENCODING.equals(document.ContentEncoding)) {
            document.Content = normalizeBase64Content(document);
            originalContent = decodeBase64(document.Content);
        } else {
            if (document.Content == null || document.Content.isBlank()) {
                throw new IllegalArgumentException("El contenido del documento es obligatorio");
            }
            originalContent = document.Content.getBytes(StandardCharsets.UTF_8);
        }
        if (originalContent.length > MAX_WEB_PROOF_SIZE_BYTES) {
            throw new IllegalArgumentException("El contenido del documento no puede superar los 10 MB");
        }

        document.TrxPaymentDocumentId = null;
        document.TrxPaymentId = trxPaymentId;
        document.SizeBytes = (long) originalContent.length;
        document.Sha256Hash = sha256(originalContent);
        document.addSession(userCod, true);
        document.Status = "A";
    }

    private String normalizeBase64Content(TrxPaymentDocumentEntity document) {
        String content = requireText(document.Content, "El contenido del comprobante es obligatorio", null);
        int separator = content.indexOf(',');
        if (content.startsWith("data:") && separator >= 0) {
            String metadata = content.substring(5, separator);
            int encodingSeparator = metadata.indexOf(';');
            if ((document.ContentType == null || document.ContentType.isBlank()) && encodingSeparator > 0) {
                document.ContentType = metadata.substring(0, encodingSeparator);
            }
            content = content.substring(separator + 1);
        }
        return content.replaceAll("\\s+", "");
    }

    private byte[] decodeBase64(String content) {
        try {
            return Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El contenido del comprobante no es un Base64 valido");
        }
    }

    private void validateImage(byte[] content) {
        try {
            if (ImageIO.read(new ByteArrayInputStream(content)) == null) {
                throw new IllegalArgumentException("El comprobante adjunto no contiene una imagen valida");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo validar la imagen del comprobante", exception);
        }
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no esta disponible", exception);
        }
    }

    private String requireText(String value, String message, Integer maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        if (maxLength != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeContentType(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
