package com.hyp.repository;

import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PartnerRepository extends MongoRepository<Partner, String> {

    List<Partner> findByType(PartnerType type);

    Partner findByRestaurantsContainingAndType(String restaurantId, PartnerType type);
}
