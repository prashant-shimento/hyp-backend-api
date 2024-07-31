package com.hyp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.entity.Feedback;
import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import com.hyp.model.ChatResponse;
import com.hyp.model.ChatResponse.ChatData;
import com.hyp.repository.FeedbackRepository;
import com.hyp.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class FeedbackService extends BaseServiceImpl<Feedback, String> {

	@Autowired
	FeedbackRepository feedbackRepository;

	@Autowired
	PartnerService partnerService;

	@Autowired
	WebClient webClient;

	public Feedback findByMobileNumber(String mobileNumber) {
		return feedbackRepository.findByMobileNumber(mobileNumber);
	}

	public List<Feedback> findByHasBeenNotifiedAndBusiness(boolean hasBeenNotified,String business) {
		return feedbackRepository.findByHasBeenNotifiedAndBusiness(hasBeenNotified,business);
	}

	@Scheduled(cron = "0 0 12,18,23 * * ?")
	public void processChatIOData() {
		List<Partner> partners = partnerService.findByPartnerType(PartnerType.NOTIFICATION);
		for (Partner partner : partners) {
			try {
				String chatIoUrl = constructChatIoUrl(partner);
				getDataFromChatIO(chatIoUrl, partner);

			} catch (Exception e) {
				log.debug("An exception occurred during scheduled task processChatIOData for business {}: {}",
						partner.getName(), e.getMessage());
			}
		}
	}

	public String constructChatIoUrl(Partner partner) {
		Map<String, String> configs = partner.getConfigs();
		String accountId = configs.get("chatIOAccountId");
		String businessNumber = configs.get("chatIOBusinessNumber");
		String baseUrl = "https://webapi.chatio.io/api/inbox/get-inbox-details-page-wise";
		String query = String.format("?AccountId=%s&BusinessNumber=%s&AccPwd=&lastUpdateTime", accountId,
				businessNumber);
		return baseUrl + query;
	}

	public void getDataFromChatIO(String url, Partner partner) {
		try {
			Mono<ChatResponse> chatResponseMono = webClient.get().uri(url).retrieve().bodyToMono(ChatResponse.class);
			ChatResponse responseData = chatResponseMono.block();
			if (responseData != null && responseData.getData() != null) {
				processChatData(responseData.getData(), partner);
			}
		} catch (Exception e) {
			log.error("Failed to retrieve data from URL: {} for business {}", url, partner.getName(), e);
		}
	}

	private void processChatData(List<ChatData> chatDataList, Partner partner) {
		for (ChatData chatData : chatDataList) {
			boolean validChatData = isValidChatData(chatData, partner);
			if (validChatData) {
				handleFeedback(chatData, partner);
			}
		}
	}

	private String extractMessageToCheck(String message, Partner partner) {
		String welcomeMessage = partner.getConfigs().getOrDefault("welcomeMessage", "default-message");
		if (message.length() >= welcomeMessage.length()) {
			return message.substring(0, welcomeMessage.length());
		}
		return message;
	}

	private boolean isValidChatData(ChatData chatData, Partner partner) {
		String messageToCheck = extractMessageToCheck(chatData.getLastMessage(), partner);
		String expectedMessage = partner.getConfigs().getOrDefault("welcomeMessage", "default-message");
		return chatData.getLastMessageDateTimeUTC() != null
				&& !StringUtils.isEmpty(chatData.getLastMessageDateTimeUTC()) && expectedMessage.equals(messageToCheck);
	}

	private void handleFeedback(ChatData chatData, Partner partner) {
		LocalDateTime lastMessageDate = CommonUtils.getLocalDateTimeFromString(chatData.getLastMessageDateTimeUTC(),
				"yyyy-MM-dd HH:mm:ss");
		if (CommonUtils.isToday(lastMessageDate.toLocalDate())) {
			Feedback feedback = findByMobileNumber(chatData.getMobileNumber());
			if (feedback == null) {
				feedback = Feedback.builder().lastFeedback(chatData.getLastMessage())
						.lastFeedbackAt(chatData.getLastMessageDateTimeUTC()).mobileNumber(chatData.getMobileNumber())
						.build();
				feedback.setId(CommonUtils.genId());
				feedback.setHasBeenNotified(false);
				feedback.setBusiness(partner.getConfigs().getOrDefault("Business", null));
				save(feedback);
			} else {
				updateExistingFeedback(chatData, feedback);
			}
		}
	}

	private void updateExistingFeedback(ChatData chatData, Feedback feedback) {
		String actualDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS";
		LocalDateTime lastFeedbackAt = CommonUtils.getLocalDateTimeFromString(feedback.getLastFeedbackAt(),
				actualDateFormat);

		if (!CommonUtils.isToday(lastFeedbackAt.toLocalDate())) {
			feedback.setLastFeedbackAt(chatData.getLastMessageDateTimeUTC());
			feedback.setHasBeenNotified(false);
			save(feedback);
		}
	}

	
}
