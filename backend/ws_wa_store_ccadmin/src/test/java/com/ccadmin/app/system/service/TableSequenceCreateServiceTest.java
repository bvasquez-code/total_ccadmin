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
class TableSequenceCreateServiceTest {

    @Mock
    private TableSequenceRepository tableSequenceRepository;
    @InjectMocks
    private TableSequenceCreateService tableSequenceCreateService;

    @Test
    void updatesSequenceValueWithoutTreatingItAsPrimaryKey() {
        TableSequenceEntity sequence = validSequence();
        sequence.SequenceTrx = 25L;
        when(tableSequenceRepository.save(sequence)).thenReturn(sequence);

        TableSequenceEntity result = tableSequenceCreateService.save(sequence);

        assertSame(sequence, result);
        verify(tableSequenceRepository).save(sequence);
    }

    private TableSequenceEntity validSequence() {
        TableSequenceEntity sequence = new TableSequenceEntity();
        sequence.SequenceTableType = "client";
        sequence.SequenceTrx = 0L;
        sequence.Prefix = "CL";
        sequence.length = 8;
        sequence.UsePrefix = "S";
        return sequence;
    }
}
