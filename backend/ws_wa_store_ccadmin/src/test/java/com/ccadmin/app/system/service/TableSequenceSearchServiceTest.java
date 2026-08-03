package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.TableSequenceEntity;
import com.ccadmin.app.system.repository.TableSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableSequenceSearchServiceTest {

    @Mock
    private TableSequenceRepository tableSequenceRepository;
    @InjectMocks
    private TableSequenceSearchService tableSequenceSearchService;

    @Test
    void findsSequenceByTableType() {
        TableSequenceEntity sequence = new TableSequenceEntity();
        sequence.SequenceTableType = "client";
        when(tableSequenceRepository.findBySequenceTableType("client")).thenReturn(sequence);

        TableSequenceEntity result = tableSequenceSearchService.findById(" client ");

        assertSame(sequence, result);
        verify(tableSequenceRepository).findBySequenceTableType("client");
    }
}
