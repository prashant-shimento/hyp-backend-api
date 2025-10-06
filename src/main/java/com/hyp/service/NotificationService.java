package com.hyp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.client.OneSignalClient;
import com.hyp.constants.Constants;
import com.hyp.entity.Feedback;
import com.hyp.entity.Partner;
import com.hyp.entity.User;
import com.hyp.enums.PartnerType;
import com.hyp.exception.NotificationException;
import com.hyp.exception.OneSignalException;
import com.hyp.request.FacebookMessageRequest;
import com.hyp.request.FacebookMessageRequest.Component;
import com.hyp.request.FacebookMessageRequest.Language;
import com.hyp.request.FacebookMessageRequest.Parameter;
import com.hyp.request.OneSignalNotificationAlias;
import com.hyp.request.OneSignalNotificationRequest;

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
	private RedisService redisService;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	private OneSignalClient oneSignalClient;

	@Autowired
	private MetaService metaService;

	@Autowired
	private UserService userService;

	@Value("${onesignal.partner.app.id}")
	private String partberAppId;

	@Scheduled(cron = "0 10 12,18,23 * * ?")
	public void sendFeedbackMessageAndUpdateFlag() {
		log.info("Starting feedback message dispatch process");
		List<Partner> partners = partnerService.findByPartnerType(PartnerType.NOTIFICATION);
		log.info("Found {} partners with NOTIFICATION type", partners.size());
		for (Partner partner : partners) {
			log.debug("Processing partner: {}", partner.getName());
			List<Feedback> feedbacks = feedbackService.findByHasBeenNotifiedAndBusiness(false,
					partner.getConfigs().getOrDefault("Business", null));
			log.info("Found {} unnotified feedback(s) for partner: {}", feedbacks.size(), partner.getName());
			for (Feedback feedback : feedbacks) {
				log.debug("About to send feedback message to mobile: {} for partner: {}", feedback.getMobileNumber(),
						partner.getName());
				sendFeedbackMessage(feedback.getMobileNumber(), partner);
				log.info("Sent feedback message to mobile: {} for partner: {}", feedback.getMobileNumber(),
						partner.getName());
				feedback.setHasBeenNotified(true);
				feedback.setLastFeedbackAt(LocalDateTime.now().toString());
				feedbackService.save(feedback);
				log.debug("Marked feedback as notified and saved for mobile: {}", feedback.getMobileNumber());
			}
		}
		log.info("Feedback message dispatch process completed");
	}

	public void sendFeedbackMessage(String mobileNumber, Partner partner) {
		log.info("Starting sendFeedbackMessage for partner: {} and mobile: {}", partner.getName(), mobileNumber);
		try {
			String apiUrl = constructFacebookGraphApiUrl(partner);
			log.debug("Constructed Facebook Graph API URL: {}", apiUrl);
			String accessToken = partner.getConfigs().get("AccessToken");
			log.debug("Retrieved accessToken of length: {}", accessToken != null ? accessToken.length() : 0);
			String templateName = partner.getConfigs().get("templateName");
			log.debug("Using templateName: {}", templateName);

			WebClient webClient = webClientBuilder.baseUrl(apiUrl)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).build();
			log.info("Initialized WebClient for POST to {}", apiUrl);
			FacebookMessageRequest facebookMessageBody = FacebookMessageRequest.builder().messagingProduct("whatsapp")
					.recipientType("individual").to(mobileNumber).type("template")
					.template(FacebookMessageRequest.Template.builder().name(templateName)
							.language(new FacebookMessageRequest.Language("en")).build())
					.build();
			log.debug("FacebookMessageRequest payload prepared: {}", facebookMessageBody);
			Mono<String> responseMono = webClient.post().bodyValue(facebookMessageBody).retrieve()
					.bodyToMono(String.class);
			log.info("Dispatching feedback message request to WhatsApp API");
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
		log.debug("Retrieving Facebook Business ID for partner {}: {}", partner.getName(), facebookBusinessId);
		String baseUrl = "https://graph.facebook.com/v19.0";
		log.info("Using Facebook Graph API base URL: {}", baseUrl);
		String endpoint = String.format("/%s/messages", facebookBusinessId);
		log.debug("Constructed endpoint path: {}", endpoint);
		String fullUrl = baseUrl + endpoint;
		log.info("Full Facebook Graph API URL for partner {}: {}", partner.getName(), fullUrl);
		return fullUrl;
	}

	@Async
	public void sendNotification(String mobile, String templateName, List<String> parameters, String buttonParam) {
		log.info("Starting sendNotification for mobile: {} with template: {}", mobile, templateName);
		try {
			log.debug("Building parameters list from: {}", parameters);
			List<Parameter> params = new ArrayList<>();
			for (String param : parameters) {
				params.add(Parameter.builder().type("text").text(param).build());
			}
			log.debug("Parameters built: {}", params);
			Component bodyComponent = Component.builder().type("body").parameters(params).build();
			log.debug("Created body component: {}", bodyComponent);
			List<Component> components = new ArrayList<>();
			components.add(bodyComponent);
			if (buttonParam != null) {
				log.debug("Building button component with buttonParam: {}", buttonParam);
				Component buttonComponent = Component.builder().type("button").subType("url").index("0")
						.parameters(
								Collections.singletonList(Parameter.builder().type("text").text(buttonParam).build()))
						.build();
				components.add(buttonComponent);
				log.debug("Button component added: {}", buttonComponent);
			} else {
				log.debug("No buttonParam provided; skipping button component");
			}
			FacebookMessageRequest messageRequest = FacebookMessageRequest.builder()
					.messagingProduct(Constants.META_WHATSAPP).to(mobile).type(Constants.TEMPLATE)
					.template(FacebookMessageRequest.Template.builder().name(templateName)
							.language(Language.builder().code("en").build()).components(components).build())
					.build();
			log.info("Constructed FacebookMessageRequest: {}", messageRequest);
			if (!redisService.isNotificationServiceEnabled()) {
				log.info("Notification service is disabled.");
				return;
			}
			log.info("Notification service enabled; sending messageRequest");
			metaService.sendMessage(messageRequest);
			log.info("sendNotification completed successfully for mobile: {}", mobile);
		} catch (NotificationException e) {
			log.error("Error occurred in sendOrderNotification {}", e.getMessage());
		}
	}

	@Async
	public void sendUserNotification(String mobile, String templateName, List<String> parameters) {
		log.info("Invoking sendUserNotification for mobile: {}, template: {}", mobile, templateName);
		log.debug("Parameters passed to sendUserNotification: {}", parameters);
		try {
			sendNotification(mobile, templateName, parameters);
			log.info("Completed sendUserNotification for mobile: {}", mobile);
		} catch (Exception e) {
			log.error("Exception in sendUserNotification for mobile: {}", mobile, e);
		}
	}

	@Async
	public void sendInternalGroupNotification(String templateName, List<String> parameters) {
		log.info("Starting internal group notification for template: {} with parameters: {}", templateName, parameters);
		String alertMobileNum = redisService.getAlertUsers();
		log.debug("Retrieved alert users string: {}", alertMobileNum);
		String[] mobileNumbers = alertMobileNum.split(",");
		for (String mobile : mobileNumbers) {
			log.debug("Sending internal group notification to mobile: {}", mobile);
			sendNotification(mobile, templateName, parameters);
			log.info("Dispatched internal group notification to: {}", mobile);
		}
		log.info("Internal group notification process completed");
	}

	@Async
	public void sendNotification(String mobile, String templateName, List<String> parameters) {
		log.info("Initiating sendNotification for mobile: {} with template: {}", mobile, templateName);
		try {
			log.debug("Building message parameters from list: {}", parameters);
			List<Parameter> params = new ArrayList<>();
			for (String param : parameters) {
				params.add(Parameter.builder().type("text").text(param).build());
			}
			log.debug("Constructed Parameter objects: {}", params);
			Component bodyComponent = Component.builder().type("body").parameters(params).build();
			log.debug("Created body component: {}", bodyComponent);
			List<Component> components = new ArrayList<>();
			components.add(bodyComponent);
			log.debug("Assembled components list: {}", components);
			FacebookMessageRequest messageRequest = FacebookMessageRequest.builder()
					.messagingProduct(Constants.META_WHATSAPP).to(mobile).type(Constants.TEMPLATE)
					.template(FacebookMessageRequest.Template.builder().name(templateName)
							.language(Language.builder().code("en").build()).components(components).build())
					.build();
			log.info("FacebookMessageRequest constructed for mobile: {}", mobile);
			if (!redisService.isNotificationServiceEnabled()) {
				log.info("Notification service is disabled for sendNotification.");
				return;
			}
			log.info("Notification service enabled; sending message to mobile: {}", mobile);
			metaService.sendMessage(messageRequest);
			log.info("sendNotification completed successfully for mobile: {}", mobile);
		} catch (NotificationException e) {
			log.error("Error occurred in sendNotification {}", e.getMessage());
		}
	}

	public void sendOneSignalNotification(OneSignalNotificationRequest oneSignalNotificationRequest)
			throws OneSignalException {
		oneSignalClient.sendNotification(oneSignalNotificationRequest);
	}

	public void sendOneSignalNotificationForPartner(OneSignalNotificationRequest oneSignalNotificationRequest)
			throws OneSignalException {
		oneSignalClient.sendNotificationforPartner(oneSignalNotificationRequest);
	}

	public void sendTestNotification(String restaurantId) {
		User user = userService.findByRestaurantId(restaurantId);
		if (user == null) {
			log.warn("No user found for restaurantId={}", restaurantId);
			return;
		}

		OneSignalNotificationRequest request = OneSignalNotificationRequest.builder().targetChannel("push")
				.includeAliases(OneSignalNotificationAlias.builder().externalId(List.of(user.getId())).build())
				.appId(partberAppId).contents(Map.of("en", "OneSignal notification is working fine")).build();

		try {
			sendOneSignalNotificationForPartner(request);
			log.info("Test notification sent to userId={} for restaurantId={}", user.getId(), restaurantId);
		} catch (OneSignalException e) {
			log.error("Error occurred in sending push notification {}", request, e);
		}
	}
}
