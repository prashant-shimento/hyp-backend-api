package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.Sequence;
import com.hyp.repository.SequenceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SequenceServiceTest {

    @Mock
    private SequenceRepository sequenceRespository;

    @InjectMocks
    private SequenceService sequenceService;

    private Sequence sequence;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        sequence = new Sequence();
        sequence.setId("address_sequence");
        sequence.setSequence(11111);
    }

    // UTC for GenerateSequence method -success
    @Test
    public void testForGenerateSequence_SUCCESS() {
        String sequenceName = sequence.getId();
        when(sequenceRespository.findById(sequenceName)).thenReturn(Optional.of(sequence));

        String result = sequenceService.generateSequence(sequenceName);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("11112");
        verify(sequenceRespository, times(1)).findById(sequenceName);
    }

    // UTC for GenerateSequence method - DOES not exist
    @Test
    public void testForGenerateSequence_SequenceIdDoesNotExist() {
        String sequenceName = sequence.getId();
        when(sequenceRespository.findById(sequenceName)).thenReturn(Optional.empty());

        String result = sequenceService.generateSequence(sequenceName);

        assertThat(result).isEqualTo("100001");
        verify(sequenceRespository, times(1)).findById(sequenceName);
    }

    // UTC for Generate sequence - Exception
    @Test
    public void testForGenerateSequence_Exception() {
        String sequenceName = sequence.getId();
        when(sequenceRespository.findById(sequenceName)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> sequenceService.generateSequence(sequenceName));

        //			String result = sequenceService.generateSequence(sequenceName);
        assertThat(exception.getMessage()).isEqualTo("Server error");
        assertThat(exception).isInstanceOf(RuntimeException.class);

        verify(sequenceRespository, times(1)).findById(sequenceName);
    }
}
