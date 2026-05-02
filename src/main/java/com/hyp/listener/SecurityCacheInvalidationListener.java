package com.hyp.listener;

import com.hyp.security.policy.AccessPolicyService;
import com.hyp.security.service.PermissionService;
import com.hyp.security.service.RoleService;
import com.hyp.security.service.SecurityCacheService.CacheInvalidationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Listener for security cache invalidation events.
 * When a cache invalidation message is received via Redis pub/sub,
 * this listener triggers a cache refresh on the appropriate service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityCacheInvalidationListener implements MessageListener {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AccessPolicyService accessPolicyService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String invalidationType = message.toString();
            log.info("Received security cache invalidation event: {}", invalidationType);

            CacheInvalidationType type = CacheInvalidationType.valueOf(invalidationType);

            switch (type) {
                case ROLES:
                    log.info("Refreshing role cache due to invalidation event");
                    roleService.refreshCache();
                    break;
                case PERMISSIONS:
                    log.info("Refreshing permission cache due to invalidation event");
                    permissionService.refreshCache();
                    break;
                case POLICIES:
                    log.info("Refreshing access policy cache due to invalidation event");
                    accessPolicyService.refreshCache();
                    break;
                case ALL:
                    log.info("Refreshing all security caches due to invalidation event");
                    roleService.refreshCache();
                    permissionService.refreshCache();
                    accessPolicyService.refreshCache();
                    break;
                default:
                    log.warn("Unknown cache invalidation type: {}", invalidationType);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid cache invalidation type received: {}", message.toString(), e);
        } catch (Exception e) {
            log.error("Error processing cache invalidation event", e);
        }
    }
}
