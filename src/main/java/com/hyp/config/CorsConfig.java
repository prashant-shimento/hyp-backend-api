package com.hyp.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {

	@Value("#{'${cors.server-ips}'.split(',')}")
	private List<String> serverIps;
	private String baseDomain = "hyperapps.in";
	private boolean allowCredentials = false;
	private long cacheTtlSeconds = 300L;
	private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

	// getters & setters
	public List<String> getServerIps() {
		return serverIps;
	}

	public void setServerIps(List<String> serverIps) {
		this.serverIps = serverIps;
	}

	public String getBaseDomain() {
		return baseDomain;
	}

	public void setBaseDomain(String baseDomain) {
		this.baseDomain = baseDomain;
	}

	public boolean isAllowCredentials() {
		return allowCredentials;
	}

	public void setAllowCredentials(boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	public long getCacheTtlSeconds() {
		return cacheTtlSeconds;
	}

	public void setCacheTtlSeconds(long cacheTtlSeconds) {
		this.cacheTtlSeconds = cacheTtlSeconds;
	}

	public List<String> getAllowedMethods() {
		return allowedMethods;
	}

	public void setAllowedMethods(List<String> allowedMethods) {
		this.allowedMethods = allowedMethods;
	}
}
