package com.ccadmin.app.sunat.identity.model.constants;

import java.util.Arrays;
import java.util.Locale;

public enum IdentityDocumentType {

    DNI("01", "1", "DNI", 8, 8, true, "1"),
    FOREIGNER_ID("04", "4", "Carnet de extranjería", 1, 12, false,
            "4", "CE", "CARNET_EXTRANJERIA", "FOREIGNER_ID"),
    PASSPORT("07", "7", "Pasaporte", 1, 12, false,
            "7", "PAS", "PASAPORTE"),
    DIPLOMATIC_ID("A", "A", "Cédula diplomática", 1, 15, false,
            "CEDULA_DIPLOMATICA", "DIPLOMATIC_ID");

    private final String referenceCode;
    private final String sunatCode;
    private final String displayName;
    private final int minimumLength;
    private final int maximumLength;
    private final boolean numericOnly;
    private final String[] aliases;

    IdentityDocumentType(
            String referenceCode,
            String sunatCode,
            String displayName,
            int minimumLength,
            int maximumLength,
            boolean numericOnly,
            String... aliases
    ) {
        this.referenceCode = referenceCode;
        this.sunatCode = sunatCode;
        this.displayName = displayName;
        this.minimumLength = minimumLength;
        this.maximumLength = maximumLength;
        this.numericOnly = numericOnly;
        this.aliases = aliases;
    }

    public String referenceCode() {
        return referenceCode;
    }

    public String sunatCode() {
        return sunatCode;
    }

    public String displayName() {
        return displayName;
    }

    public String normalizeDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio.");
        }

        String normalized = documentNumber
                .trim()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (normalized.length() < this.minimumLength || normalized.length() > this.maximumLength) {
            throw new IllegalArgumentException(
                    this.displayName + " debe tener entre " + this.minimumLength
                            + " y " + this.maximumLength + " caracteres."
            );
        }

        if (this.numericOnly && !normalized.matches("\\d+")) {
            throw new IllegalArgumentException(this.displayName + " solo debe contener números.");
        }

        if (!this.numericOnly && !normalized.matches("[A-Z0-9-]+")) {
            throw new IllegalArgumentException(this.displayName + " contiene caracteres no permitidos.");
        }

        return normalized;
    }

    public static IdentityDocumentType fromReference(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El tipo de documento es obligatorio.");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(type -> type.matches(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de documento no permitido. Valores aceptados: 01, 04, 07 y A."
                ));
    }

    private boolean matches(String value) {
        return this.name().equals(value)
                || this.referenceCode.equals(value)
                || Arrays.stream(this.aliases).anyMatch(value::equals);
    }
}
