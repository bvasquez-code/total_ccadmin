package com.ccadmin.app.shared.util;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
public final class NumberUtil {

    private NumberUtil() { /* utility class */ }

    /** Compara dos números: -1 si a<b, 0 si a==b, 1 si a>b. */
    public static int compareNumbers(Number a, Number b) {
        BigDecimal left  = toBigDecimal(a);
        BigDecimal right = toBigDecimal(b);
        return left.compareTo(right);
    }

    /** Devuelve true si a > b. */
    public static boolean isGreaterThan(Number a, Number b) {
        return compareNumbers(a, b) > 0;
    }

    /** Devuelve true si a >= b. */
    public static boolean isGreaterOrEqual(Number a, Number b) {
        return compareNumbers(a, b) >= 0;
    }

    /** Devuelve true si a < b. */
    public static boolean isLessThan(Number a, Number b) {
        return compareNumbers(a, b) < 0;
    }

    /** Devuelve true si a <= b. */
    public static boolean isLessOrEqual(Number a, Number b) {
        return compareNumbers(a, b) <= 0;
    }

    /** Devuelve true si a == b (según compareTo, ignora escala en BigDecimal). */
    public static boolean isEqualTo(Number a, Number b) {
        return compareNumbers(a, b) == 0;
    }

    /** true si n > 0 */
    public static boolean isPositive(Number n) {
        return toBigDecimal(n).compareTo(BigDecimal.ZERO) > 0;
    }

    /** true si n < 0 */
    public static boolean isNegative(Number n) {
        return toBigDecimal(n).compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * true si NO tiene parte fraccionaria.
     * Usa stripTrailingZeros para que 1.0 o 2.5000000 cuenten como enteros.
     */
    public static boolean isInteger(Number n) {
        BigDecimal bd = toBigDecimal(n).stripTrailingZeros();
        return bd.scale() <= 0;
    }

    /** true si tiene parte fraccionaria (lo contrario de isInteger). */
    public static boolean hasDecimalPart(Number n) {
        return !isInteger(n);
    }

    /**
     * Valida que a > b; si no, lanza IllegalArgumentException con mensaje claro.
     * Útil para precondiciones de negocio.
     */
    public static void requireGreaterThan(Number a, Number b, String nameA, String nameB) {
        if (!isGreaterThan(a, b)) {
            throw new IllegalArgumentException(
                    String.format("Se esperaba %s > %s, pero se recibió %s <= %s",
                            nameA, nameB, toBigDecimal(a), toBigDecimal(b)));
        }
    }

    /**
     * Convierte cualquier Number a BigDecimal con reglas seguras.
     */
    private static BigDecimal toBigDecimal(Number n) {
        Objects.requireNonNull(n, "El número no puede ser null");

        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        }
        if (n instanceof BigInteger) {
            return new BigDecimal((BigInteger) n);
        }
        if (n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long) {
            return BigDecimal.valueOf(n.longValue());
        }
        if (n instanceof Float) {
            float f = (Float) n;
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                throw new IllegalArgumentException("Float no finito: "+f);
            }
            return BigDecimal.valueOf(f);
        }
        if (n instanceof Double) {
            double d = (Double) n;
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("Double no finito: "+d);
            }
            return BigDecimal.valueOf(d);
        }

        // Fallback para otros Number (p.ej., AtomicInteger/AtomicLong)
        String s = n.toString();
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Tipo Number no soportado o formato inválido: "+n.getClass().getName()+" -> "+s, ex);
        }
    }
}
