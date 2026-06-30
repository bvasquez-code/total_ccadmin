package com.ccadmin.app.sunat.model.constants;

public class SunatElectronicStatusConst {
    public static final String PENDIENTE = "PEN";
    public static final String GENERADO = "GEN";
    public static final String FIRMADO = "FIR";
    public static final String COMPRIMIDO = "ZIP";
    public static final String ENVIADO = "ENV";
    public static final String PENDIENTE_TICKET = "TCK";
    public static final String ACEPTADO = "ACE";
    public static final String ACEPTADO_OBSERVADO = "OBS";
    public static final String RECHAZADO = "REJ";
    public static final String ERROR = "ERR";
    public static final String PENDIENTE_REINTENTO = "RET";
    public static final String ANULADO = "ANU";

    public static boolean isAccepted(String status) {
        return ACEPTADO.equals(status) || ACEPTADO_OBSERVADO.equals(status);
    }

    public static boolean isRejected(String status) {
        return RECHAZADO.equals(status);
    }
}
