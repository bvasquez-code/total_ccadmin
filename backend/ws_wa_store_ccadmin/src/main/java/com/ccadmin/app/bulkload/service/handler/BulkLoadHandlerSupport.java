package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.dto.BulkLoadErrorDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadSourceRowDto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

final class BulkLoadHandlerSupport {
    private BulkLoadHandlerSupport() {
    }

    static Map<String, Object> payload(BulkLoadSourceRowDto row) {
        return row == null || row.Payload == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(row.Payload);
    }

    static Object sourceValue(BulkLoadSourceRowDto row, String field) {
        if (row != null && row.Payload != null
                && row.Payload.containsKey(field)) {
            return row.Payload.get(field);
        }
        if ("ProductCod".equals(field)) return row == null ? null : row.ProductCod;
        if ("Value".equals(field)) return row == null ? null : row.Value;
        return null;
    }

    static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = text(value);
        return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    static int integer(Object value) {
        return decimal(value).intValueExact();
    }

    static int sourceRow(Integer rowNumber) {
        return rowNumber == null || rowNumber < 1 ? 1 : rowNumber;
    }

    static BulkLoadErrorDto error(String sheet,
                                  int rowNumber,
                                  String field,
                                  Object value,
                                  String code,
                                  String detail) {
        return new BulkLoadErrorDto(
                sheet, rowNumber, null, field, text(value), code, detail
        );
    }
}
