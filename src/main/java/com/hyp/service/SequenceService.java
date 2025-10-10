package com.hyp.service;

import com.hyp.entity.Sequence;
import com.hyp.repository.SequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SequenceService {

    @Autowired
    SequenceRepository sequenceRespository;

    public String generateSequence(String sequenceName) {
        Sequence sequence = sequenceRespository.findById(sequenceName).orElse(null);
        if (sequence == null) {
            sequence = new Sequence();
            sequence.setId(sequenceName);
            sequence.setSequence(100000);
        }
        long nextValue = sequence.getSequence() + 1;
        sequence.setSequence(nextValue);
        sequenceRespository.save(sequence);
        return String.valueOf(nextValue);
    }
}
