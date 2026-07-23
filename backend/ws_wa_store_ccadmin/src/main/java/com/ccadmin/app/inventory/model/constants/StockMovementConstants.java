package com.ccadmin.app.inventory.model.constants;

public final class StockMovementConstants {
    public static final String PROCESS_ORIGINAL = "O"; // O = Operacion original
    public static final String MODE_DIRECT = "D"; // D = Movimiento directo
    public static final String MODE_UNAVAILABLE = "N"; // N = Paso por no disponible
    public static final String RESOLUTION_RELEASE = "L"; // L = Liberar a disponible
    public static final String RESOLUTION_WITHDRAW = "B"; // B = Baja definitiva
    public static final String RESOLUTION_DESTROY = "D"; // D = Destruccion
    public static final String RESOLUTION_MAINTAIN = "M"; // M = Mantener no disponible
    public static final String SOURCE_ENTRY = "stock_entry_head";
    public static final String SOURCE_EXIT = "stock_exit_head";
    public static final String EVENT_CONFIRMATION = "CONFIRMATION";
    public static final String EVENT_RESOLUTION_PREFIX = "RESOLUTION_";

    private StockMovementConstants() {
    }
}
