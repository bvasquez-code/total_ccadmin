package com.ccadmin.app.system.utility;

import java.util.Objects;

/**
 * Utilidades básicas para Strings.
 * - No depende de librerías externas.
 * - Métodos seguros ante null.
 */
public final class StringUtil {

    private StringUtil() {
        // Utility class
    }

    /* ---------------------------
     * Vacío / Blanco
     * --------------------------- */

    /** true si s == null o s.length() == 0 */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /** true si s != null y s.length() > 0 */
    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    /** true si s == null o s.trim().isEmpty() */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** true si s != null y s.trim().length() > 0 */
    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    /* ---------------------------
     * NVL / Defaults
     * --------------------------- */

    /** NVL clásico: si s es null devuelve defaultValue, sino s */
    public static String nvl(String s, String defaultValue) {
        return (s == null) ? defaultValue : s;
    }

    /** Si s es null o vacío => defaultValue */
    public static String nvlEmpty(String s, String defaultValue) {
        return isEmpty(s) ? defaultValue : s;
    }

    /** Si s es null o blank => defaultValue */
    public static String nvlBlank(String s, String defaultValue) {
        return isBlank(s) ? defaultValue : s;
    }

    /* ---------------------------
     * Trim helpers
     * --------------------------- */

    /** trim seguro: si s es null => null */
    public static String trim(String s) {
        return (s == null) ? null : s.trim();
    }

    /** Si s es null => null; si s.trim() queda vacío => null; sino devuelve el trim */
    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Si s es null => ""; sino trim */
    public static String trimToEmpty(String s) {
        return (s == null) ? "" : s.trim();
    }

    /* ---------------------------
     * Tamaños / Longitud
     * --------------------------- */

    /** Longitud segura: null => 0 */
    public static int length(String s) {
        return (s == null) ? 0 : s.length();
    }

    /** true si length(s) == expectedLength */
    public static boolean hasLength(String s, int expectedLength) {
        if (expectedLength < 0) return false;
        return s != null && s.length() == expectedLength;
    }

    /** true si s != null y s.length() >= min */
    public static boolean minLength(String s, int min) {
        if (min < 0) return false;
        return s != null && s.length() >= min;
    }

    /** true si s != null y s.length() <= max */
    public static boolean maxLength(String s, int max) {
        if (max < 0) return false;
        return s != null && s.length() <= max;
    }

    /** true si s != null y min <= s.length() <= max */
    public static boolean lengthBetween(String s, int minInclusive, int maxInclusive) {
        if (minInclusive < 0 || maxInclusive < 0 || minInclusive > maxInclusive) return false;
        if (s == null) return false;
        int len = s.length();
        return len >= minInclusive && len <= maxInclusive;
    }

    /* ---------------------------
     * Comparaciones seguras
     * --------------------------- */

    public static boolean equals(String a, String b) {
        return Objects.equals(a, b);
    }

    public static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return a == b; // ambos null => true, uno null => false
        return a.equalsIgnoreCase(b);
    }

    /* ---------------------------
     * Contains / Starts / Ends seguros
     * --------------------------- */

    public static boolean contains(String s, String token) {
        return s != null && token != null && s.contains(token);
    }

    public static boolean startsWith(String s, String prefix) {
        return s != null && prefix != null && s.startsWith(prefix);
    }

    public static boolean endsWith(String s, String suffix) {
        return s != null && suffix != null && s.endsWith(suffix);
    }

    /* ---------------------------
     * Substring seguro
     * --------------------------- */

    /**
     * Substring seguro:
     * - Si s es null => null
     * - Ajusta índices fuera de rango
     * - Si start >= end => ""
     */
    public static String safeSubstring(String s, int startInclusive, int endExclusive) {
        if (s == null) return null;

        int len = s.length();
        int start = Math.max(0, startInclusive);
        int end = Math.min(len, endExclusive);

        if (start >= end) return "";
        return s.substring(start, end);
    }

    public static String normalize(String value) {
        return (value == null) ? "" : value.trim();
    }
}
