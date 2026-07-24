package com.ccadmin.app.bulkload.model.constants;

public final class BulkLoadConstants {
    public static final String TYPE_PRODUCT_PRICE = "PRODUCT_PRICE";
    public static final String TYPE_STOCK_ENTRY = "STOCK_ENTRY";

    public static final String DRAFT = "D";
    public static final String VALIDATING = "V";
    public static final String PENDING = "P";
    public static final String QUEUED = "Q";
    public static final String WORKING = "W";
    public static final String FINALIZED = "F";
    public static final String ERROR = "E";
    public static final String CANCELLED = "X";
    public static final String CONFIRMED = "C";

    public static final String WILDCARD_ALL = "TODOS";
    public static final String DEFAULT_VARIANT = "0000";
    public static final String STOCK_REASON = "CARGA_MASIVA_STOCK";
    public static final int CHUNK_SIZE = 20;
    public static final int PAGE_SIZE = 10;
    public static final int DETAIL_PAGE_SIZE = 20;

    private BulkLoadConstants() {
    }

    public static boolean isSupportedType(String type) {
        return TYPE_PRODUCT_PRICE.equals(type) || TYPE_STOCK_ENTRY.equals(type);
    }
}
