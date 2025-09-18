package com.hyp.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DynamicCorsFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(DynamicCorsFilter.class);

	private final CorsConfig props;
	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

	private record CacheEntry(boolean allowed, long expiresAtEpochSec) {
	}

	public DynamicCorsFilter(CorsConfig props) {
		this.props = props;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (origin != null && isOriginAllowed(origin)) {
			writeCorsResponseHeaders(request, response, origin);
		}

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void writeCorsResponseHeaders(HttpServletRequest request, HttpServletResponse response, String origin) {
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);

		String methods = String.join(",", props.getAllowedMethods());
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, methods);

		String reqHeaders = request.getHeader("Access-Control-Request-Headers");
		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, (reqHeaders == null ? "*" : reqHeaders));

		response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, String.valueOf(props.isAllowCredentials()));
		response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, String.valueOf(props.getCacheTtlSeconds()));

		response.setHeader(HttpHeaders.VARY, "Origin");
	}

	private boolean isOriginAllowed(String origin) {
		if ("null".equalsIgnoreCase(origin))
			return false;

		long now = Instant.now().getEpochSecond();
		CacheEntry cached = cache.get(origin);
		if (cached != null && cached.expiresAtEpochSec > now) {
			return cached.allowed;
		}

		boolean allowed = evaluateOrigin(origin);
		cache.put(origin, new CacheEntry(allowed, now + props.getCacheTtlSeconds()));
		if (allowed) {
			log.debug("CORS: allowed origin={} cachedFor={}s", origin, props.getCacheTtlSeconds());
		} else {
			log.debug("CORS: denied origin={}", origin);
		}
		return allowed;
	}

	private boolean evaluateOrigin(String origin) {
		try {
			URI uri = URI.create(origin);
			String host = uri.getHost();
			if (host == null)
				return false;

			// Normalize base domain and do fast-path match for subdomains
			String base = props.getBaseDomain();
			if (base != null && !base.isBlank()) {
				String normalizedBase = base.startsWith(".") ? base : "." + base;
				if (host.equals(base) || host.endsWith(normalizedBase)) {
					return true;
				}
			}

			Set<String> allowedIps = props.getServerIps().stream().collect(Collectors.toSet());
			if (allowedIps.contains(host))
				return true;

			// DNS resolution: check if any A record equals one of configured server IPs
			InetAddress[] addrs = InetAddress.getAllByName(host);
			Set<String> resolved = Arrays.stream(addrs).map(InetAddress::getHostAddress).collect(Collectors.toSet());
			for (String allowedIp : allowedIps) {
				if (resolved.contains(allowedIp)) {
					return true;
				}
			}

			return false;
		} catch (Exception e) {
			log.debug("CORS: origin evaluation error for {} -> {}", origin, e.toString());
			return false;
		}
	}
}
