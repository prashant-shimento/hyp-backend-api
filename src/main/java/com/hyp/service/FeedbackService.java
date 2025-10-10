package com.hyp.service;

import com.hyp.entity.Feedback;
import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import com.hyp.model.ChatResponse;
import com.hyp.model.ChatResponse.ChatData;
import com.hyp.repository.FeedbackRepository;
import com.hyp.util.CommonUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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

    public List<Feedback> findByHasBeenNotifiedAndBusiness(boolean hasBeenNotified, String business) {
        return feedbackRepository.findByHasBeenNotifiedAndBusiness(hasBeenNotified, business);
    }

    @Scheduled(cron = "0 0 12,18,23 * * ?")
    public void processChatIOData() {
        List<Partner> partners = partnerService.findByPartnerType(PartnerType.NOTIFICATION);
        log.info("Fetched {} partners for NOTIFICATION type", partners.size());
        for (Partner partner : partners) {
            log.info("Processing ChatIO data for partner: {}", partner);
            try {
                String chatIoUrl = constructChatIoUrl(partner);
                log.info("Constructed ChatIO URL: {}", chatIoUrl);
                getDataFromChatIO(chatIoUrl, partner);
                log.info("Successfully fetched and processed data for partner: {}", partner.getName());
            } catch (Exception e) {
                log.debug(
                        "An exception occurred during scheduled task processChatIOData for business {}: {}",
                        partner.getName(),
                        e.getMessage());
            }
        }
    }

    public String constructChatIoUrl(Partner partner) {
        Map<String, String> configs = partner.getConfigs();
        String accountId = configs.get("chatIOAccountId");
        String businessNumber = configs.get("chatIOBusinessNumber");

        log.info("Getting ChatIO config details for partner: {} -> {}", partner.getName(), configs);
        log.debug("ChatIO AccountId: {}, BusinessNumber: {}", accountId, businessNumber);

        if (accountId == null || businessNumber == null) {
            log.warn(
                    "Missing ChatIO config for partner: {} (accountId: {}, businessNumber: {})",
                    partner.getName(),
                    accountId,
                    businessNumber);
        }

        String baseUrl = "https://webapi.chatio.io/api/inbox/get-inbox-details-page-wise";
        String query =
                String.format("?AccountId=%s&BusinessNumber=%s&AccPwd=&lastUpdateTime", accountId, businessNumber);
        String fullUrl = baseUrl + query;

        log.debug("Constructed ChatIO URL: {}", fullUrl);
        return fullUrl;
    }

    public void getDataFromChatIO(String url, Partner partner) {
        try {
            log.info("Initiating ChatIO data fetch for partner: {}", partner.getName());
            log.debug("Requesting data from URL: {}", url);
            Mono<ChatResponse> chatResponseMono =
                    webClient.get().uri(url).retrieve().bodyToMono(ChatResponse.class);
            ChatResponse responseData = chatResponseMono.block();
            if (responseData != null && responseData.getData() != null) {
                log.info("Successfully received data from ChatIO for partner: {}", partner.getName());
                log.debug(
                        "ChatIO response data size: {}", responseData.getData().size());
                processChatData(responseData.getData(), partner);
            } else {
                log.warn("Empty or null response received from ChatIO for partner: {}", partner.getName());
            }
        } catch (Exception e) {
            log.error("Failed to retrieve data from URL: {} for business {}", url, partner.getName(), e);
        }
    }

    private void processChatData(List<ChatData> chatDataList, Partner partner) {
        log.info("Processing ChatIO data for partner: {} with {} records", partner.getName(), chatDataList.size());

        for (ChatData chatData : chatDataList) {
            log.debug("Evaluating chat data: {}", chatData);
            boolean validChatData = isValidChatData(chatData, partner);
            if (validChatData) {
                log.info("Valid chat data found for partner: {}. Proceeding to handle feedback.", partner.getName());
                handleFeedback(chatData, partner);
            } else {
                log.debug("Invalid chat data encountered for partner: {}. Skipping.", partner.getName());
            }
        }
    }

    private String extractMessageToCheck(String message, Partner partner) {
        String welcomeMessage = partner.getConfigs().getOrDefault("welcomeMessage", "default-message");
        log.debug(
                "Extracting message to check for partner: {}. Original message: '{}', Expected welcome message: '{}'",
                partner.getName(),
                message,
                welcomeMessage);
        if (message.length() >= welcomeMessage.length()) {
            String extractedMessage = message.substring(0, welcomeMessage.length());
            log.debug("Extracted message substring: '{}'", extractedMessage);
            return extractedMessage;
        }
        log.warn(
                "Message is shorter than expected welcome message for partner: {}. Returning full message: '{}'",
                partner.getName(),
                message);
        return message;
    }

    private boolean isValidChatData(ChatData chatData, Partner partner) {
        String messageToCheck = extractMessageToCheck(chatData.getLastMessage(), partner);
        String expectedMessage = partner.getConfigs().getOrDefault("welcomeMessage", "default-message");
        log.debug("Validating chat data for partner: {}", partner.getName());
        log.debug(
                "LastMessage: '{}', LastMessageDateTimeUTC: '{}'",
                chatData.getLastMessage(),
                chatData.getLastMessageDateTimeUTC());
        log.debug("Expected message: '{}', Extracted message to check: '{}'", expectedMessage, messageToCheck);
        boolean isValid = chatData.getLastMessageDateTimeUTC() != null
                && !StringUtils.isEmpty(chatData.getLastMessageDateTimeUTC())
                && expectedMessage.equals(messageToCheck);
        log.info("Chat data validity for partner {}: {}", partner.getName(), isValid);
        return isValid;
    }

    private void handleFeedback(ChatData chatData, Partner partner) {
        LocalDateTime lastMessageDate =
                CommonUtils.getLocalDateTimeFromString(chatData.getLastMessageDateTimeUTC(), "yyyy-MM-dd HH:mm:ss");
        log.debug(
                "Handling feedback for partner: {}, mobile: {}, message date: {}",
                partner.getName(),
                chatData.getMobileNumber(),
                lastMessageDate);
        if (CommonUtils.isToday(lastMessageDate.toLocalDate())) {
            log.info(
                    "Chat message is from today. Proceeding to process feedback for mobile: {}",
                    chatData.getMobileNumber());
            Feedback feedback = findByMobileNumber(chatData.getMobileNumber());
            if (feedback == null) {
                log.info(
                        "No existing feedback found. Creating new feedback entry for mobile: {}",
                        chatData.getMobileNumber());
                feedback = Feedback.builder()
                        .lastFeedback(chatData.getLastMessage())
                        .lastFeedbackAt(chatData.getLastMessageDateTimeUTC())
                        .mobileNumber(chatData.getMobileNumber())
                        .build();
                feedback.setId(CommonUtils.genId());
                feedback.setHasBeenNotified(false);
                feedback.setBusiness(partner.getConfigs().getOrDefault("Business", null));
                save(feedback);
                log.debug("New feedback saved for mobile: {}", chatData.getMobileNumber());
            } else {
                log.info("Existing feedback found. Updating feedback for mobile: {}", chatData.getMobileNumber());
                updateExistingFeedback(chatData, feedback);
            }
        } else {
            log.info("Skipping feedback as the message is not from today for mobile: {}", chatData.getMobileNumber());
        }
    }

    private void updateExistingFeedback(ChatData chatData, Feedback feedback) {
        String actualDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS";
        LocalDateTime lastFeedbackAt =
                CommonUtils.getLocalDateTimeFromString(feedback.getLastFeedbackAt(), actualDateFormat);
        log.debug(
                "Checking if existing feedback needs update. Last feedback at: {}, mobile: {}",
                lastFeedbackAt,
                feedback.getMobileNumber());
        if (!CommonUtils.isToday(lastFeedbackAt.toLocalDate())) {
            log.info("Updating feedback for mobile: {} as it's not from today", feedback.getMobileNumber());
            feedback.setLastFeedbackAt(chatData.getLastMessageDateTimeUTC());
            feedback.setHasBeenNotified(false);
            save(feedback);
            log.debug("Feedback updated and saved for mobile: {}", feedback.getMobileNumber());
        } else {
            log.info("Feedback is already from today for mobile: {}. No update needed.", feedback.getMobileNumber());
        }
    }
}
