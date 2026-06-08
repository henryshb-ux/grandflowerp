package com.artivisi.accountingfinance.util;

public final class FormUtils {

    private FormUtils() {
    }

    /**
     * Converts a nullable Boolean (from an HTML checkbox) to a non-null boolean.
     * HTML checkboxes submit no value when unchecked, resulting in null.
     * This method treats null as false.
     */
    public static boolean checkboxValue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
