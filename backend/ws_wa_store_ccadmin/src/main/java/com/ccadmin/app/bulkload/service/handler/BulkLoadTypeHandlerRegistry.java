package com.ccadmin.app.bulkload.service.handler;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BulkLoadTypeHandlerRegistry {
    private final Map<String, BulkLoadTypeHandler> handlerMap;

    public BulkLoadTypeHandlerRegistry(List<BulkLoadTypeHandler> handlerList) {
        Map<String, BulkLoadTypeHandler> result = new LinkedHashMap<>();
        for (BulkLoadTypeHandler handler : handlerList) {
            String type = normalize(handler.type());
            if (result.put(type, handler) != null) {
                throw new IllegalStateException(
                        "Existe mas de un manejador para " + type
                );
            }
        }
        this.handlerMap = Map.copyOf(result);
    }

    public BulkLoadTypeHandler getRequired(String type) {
        String normalizedType = normalize(type);
        BulkLoadTypeHandler handler = handlerMap.get(normalizedType);
        if (handler == null) {
            throw new IllegalArgumentException(
                    "Tipo de carga no soportado: " + normalizedType
            );
        }
        return handler;
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }
}
