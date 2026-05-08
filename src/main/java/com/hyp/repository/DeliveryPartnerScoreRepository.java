package com.hyp.repository;

import com.hyp.entity.DeliveryPartnerScore;
import com.hyp.enums.DeliveryPartner;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryPartnerScoreRepository extends MongoRepository<DeliveryPartnerScore, String> {

    Optional<DeliveryPartnerScore> findByRestaurantIdAndPartnerAndDate(
            String restaurantId, DeliveryPartner partner, LocalDate date);

    List<DeliveryPartnerScore> findByRestaurantIdAndPartnerOrderByDateDesc(
            String restaurantId, DeliveryPartner partner);

    List<DeliveryPartnerScore> findTop30ByRestaurantIdAndPartnerOrderByDateDesc(
            String restaurantId, DeliveryPartner partner);

    List<DeliveryPartnerScore> findByPartnerAndDateBetweenOrderByDateDesc(
            DeliveryPartner partner, LocalDate from, LocalDate to);
}
