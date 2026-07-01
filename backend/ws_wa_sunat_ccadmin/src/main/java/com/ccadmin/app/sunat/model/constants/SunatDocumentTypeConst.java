package com.ccadmin.app.sunat.model.constants;

public class SunatDocumentTypeConst {
    public static final String FACTURA = "01";
    public static final String BOLETA = "03";
    public static final String NOTA_CREDITO = "07";
    public static final String NOTA_DEBITO = "08";
    public static final String GUIA_REMISION_REMITENTE = "09";
    public static final String RESUMEN_DIARIO = "RC";
    public static final String COMUNICACION_BAJA = "RA";

    public static boolean isValid(String documentType) {
        return FACTURA.equals(documentType)
                || BOLETA.equals(documentType)
                || NOTA_CREDITO.equals(documentType)
                || NOTA_DEBITO.equals(documentType)
                || GUIA_REMISION_REMITENTE.equals(documentType)
                || RESUMEN_DIARIO.equals(documentType)
                || COMUNICACION_BAJA.equals(documentType);
    }
}
