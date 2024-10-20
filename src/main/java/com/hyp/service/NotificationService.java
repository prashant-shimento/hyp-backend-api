package com.hyp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.constants.Constants;
import com.hyp.entity.Feedback;
import com.hyp.entity.Partner;
import com.hyp.request.FacebookMessageRequest;
import com.hyp.request.FacebookMessageRequest.Component;
import com.hyp.request.FacebookMessageRequest.Language;
import com.hyp.request.FacebookMessageRequest.Parameter;
import com.hyp.enums.PartnerType;
import com.hyp.exception.NotificationException;

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
	
	@Autowired
	private MetaService metaService;

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
	
	@Async
	public void sendOrderNotification(String mobile, String templateName, List<String> parameters, String buttonParam) {
		try {
			List<Parameter> params = new ArrayList<>();
			for (String param : parameters) {
				params.add(Parameter.builder().type("text").text(param).build());
			}
			Component bodyComponent = Component.builder().type("body").parameters(params).build();

			List<Component> components = new ArrayList<>();
			components.add(bodyComponent);
			if (buttonParam != null) {
				Component buttonComponent = Component.builder().type("button").subType("url").index("0")
						.parameters(Arrays.asList(Parameter.builder().type("text").text(buttonParam).build())).build();
				components.add(buttonComponent);
			}

			FacebookMessageRequest messageRequest = FacebookMessageRequest.builder()
					.messagingProduct(Constants.META_WHATSAPP).to(mobile).type(Constants.TEMPLATE)
					.template(FacebookMessageRequest.Template.builder().name(templateName)
							.language(Language.builder().code("en").build()).components(components).build())
					.build();

			metaService.sendMessage(messageRequest);
		} catch (NotificationException e) {
			log.error("Error occured in sendOrderNotification " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Async
	public void sendOrderNotification(String mobile, String templateName, List<String> parameters) {
		try {
			List<Parameter> params = new ArrayList<>();
			for (String param : parameters) {
				params.add(Parameter.builder().type("text").text(param).build());
			}
			Component bodyComponent = Component.builder().type("body").parameters(params).build();

			List<Component> components = new ArrayList<>();
			components.add(bodyComponent);

			FacebookMessageRequest messageRequest = FacebookMessageRequest.builder()
					.messagingProduct(Constants.META_WHATSAPP).to(mobile).type(Constants.TEMPLATE)
					.template(FacebookMessageRequest.Template.builder().name(templateName)
							.language(Language.builder().code("en").build()).components(components).build())
					.build();

			metaService.sendMessage(messageRequest);
		} catch (NotificationException e) {
			log.error("Error occured in sendOrderNotification " + e.getMessage());
			e.printStackTrace();
		}
	}
	
}
