package com.hyp.seeder;

import com.hyp.entity.Role;
import com.hyp.repository.RoleRepository;
import com.hyp.security.service.RoleService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1) // After PermissionSeeder, before PolicySeeder
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final RoleService roleService;
    private final com.hyp.service.RedisService redisService;

    @Override
    public void run(String... args) {
        if ("false"
                .equalsIgnoreCase(
                        redisService.getRedisData("config:seeder:enabled").orElse("true"))) {
            log.info("Seeder disabled via config, skipping RoleSeeder");
            return;
        }

        if (roleRepository.count() > 0) {
            log.info("Roles already exist. Skipping RoleSeeder...");
            return;
        }

        log.info("Seeding roles...");

        List<Role> roles = List.of(

                // =====================================================
                // PUBLIC
                // Unauthenticated access - read-only menu, tracking, location
                // =====================================================
                Role.builder()
                        .name("PUBLIC")
                        .displayName("Public")
                        .description("Unauthenticated public access")
                        .priority(0)
                        .inheritsFrom(Set.of())
                        .permissions(Set.of(
                                // Auth
                                "auth:refresh",

                                // Menu read access
                                "category:read",
                                "item:read",
                                "variation:read",
                                "addon_group:read",
                                "menu:read",

                                // Public data
                                "restaurant:read",
                                "location:search",
                                "order:track",
                                "fee:read",
                                "tax:read",
                                "offer:read",
                                "order_type:read",
                                "testimonial:read",
                                "content:read",
                                "referral:token:create"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // CUSTOMER
                // End users who place orders
                // =====================================================
                Role.builder()
                        .name("CUSTOMER")
                        .displayName("Customer")
                        .description("End users placing orders")
                        .priority(10)
                        .inheritsFrom(Set.of("PUBLIC"))
                        .permissions(Set.of(
                                // Order management
                                "order:create",
                                "order:read",

                                // Address management
                                "address:read",
                                "address:create",
                                "address:update",
                                "address:delete",

                                // Customer update (own profile)
                                "customer:update",

                                // Payment
                                "payment:process",
                                "payment:view",

                                // Delivery
                                "delivery:quote",
                                "delivery:view",

                                // Feedback
                                "feedback:create",

                                // Notifications
                                "notification:one_signal:create",
                                "notification:one_signal:delete",

                                // Auth
                                "auth:profile",
                                "auth:logout"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // RESTAURANT_USER
                // Restaurant staff with operational access
                // =====================================================
                Role.builder()
                        .name("RESTAURANT_USER")
                        .displayName("Restaurant User")
                        .description("Restaurant staff with operational access")
                        .priority(20)
                        .inheritsFrom(Set.of("PUBLIC"))
                        .permissions(Set.of(
                                // Menu write (create/update, not delete)
                                "category:create",
                                "category:update",
                                "item:create",
                                "item:update",
                                "variation:create",
                                "variation:update",
                                "addon_group:create",
                                "addon_group:update",

                                // Order management
                                "order:read_all",
                                "order:update",

                                // Settlement
                                "settlement:create",

                                // Customer read
                                "customer:read",

                                // Payment processing
                                "payment:process",

                                // Delivery management
                                "delivery:view",
                                "delivery:manage",

                                // Feedback read/update
                                "feedback:read",
                                "feedback:update",

                                // Rider read
                                "rider:read",

                                // POS
                                "pos:order:create",

                                // Notifications
                                "notification:one_signal:create",
                                "notification:one_signal:delete",

                                // Auth
                                "auth:profile",
                                "auth:logout"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // RESTAURANT_ADMIN
                // Restaurant owner/manager with full restaurant control
                // =====================================================
                Role.builder()
                        .name("RESTAURANT_ADMIN")
                        .displayName("Restaurant Admin")
                        .description("Restaurant owner / manager")
                        .priority(30)
                        .inheritsFrom(Set.of("RESTAURANT_USER"))
                        .permissions(Set.of(
                                // Menu delete + import
                                "category:delete",
                                "item:delete",
                                "variation:delete",
                                "addon_group:delete",
                                "menu:import",

                                // User management
                                "user:read",
                                "user:create",

                                // Customer management
                                "customer:create",
                                "customer:update",
                                "customer:delete",

                                // Restaurant management
                                "restaurant:update",

                                // Payment refund
                                "payment:refund",

                                // Settlement read
                                "settlement:read",

                                // Fee management
                                "fee:create",
                                "fee:update",
                                "fee:delete",

                                // Tax management
                                "tax:create",
                                "tax:update",
                                "tax:delete",

                                // Offer management
                                "offer:create",
                                "offer:update",
                                "offer:delete",

                                // Order type management
                                "order_type:create",
                                "order_type:update",
                                "order_type:delete",

                                // Feedback delete
                                "feedback:delete",

                                // Report management
                                "report:read",
                                "report:create",
                                "report:update",
                                "report:delete",

                                // Referral management
                                "referral:read",
                                "referral:create",
                                "referral:update",
                                "referral:delete",

                                // Customer referral read
                                "customer-referral-read",

                                // Testimonial management
                                "testimonial:create",
                                "testimonial:update",
                                "testimonial:delete",

                                // Content management
                                "content:create",
                                "content:update",
                                "content:delete",

                                // Rider management
                                "rider:create",
                                "rider:update"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // API_PARTNER (Restaurant Scoped)
                // Third-party integrations with restaurant scope
                // =====================================================
                Role.builder()
                        .name("API_PARTNER")
                        .displayName("Restaurant API Partner")
                        .description("Third-party integrations with restaurant scope")
                        .priority(18)
                        .inheritsFrom(Set.of())
                        .permissions(Set.of(
                                // Menu read
                                "category:read",
                                "item:read",
                                "variation:read",
                                "addon_group:read",
                                "menu:read",

                                // Menu write (create/update)
                                "category:create",
                                "category:update",
                                "item:create",
                                "item:update",
                                "variation:create",
                                "variation:update",

                                // Order management
                                "order:create",
                                "order:read_all",
                                "order:update",

                                // Customer read
                                "customer:read",

                                // Delivery
                                "delivery:view",
                                "delivery:quote",

                                // Payment view
                                "payment:view"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // AI_AGENT
                // AI agents, chatbots, MCP servers
                // =====================================================
                Role.builder()
                        .name("AI_AGENT")
                        .displayName("AI Agent")
                        .description("AI agents, chatbots, MCP servers")
                        .priority(16)
                        .inheritsFrom(Set.of())
                        .permissions(Set.of(
                                // Menu read
                                "category:read",
                                "item:read",
                                "variation:read",
                                "addon_group:read",
                                "menu:read",

                                // Order
                                "order:create",
                                "order:read_all",
                                "order:track",

                                // Customer read
                                "customer:read",

                                // Delivery
                                "delivery:view",
                                "delivery:quote",

                                // Restaurant
                                "restaurant:read"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // POS_PARTNER
                // POS system integrations (webhook authentication)
                // =====================================================
                Role.builder()
                        .name("POS_PARTNER")
                        .displayName("POS Partner")
                        .description("POS integrations (webhooks)")
                        .priority(15)
                        .inheritsFrom(Set.of())
                        .permissions(Set.of("webhook:pos"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // DELIVERY_PARTNER
                // Delivery service webhooks
                // =====================================================
                Role.builder()
                        .name("DELIVERY_PARTNER")
                        .displayName("Delivery Partner")
                        .description("Delivery service webhooks")
                        .priority(15)
                        .inheritsFrom(Set.of())
                        .permissions(Set.of("webhook:delivery"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // PAYMENT_PARTNER
                // Payment gateway webhooks
                // =====================================================
                Role.builder()
                        .name("PAYMENT_PARTNER")
                        .displayName("Payment Partner")
                        .description("Payment gateway webhooks")
                        .priority(15)
                        .inheritsFrom(Set.of())
                        .permissions(Set.of("webhook:payment"))
                        .isSystem(true)
                        .active(true)
                        .build(),

                // =====================================================
                // PLATFORM_ADMIN
                // Full platform access (superadmin bypass)
                // =====================================================
                Role.builder()
                        .name("PLATFORM_ADMIN")
                        .displayName("Platform Admin")
                        .description("Full platform access (bypass)")
                        .priority(100)
                        .inheritsFrom(Set.of())
                        .permissions(Set.of(
                                // Restaurant create/delete
                                "restaurant:create",
                                "restaurant:delete",

                                // Admin
                                "admin:permissions",
                                "admin:roles",
                                "admin:policies",
                                "admin:api-keys",

                                // Partner management
                                "partner:read",
                                "partner:create",
                                "partner:update",
                                "partner:delete",

                                // Rider delete
                                "rider:read",
                                "rider:create",
                                "rider:update",
                                "rider:delete",

                                // Cache
                                "cache:manage"))
                        .isSystem(true)
                        .isSuperAdmin(true)
                        .active(true)
                        .build());

        roleRepository.saveAll(roles);
        log.info("Seeded {} roles", roles.size());

        roleService.refreshCache();
    }
}
