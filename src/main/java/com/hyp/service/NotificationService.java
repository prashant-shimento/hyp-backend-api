package com.hyp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.entity.Feedback;
import com.hyp.entity.Partner;
import com.hyp.request.FacebookMessageRequest;
import com.hyp.enums.PartnerType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NotificationService {

	@Autowired
	private FeedbackService feedbackService;

	@Autowired
	private PartnerService partnerService;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Scheduled(cron = "0 0 12,18,23 * * ?")
	public void sendFeedbackMessageAndUpdateFlag() {
		List<Partner> partners = partnerService.findByPartnerType(PartnerType.NOTIFICATION);
		for (Partner partner : partners) {
			List<Feedback> feedbacks = feedbackService.findByHasBeenNotifiedAndBusiness(false, partner.getConfigs().getOrDefault("Business", null));
			log.debug("Found feedbacks: " + feedbacks.size());
			for (Feedback feedback : feedbacks) {
				sendFeedbackMessage(feedback.getMobileNumber(), partner);
				feedback.setHasBeenNotified(true);
				feedback.setLastFeedbackAt(LocalDateTime.now().toString());
				feedbackService.save(feedback);
			}
		}
	}

	public void sendFeedbackMessage(String mobileNumber, Partner partner) {
		try {
			

			String apiUrl = constructFacebookGraphApiUrl(partner);
			String accessToken = partner.getConfigs().get("AccessToken");
			String templateName = partner.getConfigs().get("templateName");

			WebClient webClient = webClientBuilder.baseUrl(apiUrl)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).build();
            FacebookMessageRequest facebookMessageBody = FacebookMessageRequest.builder()
                    .messagingProduct("whatsapp")
                    .recipientType("individual")
                    .to(mobileNumber)
                    .type("template")
                    .template(FacebookMessageRequest.Template.builder()
                            .name(templateName)
                            .language(new FacebookMessageRequest.Language("en"))
                            .build())
                    .build();
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

	private String constructFacebookGraphApiUrl(Partner partner) {
		String facebookBusinessId = partner.getConfigs().get("faceBookBusinessId");
		String baseUrl = "https://graph.facebook.com/v19.0";
		String endpoint = String.format("/%s/messages", facebookBusinessId);
		return baseUrl + endpoint;
	}
	
	
	
}
