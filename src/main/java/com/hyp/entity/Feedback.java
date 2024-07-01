package com.hyp.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "feedbacks")
@Builder
public class Feedback extends BaseEntity {

	private static final long serialVersionUID = 1L;

	private String mobileNumber;
	private String lastFeedbackAt;
	private String lastFeedback;
	private Boolean hasBeenNotified;

}