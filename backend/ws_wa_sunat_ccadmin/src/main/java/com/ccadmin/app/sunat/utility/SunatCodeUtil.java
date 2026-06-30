package com.ccadmin.app.sunat.utility;

import java.security.SecureRandom;

public class SunatCodeUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String newCode(String prefix) {
        String time = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        String random = Long.toString(Math.abs(RANDOM.nextLong()), 36).toUpperCase();
        String code = prefix + time + random;
        return code.substring(0, Math.min(code.length(), 24));
    }
}
