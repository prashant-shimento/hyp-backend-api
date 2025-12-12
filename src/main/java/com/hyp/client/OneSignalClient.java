package com.hyp.client;

import com.hyp.exception.OneSignalException;
import com.hyp.request.IdentityRequest;
import com.hyp.request.IdentityRequest.Identity;
import com.hyp.request.OneSignalNotificationRequest;
import com.hyp.util.LoggingUtils;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class OneSignalClient {

    @Value("${onesignal.api.url}")
    private String baseUrl;

    @Value("${onesignal.api.key}")
    private String apiKey;

    @Value("${onesignal.partner.api.key}")
    private String partnerApiKey;

    @Value("${onesignal.partner.app.id}")
    private String partnerAppId;

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DURATION = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF_DURATION = Duration.ofSeconds(6);

    private WebClient getClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private WebClient getClientForPartner() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + partnerApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private <T> Mono<T> executeWithRetry(Mono<T> mono) {
        return mono.retryWhen(Retry.backoff(MAX_RETRIES, BACKOFF_DURATION)
                .maxBackoff(MAX_BACKOFF_DURATION)
                .filter(throwable -> throwable instanceof WebClientResponseException)
                .doBeforeRetry(signal -> log.warn("Retrying OneSignal call, attempt {}", signal.totalRetries() + 1)));
    }

    public Mono<Void> registerUser(String userId, String oneSignalId) {
        String endpoint = baseUrl + "/apps/" + partnerAppId + "/users/by/onesignal_id/" + oneSignalId + "/identity";

        log.info("registerUser endpoint {}", endpoint);
        IdentityRequest identityRequest = IdentityRequest.builder()
                .identity(Identity.builder().externalId(userId).build())
                .build();
        LoggingUtils.logRequest("registerUser", identityRequest);
        return executeWithRetry(getClientForPartner()
                .patch()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(identityRequest))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(unused -> log.info("registerUser completed successfully for user: {}", userId))
                .doOnError(e -> log.error("Error in registerUser for user {}: {}", userId, e.getMessage(), e))
                .onErrorMap(e -> {
                    log.error("Final failure after retries for user {}. Alerting about failure...", userId);
                    return new OneSignalException("registerUser" + " " + userId, e.getMessage());
                }));
    }

    public Mono<Void> deleteUser(String userId, String oneSignalId) {
        String endpoint = baseUrl + "/apps/" + partnerAppId + "/users/by/onesignal_id/" + oneSignalId;

        log.info("deleteUser endpoint {}", endpoint);

        return executeWithRetry(getClientForPartner()
                .delete()
                .uri(endpoint)
                .header("Authorization", "Key " + partnerApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class).flatMap(body -> {
                            log.error(
                                    "Failed deleteUser {} status {} body {}",
                                    oneSignalId,
                                    resp.statusCode().value(),
                                    body);
                            return Mono.error(new OneSignalException("deleteUser " + oneSignalId, body));
                        }))
                .bodyToMono(Void.class)
                .doOnSuccess(unused -> log.info("deleteUser completed successfully for onesignalId: {}", oneSignalId))
                .doOnError(e -> log.error("Error in deleteUser for onesignalId {}: {}", oneSignalId, e.getMessage(), e))
                .onErrorMap(e -> {
                    log.error("Final failure after retries for onesignalId {}. Alerting about failure...", oneSignalId);
                    return new OneSignalException("deleteUser " + oneSignalId, e.getMessage());
                }));
    }

    public void sendNotification(OneSignalNotificationRequest oneSignalNotificationRequest) throws OneSignalException {
        String endpoint = "/notifications?c=push";
        LoggingUtils.logRequest("sendNotification", oneSignalNotificationRequest);

        try {
            executeWithRetry(getClient()
                            .post()
                            .uri(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(oneSignalNotificationRequest))
                            .retrieve()
                            .bodyToMono(Void.class))
                    .doOnSuccess(unused -> log.info("OneSignal sendNotification completed successfully"))
                    .onErrorMap(e -> {
                        log.error("Final failure after retries. Error: {}", e.getMessage(), e);
                        return new OneSignalException(
                                "Failed to sendNotification OneSignal " + oneSignalNotificationRequest.toString(),
                                e.getMessage());
                    })
                    .block();
        } catch (Exception e) {
            throw new OneSignalException("Unexpected error while sending OneSignal notification", e.getMessage());
        }
    }

    public void sendNotificationForPartner(OneSignalNotificationRequest oneSignalNotificationRequest)
            throws OneSignalException {
        String endpoint = "/notifications?c=push";
        LoggingUtils.logRequest("sendNotification", oneSignalNotificationRequest);

        try {
            executeWithRetry(getClientForPartner()
                            .post()
                            .uri(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(oneSignalNotificationRequest))
                            .retrieve()
                            .bodyToMono(Void.class))
                    .doOnSuccess(unused -> log.info("OneSignal sendNotificationForPartner completed successfully"))
                    .onErrorMap(e -> {
                        log.error("Final failure after retries. Error: {}", e.getMessage(), e);
                        return new OneSignalException(
                                "Failed to sendNotification OneSignal " + oneSignalNotificationRequest.toString(),
                                e.getMessage());
                    })
                    .block();
        } catch (Exception e) {
            throw new OneSignalException("Unexpected error while sending OneSignal notification", e.getMessage());
        }
    }
}
