package com.ccadmin.app.sunat.model.constants;

public class SunatEnvironmentConst {
    public static final String BETA = "BETA";
    public static final String PRODUCCION = "PRODUCCION";

    public static boolean isValid(String environment) {
        return BETA.equals(environment) || PRODUCCION.equals(environment);
    }
}
