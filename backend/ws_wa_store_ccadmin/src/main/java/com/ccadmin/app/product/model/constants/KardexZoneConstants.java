package com.ccadmin.app.product.model.constants;

public final class KardexZoneConstants {

    public static final String ZONE_PHYSICAL = "PHYSICAL";
    public static final String ZONE_RESERVED = "RESERVED";
    public static final String ZONE_UNAVAILABLE = "UNAVAILABLE";
    public static final String TYPE_OPERATION_SUBTRACT = "R";
    public static final String TYPE_OPERATION_ADD = "S";

    public static boolean isSupported(String zone) {
        return ZONE_PHYSICAL.equals(zone)
                || ZONE_RESERVED.equals(zone)
                || ZONE_UNAVAILABLE.equals(zone);
    }

    private KardexZoneConstants() {
    }
}
