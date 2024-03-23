package com.hyp.service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class OtpService {

	@Value("${sms.url}")
	private String smsBaseUrl;

	@Value("${sms.key}")
	private String smsKey;

	private static final Integer EXPIRE_MINS = 1;
	private LoadingCache<String, Integer> otpCache;
	private WebClient webClient;

	public OtpService(WebClient.Builder webClientBuilder) {
		otpCache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRE_MINS, TimeUnit.MINUTES)
				.build(new CacheLoader<String, Integer>() {
					public Integer load(String key) {
						return 0;
					}
				});
		this.webClient = webClientBuilder.build();

	}

	public int generateOTP(String key) {
		Random random = new Random();
		int otp = 100000 + random.nextInt(900000);
		otpCache.put(key, otp);
		return otp;
	}

	public int getOtp(String key) {
		try {
			return otpCache.get(key);
		} catch (Exception e) {
			return 0;
		}
	}

	public void clearOTP(String key) {
		otpCache.invalidate(key);
	}

	public boolean sendOtp(String mobileNum) {
		int otp = generateOTP(mobileNum);
		String smsUrl = smsBaseUrl;
		String apikey = smsKey;
		smsUrl = smsUrl.replace("{key}", apikey).replace("{mobile}", mobileNum).replace("{otp}", String.valueOf(otp));

		return webClient.get().uri(smsUrl).retrieve().bodyToMono(String.class)
				.map(response -> new JSONObject(response).optString("Status"))
				.map(status -> status.equalsIgnoreCase("Success")).block();
	}
}
