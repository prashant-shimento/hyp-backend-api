package com.hyp.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;

public interface PartnerRepository extends MongoRepository<Partner, String> {
	
	List<Partner> findByType(PartnerType type);

}
