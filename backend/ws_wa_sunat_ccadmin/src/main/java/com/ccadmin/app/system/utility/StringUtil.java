package com.ccadmin.app.system.utility;

public class StringUtil {

    public static boolean isEmpty(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }
}
