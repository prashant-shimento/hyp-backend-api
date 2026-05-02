package com.hyp.seeder;

import com.hyp.entity.AccessPolicy;
import com.hyp.repository.AccessPolicyRepository;
import com.hyp.security.policy.AccessPolicyService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2) // Run after RoleSeeder
@RequiredArgsConstructor
public class PolicySeeder implements CommandLineRunner {

    private final AccessPolicyRepository accessPolicyRepository;
    private final AccessPolicyService accessPolicyService;
    private final com.hyp.service.RedisService redisService;

    @Override
    public void run(String... args) {
        if ("false"
                .equalsIgnoreCase(
                        redisService.getRedisData("config:seeder:enabled").orElse("true"))) {
            log.info("Seeder disabled via config, skipping PolicySeeder");
            return;
        }

        if (accessPolicyRepository.count() > 0) {
            log.info("Policies already seeded, skipping...");
            return;
        }

        log.info("Seeding default access policies...");

        List<AccessPolicy> policies = new ArrayList<>();

        // ===========================================
        // AUTHENTICATION APIs
        // ===========================================
        policies.add(createTruePublicPolicy("/login/otp", "POST", 100));
        policies.add(createTruePublicPolicy("/login/verify-otp", "POST", 100));
        policies.add(createTruePublicPolicy("/login/resend-otp/{mobile}", "POST", 100));
        policies.add(createTruePublicPolicy("/user/auth/login", "POST", 100));
        policies.add(createPolicy("/auth/refresh", "POST", List.of("auth:refresh"), true, false, false, false, 100));
        policies.add(createPolicy("/auth/logout", "POST", List.of("auth:logout"), false, false, false, false, 90));
        policies.add(createPolicy("/auth/logout-all", "POST", List.of("auth:logout"), false, false, false, false, 90));
        policies.add(createPolicy("/auth/me", "GET", List.of("auth:profile"), false, false, false, false, 90));

        // ===========================================
        // STATIC RESOURCES
        // ===========================================
        policies.add(createTruePublicPolicy("/checkout.html", "GET", 100));

        // ===========================================
        // CATEGORY APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/category", "GET", List.of("category:read"), 90));
        policies.add(createRestaurantScopedPolicy("/category/{id}", "GET", List.of("category:read"), 90));
        policies.add(createRestaurantScopedPolicy("/category", "POST", List.of("category:create"), 90));
        policies.add(createRestaurantScopedPolicy("/category/{id}", "PATCH", List.of("category:update"), 90));
        policies.add(createRestaurantScopedPolicy("/category/{id}", "DELETE", List.of("category:delete"), 90));

        // ===========================================
        // ITEM APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/item", "GET", List.of("item:read"), 90));
        policies.add(createRestaurantScopedPolicy("/item/{id}", "GET", List.of("item:read"), 90));
        policies.add(
                createPolicy("/item/{id}/variations", "GET", List.of("item:read"), true, false, false, false, 100));
        policies.add(createPolicy("/item/{id}/addons", "GET", List.of("item:read"), true, false, false, false, 100));
        policies.add(createRestaurantScopedPolicy("/item", "POST", List.of("item:create"), 90));
        policies.add(createRestaurantScopedPolicy("/item/{id}", "PATCH", List.of("item:update"), 90));
        policies.add(createRestaurantScopedPolicy("/item/{id}", "DELETE", List.of("item:delete"), 90));

        // ===========================================
        // VARIATION APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/variation", "GET", List.of("variation:read"), 90));
        policies.add(createRestaurantScopedPolicy("/variation/{id}", "GET", List.of("variation:read"), 90));
        policies.add(createRestaurantScopedPolicy("/variation/addons", "GET", List.of("variation:read"), 90));
        policies.add(createRestaurantScopedPolicy("/variation/{id}/addons", "GET", List.of("variation:read"), 90));
        policies.add(createRestaurantScopedPolicy("/variation", "POST", List.of("variation:create"), 90));
        policies.add(createRestaurantScopedPolicy("/variation/{id}", "PATCH", List.of("variation:update"), 90));
        policies.add(createRestaurantScopedPolicy("/variation/{id}", "DELETE", List.of("variation:delete"), 90));

        // ===========================================
        // ADDON GROUP APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/addon-group", "GET", List.of("addon_group:read"), 90));
        policies.add(createRestaurantScopedPolicy("/addon-group/{id}", "GET", List.of("addon_group:read"), 90));
        policies.add(createRestaurantScopedPolicy("/addon-group/items", "GET", List.of("addon_group:read"), 90));
        policies.add(createRestaurantScopedPolicy("/addon-group/{id}/items", "GET", List.of("addon_group:read"), 90));
        policies.add(createRestaurantScopedPolicy("/addon-group", "POST", List.of("addon_group:create"), 90));
        policies.add(createRestaurantScopedPolicy("/addon-group/{id}", "PATCH", List.of("addon_group:update"), 90));
        policies.add(createRestaurantScopedPolicy("/addon-group/{id}", "DELETE", List.of("addon_group:delete"), 90));

        // ===========================================
        // MENU APIs
        // ===========================================
        // Primary: restaurant-scoped via X-Restaurant-Id header (preferred)
        policies.add(createPolicy("/menu", "GET", List.of("menu:read"), true, true, false, false, 100));
        policies.add(createPolicy("/menu/category", "GET", List.of("menu:read"), true, true, false, false, 100));
        policies.add(createPolicy("/menu/category/{id}", "GET", List.of("menu:read"), true, true, false, false, 100));
        policies.add(createRestaurantScopedPolicy("/menu/extract", "POST", List.of("menu:import"), 90));
        policies.add(createRestaurantScopedPolicy("/menu/import", "POST", List.of("menu:import"), 90));

        // ===========================================
        // ORDER APIs
        // ===========================================
        policies.add(
                createPolicy("/order", "GET", List.of("order:read", "order:read_all"), false, true, true, false, 90));
        policies.add(createPolicy(
                "/order/{id}", "GET", List.of("order:read", "order:read_all"), false, true, true, false, 90));
        policies.add(createPolicy("/order", "POST", List.of("order:create"), false, true, false, false, 90));
        policies.add(createRestaurantScopedPolicy("/order/{id}", "PATCH", List.of("order:update"), 90));
        policies.add(createPolicy("/order/track/{id}", "GET", List.of("order:track"), false, false, true, false, 100));
        policies.add(createRestaurantScopedPolicy("/order/{id}/settlement", "POST", List.of("settlement:create"), 90));

        // ===========================================
        // CUSTOMER APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/customer", "GET", List.of("customer:read"), 90));
        policies.add(createRestaurantScopedPolicy("/customer/{id}", "GET", List.of("customer:read"), 90));
        policies.add(createPolicy("/customer", "POST", List.of("customer:create"), false, false, false, false, 90));
        policies.add(
                createPolicy("/customer/{id}", "PATCH", List.of("customer:update"), false, false, true, false, 90));
        policies.add(createRestaurantScopedPolicy("/customer/{id}", "DELETE", List.of("customer:delete"), 90));

        // ===========================================
        // ADDRESS APIs
        // ===========================================
        policies.add(createOwnerOnlyPolicy("/address", "GET", List.of("address:read"), 90));
        policies.add(createOwnerOnlyPolicy("/address/{id}", "GET", List.of("address:read"), 90));
        policies.add(createOwnerOnlyPolicy("/address", "POST", List.of("address:create"), 90));
        policies.add(createOwnerOnlyPolicy("/address/{id}", "PATCH", List.of("address:update"), 90));
        policies.add(createOwnerOnlyPolicy("/address/{id}", "DELETE", List.of("address:delete"), 90));

        // ===========================================
        // PAYMENT APIs
        // ===========================================
        policies.add(
                createPolicy("/payment/{orderId}", "POST", List.of("payment:process"), false, false, false, false, 90));
        policies.add(
                createRestaurantScopedPolicy("/payment/consume/{orderId}", "POST", List.of("payment:process"), 90));
        policies.add(
                createRestaurantScopedPolicy("/payment/process/{orderId}", "POST", List.of("payment:process"), 90));
        policies.add(createPolicy(
                "/payment/verify/{orderId}", "POST", List.of("payment:process"), false, false, false, false, 90));
        policies.add(createRestaurantScopedPolicy("/payment/refund", "POST", List.of("payment:refund"), 90));
        policies.add(createPolicy(
                "/payment/refund/{orderId}", "GET", List.of("payment:view"), false, false, false, false, 90));
        policies.add(createWebhookPolicy("/payment/callback", "POST", List.of("webhook:payment"), 100));

        // ===========================================
        // DELIVERY APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/delivery", "GET", List.of("delivery:view"), 90));
        policies.add(createRestaurantScopedPolicy("/delivery/{id}", "GET", List.of("delivery:view"), 90));
        policies.add(createPolicy(
                "/delivery/quote/{restaurantId}", "GET", List.of("delivery:quote"), false, false, false, false, 90));
        policies.add(createPolicy(
                "/delivery/rider-detail/{orderId}", "GET", List.of("delivery:view"), false, false, false, false, 90));
        policies.add(createPolicy(
                "/delivery/rider-location/{orderId}", "GET", List.of("delivery:view"), false, false, false, false, 90));
        policies.add(
                createRestaurantScopedPolicy("/delivery/create/{orderId}", "POST", List.of("delivery:manage"), 90));
        policies.add(
                createRestaurantScopedPolicy("/delivery/fulfill/{orderId}", "POST", List.of("delivery:manage"), 90));
        policies.add(
                createRestaurantScopedPolicy("/delivery/consume/{orderId}", "POST", List.of("delivery:manage"), 90));
        policies.add(
                createRestaurantScopedPolicy("/delivery/unallocate/{orderId}", "POST", List.of("delivery:manage"), 90));
        policies.add(
                createRestaurantScopedPolicy("/delivery/cancel/{orderId}", "POST", List.of("delivery:manage"), 90));
        policies.add(createRestaurantScopedPolicy("/delivery/status/{orderId}", "GET", List.of("delivery:manage"), 90));
        policies.add(createWebhookPolicy("/delivery/callback", "POST", List.of("webhook:delivery"), 100));

        // ===========================================
        // RESTAURANT APIs
        // ===========================================
        policies.add(createPolicy(
                "/restaurant/storefront", "GET", List.of("restaurant:read"), true, true, false, false, 90));
        policies.add(createRestaurantScopedPolicy("/restaurant", "GET", List.of("restaurant:read"), 90));
        policies.add(createRestaurantScopedPolicy("/restaurant/{id}", "GET", List.of("restaurant:read"), 90));
        policies.add(createPolicy("/restaurant", "POST", List.of("restaurant:create"), false, false, false, false, 80));
        policies.add(createRestaurantScopedPolicy("/restaurant/{id}", "PATCH", List.of("restaurant:update"), 90));
        policies.add(createPolicy(
                "/restaurant/{id}", "DELETE", List.of("restaurant:delete"), false, false, false, false, 80));
        policies.add(
                createRestaurantScopedPolicy("/restaurant/{id}/settlement", "POST", List.of("settlement:create"), 90));
        policies.add(createPolicy(
                "/restaurant/{id}/addsubscription",
                "PATCH",
                List.of("restaurant:manage"),
                false,
                false,
                false,
                false,
                80));

        // ===========================================
        // USER APIs (Restaurant Staff)
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/user", "GET", List.of("user:read"), 90));
        policies.add(createRestaurantScopedPolicy("/user/{id}", "GET", List.of("user:read"), 90));
        policies.add(createRestaurantScopedPolicy("/user", "POST", List.of("user:create"), 90));

        // ===========================================
        // POS APIs (Webhook)
        // ===========================================
        policies.add(createWebhookPolicy("/pos/menu", "POST", List.of("webhook:pos"), 100));
        policies.add(createWebhookPolicy("/pos/status/get", "POST", List.of("webhook:pos"), 100));
        policies.add(createWebhookPolicy("/pos/status/update", "POST", List.of("webhook:pos"), 100));
        policies.add(createWebhookPolicy("/pos/stock", "POST", List.of("webhook:pos"), 100));
        policies.add(createWebhookPolicy("/pos/order/callback", "POST", List.of("webhook:pos"), 100));
        policies.add(createPolicy(
                "/pos/order/{orderId}", "POST", List.of("pos:order:create"), false, false, false, false, 90));

        // ===========================================
        // LOCATION APIs
        // ===========================================
        policies.add(createPolicy(
                "/location/maps/predict", "GET", List.of("location:search"), true, false, false, false, 100));
        policies.add(createPolicy(
                "/location/maps/place/{entityId}", "GET", List.of("location:search"), true, false, false, false, 100));

        // ===========================================
        // ADMIN PERMISSION APIs
        // ===========================================
        policies.add(createPolicy(
                "/admin/permissions", "GET", List.of("admin:permissions"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/permissions/{id}", "GET", List.of("admin:permissions"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/permissions", "POST", List.of("admin:permissions"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/permissions/{id}", "PATCH", List.of("admin:permissions"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/permissions/{id}", "DELETE", List.of("admin:permissions"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/permissions/refresh", "POST", List.of("admin:permissions"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/permissions/categories", "GET", List.of("admin:permissions"), false, false, false, false, 80));

        // ===========================================
        // ADMIN ROLE APIs
        // ===========================================
        policies.add(createPolicy("/admin/roles", "GET", List.of("admin:roles"), false, false, false, false, 80));
        policies.add(createPolicy("/admin/roles/{id}", "GET", List.of("admin:roles"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/roles/{name}/effective", "GET", List.of("admin:roles"), false, false, false, false, 80));
        policies.add(createPolicy("/admin/roles", "POST", List.of("admin:roles"), false, false, false, false, 80));
        policies.add(
                createPolicy("/admin/roles/{id}", "PATCH", List.of("admin:roles"), false, false, false, false, 80));
        policies.add(
                createPolicy("/admin/roles/{id}", "DELETE", List.of("admin:roles"), false, false, false, false, 80));
        policies.add(
                createPolicy("/admin/roles/refresh", "POST", List.of("admin:roles"), false, false, false, false, 80));

        // ===========================================
        // ADMIN POLICY APIs
        // ===========================================
        policies.add(createPolicy("/admin/policies", "GET", List.of("admin:policies"), false, false, false, false, 80));
        policies.add(
                createPolicy("/admin/policies/{id}", "GET", List.of("admin:policies"), false, false, false, false, 80));
        policies.add(
                createPolicy("/admin/policies", "POST", List.of("admin:policies"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/policies/{id}", "PATCH", List.of("admin:policies"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/policies/{id}", "DELETE", List.of("admin:policies"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/policies/refresh", "POST", List.of("admin:policies"), false, false, false, false, 80));

        // ===========================================
        // ADMIN API KEY APIs
        // ===========================================
        policies.add(createPolicy("/admin/api-keys", "GET", List.of("admin:api-keys"), false, false, false, false, 80));
        policies.add(
                createPolicy("/admin/api-keys/{id}", "GET", List.of("admin:api-keys"), false, false, false, false, 80));
        policies.add(
                createPolicy("/admin/api-keys", "POST", List.of("admin:api-keys"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/api-keys/{id}", "PATCH", List.of("admin:api-keys"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/api-keys/{id}", "DELETE", List.of("admin:api-keys"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/api-keys/{id}/regenerate", "POST", List.of("admin:api-keys"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/api-keys/{id}/deactivate", "POST", List.of("admin:api-keys"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/admin/api-keys/{id}/activate", "POST", List.of("admin:api-keys"), false, false, false, false, 80));

        // ===========================================
        // FEE APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/fee", "GET", List.of("fee:read"), 90));
        policies.add(createRestaurantScopedPolicy("/fee/{id}", "GET", List.of("fee:read"), 90));
        policies.add(createRestaurantScopedPolicy("/fee", "POST", List.of("fee:create"), 90));
        policies.add(createRestaurantScopedPolicy("/fee/{id}", "PATCH", List.of("fee:update"), 90));
        policies.add(createRestaurantScopedPolicy("/fee/{id}", "DELETE", List.of("fee:delete"), 90));

        // ===========================================
        // TAX APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/tax", "GET", List.of("tax:read"), 90));
        policies.add(createRestaurantScopedPolicy("/tax/{id}", "GET", List.of("tax:read"), 90));
        policies.add(createRestaurantScopedPolicy("/tax", "POST", List.of("tax:create"), 90));
        policies.add(createRestaurantScopedPolicy("/tax/{id}", "PATCH", List.of("tax:update"), 90));
        policies.add(createRestaurantScopedPolicy("/tax/{id}", "DELETE", List.of("tax:delete"), 90));

        // ===========================================
        // OFFER APIs
        // ===========================================
        policies.add(createPolicy("/offer", "GET", List.of("offer:read"), true, true, false, false, 100));
        policies.add(createPolicy("/offer/{id}", "GET", List.of("offer:read"), true, true, false, false, 100));
        policies.add(createRestaurantScopedPolicy("/offer", "POST", List.of("offer:create"), 90));
        policies.add(createRestaurantScopedPolicy("/offer/{id}", "PATCH", List.of("offer:update"), 90));
        policies.add(createRestaurantScopedPolicy("/offer/{id}", "DELETE", List.of("offer:delete"), 90));

        // ===========================================
        // FEEDBACK APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/feedback", "GET", List.of("feedback:read"), 90));
        policies.add(createRestaurantScopedPolicy("/feedback/{id}", "GET", List.of("feedback:read"), 90));
        policies.add(createPolicy("/feedback", "POST", List.of("feedback:create"), false, false, false, false, 90));
        policies.add(createRestaurantScopedPolicy("/feedback/{id}", "PATCH", List.of("feedback:update"), 90));
        policies.add(createRestaurantScopedPolicy("/feedback/{id}", "DELETE", List.of("feedback:delete"), 90));

        // ===========================================
        // SETTLEMENT APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/settlement", "GET", List.of("settlement:read"), 90));
        policies.add(createRestaurantScopedPolicy("/settlement/{id}", "GET", List.of("settlement:read"), 90));

        // ===========================================
        // REPORT APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/report", "GET", List.of("report:read"), 90));
        policies.add(createRestaurantScopedPolicy("/report/{id}", "GET", List.of("report:read"), 90));
        policies.add(createRestaurantScopedPolicy("/report", "POST", List.of("report:create"), 90));
        policies.add(createRestaurantScopedPolicy("/report/{id}", "PATCH", List.of("report:update"), 90));
        policies.add(createRestaurantScopedPolicy("/report/{id}", "DELETE", List.of("report:delete"), 90));
        policies.add(createRestaurantScopedPolicy("/report/{id}/generate", "POST", List.of("report:create"), 90));

        // ===========================================
        // NOTIFICATION APIs
        // ===========================================
        policies.add(createPolicy(
                "/notification/one-signal/register/{id}",
                "POST",
                List.of("notification:one_signal:create"),
                false,
                false,
                false,
                false,
                90));
        policies.add(createPolicy(
                "/notification/one-signal/register/{id}",
                "DELETE",
                List.of("notification:one_signal:delete"),
                false,
                false,
                false,
                false,
                90));

        // ===========================================
        // REFERRAL APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/referral", "GET", List.of("referral:read"), 90));
        policies.add(createRestaurantScopedPolicy("/referral/{id}", "GET", List.of("referral:read"), 90));
        policies.add(createRestaurantScopedPolicy("/referral", "POST", List.of("referral:create"), 90));
        policies.add(createRestaurantScopedPolicy("/referral/{id}", "PATCH", List.of("referral:update"), 90));
        policies.add(createRestaurantScopedPolicy("/referral/{id}", "DELETE", List.of("referral:delete"), 90));
        policies.add(createPolicy(
                "/referral/token", "POST", List.of("referral:token:create"), true, false, false, false, 100));

        // ===========================================
        // CUSTOMER REFERRAL APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/customer-referral", "GET", List.of("customer-referral-read"), 90));
        policies.add(
                createRestaurantScopedPolicy("/customer-referral/{id}", "GET", List.of("customer-referral-read"), 90));

        // ===========================================
        // TESTIMONIAL APIs
        // ===========================================
        policies.add(createPolicy("/testimonial", "GET", List.of("testimonial:read"), true, true, false, false, 100));
        policies.add(
                createPolicy("/testimonial/{id}", "GET", List.of("testimonial:read"), true, true, false, false, 100));
        policies.add(createRestaurantScopedPolicy("/testimonial", "POST", List.of("testimonial:create"), 90));
        policies.add(createRestaurantScopedPolicy("/testimonial/{id}", "PATCH", List.of("testimonial:update"), 90));
        policies.add(createRestaurantScopedPolicy("/testimonial/{id}", "DELETE", List.of("testimonial:delete"), 90));

        // ===========================================
        // CONTENT APIs
        // ===========================================
        policies.add(createPolicy("/content", "GET", List.of("content:read"), true, true, false, false, 100));
        policies.add(createPolicy("/content/{id}", "GET", List.of("content:read"), true, true, false, false, 100));
        policies.add(createRestaurantScopedPolicy("/content", "POST", List.of("content:create"), 90));
        policies.add(createRestaurantScopedPolicy("/content/{id}", "PATCH", List.of("content:update"), 90));
        policies.add(createRestaurantScopedPolicy("/content/{id}", "DELETE", List.of("content:delete"), 90));

        // ===========================================
        // PARTNER APIs
        // ===========================================
        // Public discovery — lists all partners, or resolves a single partner by ?domain=
        policies.add(
                createPolicy("/partner/storefront", "GET", List.of("partner:read"), true, false, false, false, 100));
        policies.add(createPolicy("/partner", "GET", List.of("partner:read"), false, false, false, false, 80));
        policies.add(createPolicy("/partner/{id}", "GET", List.of("partner:read"), false, false, false, false, 80));
        policies.add(createPolicy("/partner", "POST", List.of("partner:create"), false, false, false, false, 80));
        policies.add(createPolicy("/partner/{id}", "PATCH", List.of("partner:update"), false, false, false, false, 80));
        policies.add(
                createPolicy("/partner/{id}", "DELETE", List.of("partner:delete"), false, false, false, false, 80));
        policies.add(createPolicy(
                "/partner/{id}/upload", "POST", List.of("partner:update"), false, false, false, false, 80));

        // ===========================================
        // RIDER APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/rider", "GET", List.of("rider:read"), 90));
        policies.add(createRestaurantScopedPolicy("/rider/{id}", "GET", List.of("rider:read"), 90));
        policies.add(createRestaurantScopedPolicy("/rider", "POST", List.of("rider:create"), 90));
        policies.add(createRestaurantScopedPolicy("/rider/{id}", "PATCH", List.of("rider:update"), 90));
        policies.add(createRestaurantScopedPolicy("/rider/{id}", "DELETE", List.of("rider:delete"), 90));

        // ===========================================
        // CACHE APIs
        // ===========================================
        policies.add(createPolicy("/cache", "GET", List.of("cache:manage"), false, false, false, false, 80));
        policies.add(createPolicy("/cache/{name}", "GET", List.of("cache:manage"), false, false, false, false, 80));
        policies.add(createPolicy("/cache/{name}", "POST", List.of("cache:manage"), false, false, false, false, 80));
        policies.add(createPolicy("/cache/{name}", "PUT", List.of("cache:manage"), false, false, false, false, 80));
        policies.add(createPolicy("/cache/{name}", "DELETE", List.of("cache:manage"), false, false, false, false, 80));
        policies.add(createPolicy("/cache", "DELETE", List.of("cache:manage"), false, false, false, false, 80));

        // ===========================================
        // CLIENT ONBOARDING APIs (Platform Admin only)
        // ===========================================
        policies.add(createPolicy("/client", "GET", List.of("admin:client"), false, false, false, false, 80));
        policies.add(createPolicy("/client/{id}", "GET", List.of("admin:client"), false, false, false, false, 80));
        policies.add(createPolicy("/client", "POST", List.of("admin:client"), false, false, false, false, 80));
        policies.add(createPolicy("/client/{id}", "PATCH", List.of("admin:client"), false, false, false, false, 80));
        policies.add(createPolicy("/client/{id}", "DELETE", List.of("admin:client"), false, false, false, false, 80));

        // ===========================================
        // OFFER USAGE APIs
        // ===========================================
        policies.add(createRestaurantScopedPolicy("/offer-usage", "GET", List.of("offer:read"), 90));
        policies.add(createRestaurantScopedPolicy("/offer-usage/{id}", "GET", List.of("offer:read"), 90));
        policies.add(createRestaurantScopedPolicy("/offer-usage", "POST", List.of("offer:create"), 90));
        policies.add(createRestaurantScopedPolicy("/offer-usage/{id}", "PATCH", List.of("offer:update"), 90));
        policies.add(createRestaurantScopedPolicy("/offer-usage/{id}", "DELETE", List.of("offer:delete"), 90));

        // ===========================================
        // PLAYGROUND APIs (Testing Only)
        // ===========================================
        policies.add(createPolicy("/play-ground/**", "*", Collections.emptyList(), false, false, false, false, 80));

        accessPolicyRepository.saveAll(policies);
        log.info("Seeded {} access policies", policies.size());

        // Refresh the cache
        accessPolicyService.refreshCache();
    }

    /**
     * Creates a truly public policy with no authentication required and no restaurant scoping.
     */
    private AccessPolicy createTruePublicPolicy(String resource, String operation, int priority) {
        return AccessPolicy.builder()
                .resource(resource)
                .operation(operation)
                .permissions(Collections.emptyList())
                .isPublic(true)
                .restaurantScoped(false)
                .ownerOnly(false)
                .webhookAuth(false)
                .priority(priority)
                .active(true)
                .build();
    }

    /**
     * Creates a policy requiring authentication with restaurant scoping.
     */
    private AccessPolicy createRestaurantScopedPolicy(
            String resource, String operation, List<String> permissions, int priority) {
        return AccessPolicy.builder()
                .resource(resource)
                .operation(operation)
                .permissions(permissions)
                .isPublic(false)
                .restaurantScoped(true)
                .ownerOnly(false)
                .webhookAuth(false)
                .priority(priority)
                .active(true)
                .build();
    }

    /**
     * Creates a policy where users can only access their own resources.
     */
    private AccessPolicy createOwnerOnlyPolicy(
            String resource, String operation, List<String> permissions, int priority) {
        return AccessPolicy.builder()
                .resource(resource)
                .operation(operation)
                .permissions(permissions)
                .isPublic(false)
                .restaurantScoped(false)
                .ownerOnly(true)
                .webhookAuth(false)
                .priority(priority)
                .active(true)
                .build();
    }

    /**
     * Creates a webhook policy requiring API key authentication.
     */
    private AccessPolicy createWebhookPolicy(
            String resource, String operation, List<String> permissions, int priority) {
        return AccessPolicy.builder()
                .resource(resource)
                .operation(operation)
                .permissions(permissions)
                .isPublic(false)
                .restaurantScoped(false)
                .ownerOnly(false)
                .webhookAuth(true)
                .priority(priority)
                .active(true)
                .build();
    }

    /**
     * Creates a fully customizable policy with all parameters specified.
     */
    private AccessPolicy createPolicy(
            String resource,
            String operation,
            List<String> permissions,
            boolean isPublic,
            boolean restaurantScoped,
            boolean ownerOnly,
            boolean webhookAuth,
            int priority) {
        return AccessPolicy.builder()
                .resource(resource)
                .operation(operation)
                .permissions(permissions)
                .isPublic(isPublic)
                .restaurantScoped(restaurantScoped)
                .ownerOnly(ownerOnly)
                .webhookAuth(webhookAuth)
                .priority(priority)
                .active(true)
                .build();
    }
}
