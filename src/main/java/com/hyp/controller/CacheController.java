package com.hyp.controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.exception.EntityNotFoundException;
import com.hyp.response.Response;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/cache")
public class CacheController {

	@Autowired
	private CacheManager cacheManager;

	@GetMapping
	public ResponseEntity<Response> listAllCaches() {
		Collection<String> cacheNames = cacheManager.getCacheNames();
		return ResponseEntity.ok(new Response(List.of(cacheNames), false, "All cache names listed"));
	}

	@GetMapping("/{cacheName}")
	public ResponseEntity<Response> getCacheContent(@PathVariable String cacheName) throws EntityNotFoundException {
		Cache cache = Optional.ofNullable(cacheManager.getCache(cacheName))
				.orElseThrow(() -> new EntityNotFoundException("Cache", cacheName));

		Object nativeCache = cache.getNativeCache();

		if (nativeCache instanceof ConcurrentMap) {
			@SuppressWarnings("unchecked")
			ConcurrentMap<Object, Object> cacheMap = (ConcurrentMap<Object, Object>) nativeCache;

			return ResponseEntity.ok(new Response(List.of(cacheMap), false, "Cache content fetched successfully"));
		}

		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
				.body(new Response(null, true, "Unsupported cache backend: " + nativeCache.getClass().getName()));
	}
	
	@PostMapping("/{cacheName}")
	public ResponseEntity<Response> createCache(@PathVariable String cacheName) {
	    cacheManager.getCache(cacheName);
	    return ResponseEntity.ok(new Response(null, false, "Cache '" + cacheName + "' created successfully"));
	}
	
	@PutMapping("/{cacheName}")
	public ResponseEntity<Response> addOrUpdateCacheEntry(
	        @PathVariable String cacheName,
	        @RequestBody Map<Object, Object> entries) throws EntityNotFoundException {

	    Cache cache = Optional.ofNullable(cacheManager.getCache(cacheName))
	            .orElseThrow(() -> new EntityNotFoundException("Cache", cacheName));

	    entries.forEach(cache::put);

	    return ResponseEntity.ok(new Response(null, false, "Cache entries added/updated"));
	}

	@DeleteMapping("/{cacheName}")
	public ResponseEntity<Response> clearCache(@PathVariable String cacheName) throws EntityNotFoundException {
		Cache cache = Optional.ofNullable(cacheManager.getCache(cacheName))
				.orElseThrow(() -> new EntityNotFoundException("Cache", cacheName));

		cache.clear();
		return ResponseEntity.ok(new Response(null, false, "Cache '" + cacheName + "' cleared successfully"));
	}

	@DeleteMapping
	public ResponseEntity<Response> clearAllCaches() {
		cacheManager.getCacheNames().forEach(name -> {
			Cache cache = cacheManager.getCache(name);
			if (cache != null)
				cache.clear();
		});
		return ResponseEntity.ok(new Response(null, false, "All caches cleared successfully"));
	}

}
