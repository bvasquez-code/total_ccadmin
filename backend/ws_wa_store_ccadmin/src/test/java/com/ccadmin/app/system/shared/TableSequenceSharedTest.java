package com.ccadmin.app.system.shared;

import com.ccadmin.app.system.service.TableSequenceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TableSequenceSharedTest {

    @Test
    void nextAvailableCodeSkipsBlankAndExistingCodes() {
        TableSequenceService tableSequenceService = mock(TableSequenceService.class);
        TableSequenceShared tableSequenceShared = new TableSequenceShared(tableSequenceService);
        when(tableSequenceService.getNextCode("brand"))
                .thenReturn("", "BR001", "BR002");

        String result = tableSequenceShared.getNextAvailableCode(
                "brand", code -> code.equals("BR001")
        );

        assertEquals("BR002", result);
        verify(tableSequenceService, times(3)).getNextCode("brand");
    }
}
