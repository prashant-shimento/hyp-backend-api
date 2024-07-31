package com.hyp.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Feedback;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {

	Feedback findByMobileNumber(String mobileNumber);

	List<Feedback> findByHasBeenNotifiedAndBusiness(boolean hasBeenNotified, String business);

}
