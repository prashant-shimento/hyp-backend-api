package com.hyp.repository;

import com.hyp.entity.Feedback;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {

    Feedback findByMobileNumber(String mobileNumber);

    List<Feedback> findByHasBeenNotifiedAndBusiness(boolean hasBeenNotified, String business);
}
