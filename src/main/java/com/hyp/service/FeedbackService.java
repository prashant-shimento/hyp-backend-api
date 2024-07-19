package com.hyp.service;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.constants.Constants;
import com.hyp.entity.Feedback;
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

	@Value("${chat.io.url}")
	private String chatIOUrl;

	@Autowired
	WebClient webClient;

	public Feedback findByMobileNumber(String mobileNumber) {
		return feedbackRepository.findByMobileNumber(mobileNumber);
	}

	public List<Feedback> findByHasBeenNotified(boolean hasBeenNotified) {
		return feedbackRepository.findByHasBeenNotified(hasBeenNotified);
	}

	@Scheduled(cron = "0 0 12,18,23 * * ?")
	public void processChatIOData() {
		try {
			getDataFromChatIO(chatIOUrl);
		} catch (Exception e) {
			log.debug("An exception occurred during scheduled task processChatIOData: {}", e.getMessage());
		}
	}

	public void getDataFromChatIO(String url) {
		try {
			Mono<ChatResponse> chatResponseMono = webClient.get().uri(url).retrieve().bodyToMono(ChatResponse.class);
			ChatResponse responseData = chatResponseMono.block();
			if (responseData != null && responseData.getData() != null) {
				processChatData(responseData.getData());
			}
		} catch (Exception e) {
			log.error("Failed to retrieve data from URL: {}", url, e);
		}
	}

	private void processChatData(List<ChatData> chatDataList) {
		for (ChatData chatData : chatDataList) {
			if (isValidChatData(chatData)) {
				handleFeedback(chatData);
			}
		}
	}

	private String extractMessageToCheck(String message) {
		if (message.length() >= Constants.WELCOME_MESSAGE.length()) {
			return message.substring(0, Constants.WELCOME_MESSAGE.length());
		}
		return "";
	}

	private boolean isValidChatData(ChatData chatData) {
		String messageToCheck = extractMessageToCheck(chatData.getLastMessage());
		return chatData.getLastMessageDateTimeUTC() != null
				&& !StringUtils.isEmpty(chatData.getLastMessageDateTimeUTC())
				&& Constants.WELCOME_MESSAGE.equals(messageToCheck);
	}

	private void handleFeedback(ChatData chatData) {
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
				save(feedback);
			} else {
				updateExistingFeedback(chatData, feedback);
			}
		}
	}

	private void updateExistingFeedback(ChatData chatData, Feedback feedback) {
		String actualDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS";
	    LocalDateTime lastFeedbackAt = CommonUtils.getLocalDateTimeFromString(feedback.getLastFeedbackAt(), actualDateFormat);
		if (!CommonUtils.isToday(lastFeedbackAt.toLocalDate())) {
			feedback.setLastFeedbackAt(chatData.getLastMessageDateTimeUTC());
			feedback.setHasBeenNotified(false);
			save(feedback);
		}
	}

}
