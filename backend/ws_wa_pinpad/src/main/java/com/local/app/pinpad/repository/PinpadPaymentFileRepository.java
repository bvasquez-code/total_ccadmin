package com.local.app.pinpad.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.constants.PinpadConstants;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.enums.PinpadPaymentStatus;
import com.local.app.pinpad.exception.PinpadPaymentException;
import com.local.app.pinpad.model.dto.PinpadPaymentAckDto;
import com.local.app.pinpad.model.dto.PinpadPaymentDetailDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Repository
public class PinpadPaymentFileRepository {

    private static final Pattern SAFE_PAYMENT_ID = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final PinpadAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final List<String> searchableFolders = List.of(
            PinpadConstants.FOLDER_PROCESSING,
            PinpadConstants.FOLDER_APPROVED,
            PinpadConstants.FOLDER_REJECTED,
            PinpadConstants.FOLDER_CANCELLED,
            PinpadConstants.FOLDER_TIMEOUT,
            PinpadConstants.FOLDER_ERROR,
            PinpadConstants.FOLDER_UNKNOWN,
            PinpadConstants.FOLDER_READ,
            PinpadConstants.FOLDER_ARCHIVE,
            PinpadConstants.FOLDER_PENDING
    );

    public PinpadPaymentFileRepository(PinpadAgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeStorage() {
        List<String> folders = List.of(
                PinpadConstants.FOLDER_PENDING,
                PinpadConstants.FOLDER_PROCESSING,
                PinpadConstants.FOLDER_APPROVED,
                PinpadConstants.FOLDER_REJECTED,
                PinpadConstants.FOLDER_CANCELLED,
                PinpadConstants.FOLDER_TIMEOUT,
                PinpadConstants.FOLDER_ERROR,
                PinpadConstants.FOLDER_UNKNOWN,
                PinpadConstants.FOLDER_READ,
                PinpadConstants.FOLDER_LOGS,
                PinpadConstants.FOLDER_ARCHIVE
        );
        try {
            Files.createDirectories(root());
            for (String folder : folders) {
                Files.createDirectories(root().resolve(folder));
            }
        } catch (IOException e) {
            throw storageException("No se pudieron crear las carpetas del agente pinpad", e);
        }
    }

    public void saveProcessing(PinpadPaymentDetailDto dto) {
        dto.setStatus(PinpadPaymentStatus.PROCESSING);
        dto.setUpdatedAt(LocalDateTime.now());
        write(folderForStatus(dto.getStatus()).resolve(fileName(dto.getPaymentId())), dto);
    }

    public void saveFinal(PinpadPaymentDetailDto dto) {
        dto.setUpdatedAt(LocalDateTime.now());
        Path target = folderForStatus(dto.getStatus()).resolve(fileName(dto.getPaymentId()));
        write(target, dto);
        Path processing = root().resolve(PinpadConstants.FOLDER_PROCESSING).resolve(fileName(dto.getPaymentId()));
        if (!processing.equals(target) && Files.exists(processing)) {
            deleteQuietly(processing);
        }
    }

    public Optional<PinpadPaymentDetailDto> findByPaymentId(String paymentId) {
        return findInAllFolders(paymentId);
    }

    public Optional<PinpadPaymentDetailDto> findFirstProcessing() {
        Path processing = root().resolve(PinpadConstants.FOLDER_PROCESSING);
        try (Stream<Path> paths = Files.list(processing)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .findFirst()
                    .map(this::read);
        } catch (IOException e) {
            throw storageException("No se pudo revisar pagos en proceso", e);
        }
    }

    public Optional<PinpadPaymentDetailDto> findInAllFolders(String paymentId) {
        validatePaymentId(paymentId);
        for (String folder : searchableFolders) {
            Path file = root().resolve(folder).resolve(fileName(paymentId));
            if (Files.exists(file)) {
                return Optional.of(read(file));
            }
        }
        return Optional.empty();
    }

    public PinpadPaymentDetailDto moveToStatusFolder(String paymentId, PinpadPaymentStatus status) {
        PinpadPaymentDetailDto detail = findInAllFolders(paymentId)
                .orElseThrow(() -> new PinpadPaymentException(PinpadErrorCode.PAYMENT_NOT_FOUND, "Pago no encontrado"));
        detail.setStatus(status);
        detail.setUpdatedAt(LocalDateTime.now());
        Path target = folderForStatus(status).resolve(fileName(paymentId));
        write(target, detail);
        removeOtherCopies(paymentId, target);
        return detail;
    }

    public PinpadPaymentDetailDto markAsRead(String paymentId, PinpadPaymentAckDto ackDto) {
        PinpadPaymentDetailDto detail = findInAllFolders(paymentId)
                .orElseThrow(() -> new PinpadPaymentException(PinpadErrorCode.PAYMENT_NOT_FOUND, "Pago no encontrado"));
        detail.setStatus(PinpadPaymentStatus.READ);
        detail.setAckReceived(true);
        detail.setAckAt(LocalDateTime.now());
        detail.setCentralSaved(Boolean.TRUE.equals(ackDto.getCentralSaved()));
        detail.setCentralPaymentCod(ackDto.getCentralPaymentCod());
        detail.setMessage("Resultado marcado como leido");
        detail.setUpdatedAt(LocalDateTime.now());
        Path target = folderForStatus(PinpadPaymentStatus.READ).resolve(fileName(paymentId));
        write(target, detail);
        removeOtherCopies(paymentId, target);
        appendLog(paymentId, "ACK_RECEIVED", ackDto);
        return detail;
    }

    public void appendLog(String paymentId, String event, Object payload) {
        validatePaymentId(paymentId);
        Path log = root().resolve(PinpadConstants.FOLDER_LOGS).resolve(paymentId + ".jsonl");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("at", LocalDateTime.now());
        row.put("event", event);
        row.put("payload", payload);
        try {
            Files.writeString(log, objectMapper.writeValueAsString(row) + System.lineSeparator(),
                    StandardCharsets.UTF_8, Files.exists(log)
                            ? new StandardOpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.APPEND}
                            : new StandardOpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE});
        } catch (IOException e) {
            throw storageException("No se pudo escribir log de operacion pinpad", e);
        }
    }

    public void validatePaymentId(String paymentId) {
        if (paymentId == null || paymentId.isBlank() || !SAFE_PAYMENT_ID.matcher(paymentId).matches()) {
            throw new PinpadPaymentException(PinpadErrorCode.INVALID_REQUEST,
                    "paymentId invalido. Solo se permiten letras, numeros, punto, guion y guion bajo");
        }
    }

    private PinpadPaymentDetailDto read(Path file) {
        try {
            return objectMapper.readValue(file.toFile(), PinpadPaymentDetailDto.class);
        } catch (IOException e) {
            throw storageException("No se pudo leer archivo de pago pinpad", e);
        }
    }

    private void write(Path target, PinpadPaymentDetailDto dto) {
        validatePaymentId(dto.getPaymentId());
        try {
            Files.createDirectories(target.getParent());
            Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), dto);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw storageException("No se pudo persistir archivo de pago pinpad", e);
        }
    }

    private void removeOtherCopies(String paymentId, Path keep) {
        validatePaymentId(paymentId);
        for (String folder : searchableFolders) {
            Path candidate = root().resolve(folder).resolve(fileName(paymentId));
            if (!candidate.equals(keep) && Files.exists(candidate)) {
                deleteQuietly(candidate);
            }
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw storageException("No se pudo mover archivo de pago pinpad", e);
        }
    }

    private String fileName(String paymentId) {
        validatePaymentId(paymentId);
        return paymentId + ".json";
    }

    private Path folderForStatus(PinpadPaymentStatus status) {
        return root().resolve(switch (status) {
            case CREATED -> PinpadConstants.FOLDER_PENDING;
            case PROCESSING -> PinpadConstants.FOLDER_PROCESSING;
            case APPROVED -> PinpadConstants.FOLDER_APPROVED;
            case REJECTED -> PinpadConstants.FOLDER_REJECTED;
            case CANCELLED -> PinpadConstants.FOLDER_CANCELLED;
            case TIMEOUT -> PinpadConstants.FOLDER_TIMEOUT;
            case ERROR -> PinpadConstants.FOLDER_ERROR;
            case UNKNOWN -> PinpadConstants.FOLDER_UNKNOWN;
            case READ -> PinpadConstants.FOLDER_READ;
        });
    }

    private Path root() {
        return Path.of(properties.getStoragePath()).toAbsolutePath().normalize();
    }

    private PinpadPaymentException storageException(String message, Throwable cause) {
        return new PinpadPaymentException(PinpadErrorCode.STORAGE_ERROR, message, cause);
    }
}
