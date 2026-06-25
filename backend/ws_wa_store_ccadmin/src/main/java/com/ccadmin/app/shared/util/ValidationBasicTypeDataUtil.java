package com.ccadmin.app.shared.util;

public final class ValidationBasicTypeDataUtil {

    public static boolean validateText(String text,int longText) {
        return text.matches("[a-zA-Z0-9]+") && (text.length()<=longText);
    }

}
