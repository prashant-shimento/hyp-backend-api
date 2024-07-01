package com.hyp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.entity.Feedback;
import com.hyp.model.FacebookMessage;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NotificationService {

	@Value("${facebook.graph.api.url}")
	private String facebookGraphApiUrl;

	@Value("${facebook.access.token}")
	private String accessToken;

	@Autowired
	private FeedbackService feedbackService;

	@Scheduled(cron = "0 0 12,18,23 * * ?")
	public void sendFeedbackMessageAndUpdateFlag() {
		List<Feedback> feedbacks = feedbackService.findByHasBeenNotified(false);
		for (Feedback feedback : feedbacks) {
			sendFeedbackMessage(feedback.getMobileNumber());
			feedback.setHasBeenNotified(true);
			feedback.setLastFeedbackAt(LocalDateTime.now().toString());
			feedbackService.save(feedback);
		}
	}

	public void sendFeedbackMessage(String mobileNumber) {
		try {
			FacebookMessage facebookMessageBody = FacebookMessage.builder().messaging_product("whatsapp")
					.recipient_type("individual").to(mobileNumber).type("template").template(FacebookMessage.Template
							.builder().name("rasyumm").language(new FacebookMessage.Template.Language("en")).build())
					.build();

			WebClient webClient = WebClient.builder().baseUrl(facebookGraphApiUrl)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).build();

			Mono<String> responseMono = webClient.post().bodyValue(facebookMessageBody).retrieve()
					.bodyToMono(String.class);

			responseMono.subscribe(response -> {
				log.info("sendFeedbackMessage Response: " + response);
			}, error -> {
				log.error("Error response sendFeedbackMessage: " + error.getMessage());
			});

		} catch (Exception e) {
			log.error("Exception occurred on sendFeedbackMessage: ", e);
		}
	}
}
