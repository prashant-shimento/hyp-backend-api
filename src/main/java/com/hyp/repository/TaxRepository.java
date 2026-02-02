package com.hyp.repository;

import com.hyp.entity.Tax;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaxRepository extends MongoRepository<Tax, String> {
    List<Tax> findAllByIdIn(List<String> requestTaxIds);
}
