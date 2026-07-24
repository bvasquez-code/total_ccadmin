package com.ccadmin.app.bulkload.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BulkLoadProcessService {
    private final BulkLoadChunkService chunkService;

    public BulkLoadProcessService(BulkLoadChunkService chunkService) {
        this.chunkService = chunkService;
    }

    public void process(String code) {
        try {
            if (!chunkService.start(code)) return;
            while (true) {
                String preparedResourceCode = chunkService.prepareNextChunk(code);
                int processedDetails = chunkService.processNextChunk(
                        code, preparedResourceCode
                );
                if (processedDetails == 0) break;
                // Cada iteracion confirma una transaccion independiente de hasta 20 detalles.
            }
            chunkService.finish(code);
        } catch (Exception exception) {
            log.error("Error procesando carga masiva {}", code, exception);
            try {
                chunkService.fail(code, exception);
            } catch (Exception failException) {
                log.error("No se pudo registrar el error de la carga masiva {}", code, failException);
            }
        }
    }
}
