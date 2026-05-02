package com.hyp.seeder;

import com.hyp.entity.Permission;
import com.hyp.repository.PermissionRepository;
import com.hyp.security.service.PermissionService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0) // Run before RoleSeeder
@RequiredArgsConstructor
public class PermissionSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final PermissionService permissionService;
    private final com.hyp.service.RedisService redisService;

    @Override
    public void run(String... args) {
        if ("false"
                .equalsIgnoreCase(
                        redisService.getRedisData("config:seeder:enabled").orElse("true"))) {
            log.info("Seeder disabled via config, skipping PermissionSeeder");
            return;
        }

        if (permissionRepository.count() > 0) {
            log.info("Permissions already seeded, skipping...");
            return;
        }

        log.info("Seeding default permissions...");

        List<Permission> permissions = new ArrayList<>();

        // ===========================================
        // AUTH PERMISSIONS
        // ===========================================
        permissions.add(createPermission("auth:refresh", "Refresh Token", "Refresh access tokens", "AUTH"));
        permissions.add(createPermission("auth:logout", "Logout", "Logout and revoke tokens", "AUTH"));
        permissions.add(
                createPermission("auth:profile", "View Profile", "View and update own profile information", "AUTH"));

        // ===========================================
        // CATEGORY PERMISSIONS
        // ===========================================
        permissions.add(createPermission(
                "category:read", "Read Categories", "View categories and their menu items", "CATEGORY"));
        permissions.add(createPermission("category:create", "Create Category", "Create new categories", "CATEGORY"));
        permissions.add(
                createPermission("category:update", "Update Category", "Update existing categories", "CATEGORY"));
        permissions.add(createPermission("category:delete", "Delete Category", "Delete categories", "CATEGORY"));

        // ===========================================
        // ITEM PERMISSIONS
        // ===========================================
        permissions.add(createPermission("item:read", "Read Items", "View menu items and their details", "ITEM"));
        permissions.add(createPermission("item:create", "Create Item", "Create new menu items", "ITEM"));
        permissions.add(createPermission("item:update", "Update Item", "Update existing menu items", "ITEM"));
        permissions.add(createPermission("item:delete", "Delete Item", "Delete menu items", "ITEM"));

        // ===========================================
        // VARIATION PERMISSIONS
        // ===========================================
        permissions.add(createPermission(
                "variation:read", "Read Variations", "View item variations and their addons", "VARIATION"));
        permissions.add(
                createPermission("variation:create", "Create Variation", "Create new item variations", "VARIATION"));
        permissions.add(createPermission(
                "variation:update", "Update Variation", "Update existing item variations", "VARIATION"));
        permissions.add(
                createPermission("variation:delete", "Delete Variation", "Delete item variations", "VARIATION"));

        // ===========================================
        // ADDON GROUP PERMISSIONS
        // ===========================================
        permissions.add(createPermission(
                "addon_group:read",
                "Read Addon Groups",
                "View addon groups and their associated addon items",
                "ADDON_GROUP"));
        permissions.add(createPermission(
                "addon_group:create", "Create Addon Group", "Create new addon groups for menu items", "ADDON_GROUP"));
        permissions.add(createPermission(
                "addon_group:update",
                "Update Addon Group",
                "Update existing addon groups and their configurations",
                "ADDON_GROUP"));
        permissions.add(createPermission(
                "addon_group:delete", "Delete Addon Group", "Delete addon groups from the menu", "ADDON_GROUP"));

        // ===========================================
        // MENU PERMISSIONS
        // ===========================================
        permissions.add(
                createPermission("menu:read", "Read Menu", "View full menu structure via menu controller", "MENU"));
        permissions.add(createPermission("menu:import", "Import Menu", "Import menu from external sources", "MENU"));

        // ===========================================
        // ORDER PERMISSIONS
        // ===========================================
        permissions.add(createPermission("order:create", "Create Order", "Place new orders", "ORDER"));
        permissions.add(createPermission("order:read", "Read Own Orders", "View orders placed by self", "ORDER"));
        permissions.add(
                createPermission("order:read_all", "Read All Orders", "View all orders in restaurant scope", "ORDER"));
        permissions.add(createPermission("order:update", "Update Orders", "Update order status", "ORDER"));
        permissions.add(createPermission("order:track", "Track Order", "Track order status publicly", "ORDER"));

        // ===========================================
        // SETTLEMENT PERMISSIONS
        // ===========================================
        permissions.add(
                createPermission("settlement:create", "Create Settlement", "Create settlement records", "SETTLEMENT"));
        permissions.add(
                createPermission("settlement:read", "Read Settlements", "View settlement information", "SETTLEMENT"));

        // ===========================================
        // CUSTOMER PERMISSIONS
        // ===========================================
        permissions.add(createPermission("customer:read", "Read Customers", "View customer information", "CUSTOMER"));
        permissions.add(createPermission("customer:create", "Create Customer", "Create new customers", "CUSTOMER"));
        permissions.add(
                createPermission("customer:update", "Update Customer", "Update existing customers", "CUSTOMER"));
        permissions.add(createPermission("customer:delete", "Delete Customer", "Delete customers", "CUSTOMER"));

        // ===========================================
        // ADDRESS PERMISSIONS
        // ===========================================
        permissions.add(createPermission("address:read", "Read Addresses", "View own addresses", "ADDRESS"));
        permissions.add(createPermission("address:create", "Create Address", "Create new addresses", "ADDRESS"));
        permissions.add(createPermission("address:update", "Update Address", "Update existing addresses", "ADDRESS"));
        permissions.add(createPermission("address:delete", "Delete Address", "Delete addresses", "ADDRESS"));

        // ===========================================
        // PAYMENT PERMISSIONS
        // ===========================================
        permissions.add(
                createPermission("payment:process", "Process Payments", "Process and verify payments", "PAYMENT"));
        permissions.add(createPermission("payment:view", "View Payments", "View payment information", "PAYMENT"));
        permissions.add(createPermission("payment:refund", "Process Refunds", "Process payment refunds", "PAYMENT"));

        // ===========================================
        // DELIVERY PERMISSIONS
        // ===========================================
        permissions.add(createPermission("delivery:view", "View Deliveries", "View delivery information", "DELIVERY"));
        permissions.add(createPermission(
                "delivery:manage", "Manage Deliveries", "Create, update, fulfill, cancel deliveries", "DELIVERY"));
        permissions.add(createPermission("delivery:quote", "Get Delivery Quote", "Get delivery quotes", "DELIVERY"));

        // ===========================================
        // RESTAURANT PERMISSIONS
        // ===========================================
        permissions.add(
                createPermission("restaurant:read", "Read Restaurant", "View restaurant information", "RESTAURANT"));
        permissions.add(createPermission(
                "restaurant:create", "Create Restaurant", "Create new restaurants (platform only)", "RESTAURANT"));
        permissions.add(createPermission(
                "restaurant:update",
                "Update Restaurant",
                "Update restaurant settings and configuration",
                "RESTAURANT"));
        permissions.add(createPermission(
                "restaurant:delete", "Delete Restaurant", "Delete restaurants (platform only)", "RESTAURANT"));

        // ===========================================
        // USER PERMISSIONS (Restaurant Staff)
        // ===========================================
        permissions.add(createPermission("user:read", "Read Users", "View restaurant staff users", "USER"));
        permissions.add(createPermission("user:create", "Create User", "Create new restaurant staff users", "USER"));

        // ===========================================
        // POS PERMISSIONS
        // ===========================================
        permissions.add(
                createPermission("pos:order:create", "Create POS Order", "Create order in the POS system", "POS"));

        // ===========================================
        // LOCATION PERMISSIONS
        // ===========================================
        permissions.add(createPermission("location:search", "Search Locations", "Use location/maps APIs", "LOCATION"));

        // ===========================================
        // FEE PERMISSIONS
        // ===========================================
        permissions.add(createPermission("fee:read", "Read Fees", "View fee configurations", "FEE"));
        permissions.add(createPermission("fee:create", "Create Fee", "Create new fees", "FEE"));
        permissions.add(createPermission("fee:update", "Update Fee", "Update existing fees", "FEE"));
        permissions.add(createPermission("fee:delete", "Delete Fee", "Delete fees", "FEE"));

        // ===========================================
        // TAX PERMISSIONS
        // ===========================================
        permissions.add(createPermission("tax:read", "Read Taxes", "View tax configurations", "TAX"));
        permissions.add(createPermission("tax:create", "Create Tax", "Create new taxes", "TAX"));
        permissions.add(createPermission("tax:update", "Update Tax", "Update existing taxes", "TAX"));
        permissions.add(createPermission("tax:delete", "Delete Tax", "Delete taxes", "TAX"));

        // ===========================================
        // OFFER PERMISSIONS
        // ===========================================
        permissions.add(createPermission("offer:read", "Read Offers", "View offer configurations", "OFFER"));
        permissions.add(createPermission("offer:create", "Create Offer", "Create new offers", "OFFER"));
        permissions.add(createPermission("offer:update", "Update Offer", "Update existing offers", "OFFER"));
        permissions.add(createPermission("offer:delete", "Delete Offer", "Delete offers", "OFFER"));

        // ===========================================
        // ORDER TYPE PERMISSIONS
        // ===========================================
        permissions.add(createPermission(
                "order_type:read", "Read Order Types", "View order type configurations", "ORDER_TYPE"));
        permissions.add(
                createPermission("order_type:create", "Create Order Type", "Create new order types", "ORDER_TYPE"));
        permissions.add(createPermission(
                "order_type:update", "Update Order Type", "Update existing order types", "ORDER_TYPE"));
        permissions.add(createPermission("order_type:delete", "Delete Order Type", "Delete order types", "ORDER_TYPE"));

        // ===========================================
        // FEEDBACK PERMISSIONS
        // ===========================================
        permissions.add(createPermission("feedback:read", "Read Feedback", "View feedback", "FEEDBACK"));
        permissions.add(createPermission("feedback:create", "Create Feedback", "Submit feedback", "FEEDBACK"));
        permissions.add(createPermission("feedback:update", "Update Feedback", "Update feedback", "FEEDBACK"));
        permissions.add(createPermission("feedback:delete", "Delete Feedback", "Delete feedback", "FEEDBACK"));

        // ===========================================
        // REPORT PERMISSIONS
        // ===========================================
        permissions.add(createPermission("report:read", "Read Reports", "View reports", "REPORT"));
        permissions.add(createPermission("report:create", "Create Report", "Create new reports", "REPORT"));
        permissions.add(createPermission("report:update", "Update Report", "Update existing reports", "REPORT"));
        permissions.add(createPermission("report:delete", "Delete Report", "Delete reports", "REPORT"));

        // ===========================================
        // NOTIFICATION PERMISSIONS
        // ===========================================
        permissions.add(createPermission(
                "notification:one_signal:create",
                "Register Notifications",
                "Register for push notifications",
                "NOTIFICATION"));
        permissions.add(createPermission(
                "notification:one_signal:delete",
                "Unregister Notifications",
                "Unregister from push notifications",
                "NOTIFICATION"));

        // ===========================================
        // REFERRAL PERMISSIONS
        // ===========================================
        permissions.add(createPermission("referral:read", "Read Referrals", "View referral programs", "REFERRAL"));
        permissions.add(createPermission("referral:create", "Create Referral", "Create referral programs", "REFERRAL"));
        permissions.add(createPermission("referral:update", "Update Referral", "Update referral programs", "REFERRAL"));
        permissions.add(createPermission("referral:delete", "Delete Referral", "Delete referral programs", "REFERRAL"));
        permissions.add(createPermission(
                "referral:token:create", "Create Referral Token", "Create referral token publicly", "REFERRAL"));

        // ===========================================
        // CUSTOMER REFERRAL PERMISSIONS
        // ===========================================
        permissions.add(createPermission(
                "customer-referral-read", "Read Customer Referrals", "View customer referrals", "CUSTOMER_REFERRAL"));

        // ===========================================
        // TESTIMONIAL PERMISSIONS
        // ===========================================
        permissions.add(createPermission("testimonial:read", "Read Testimonials", "View testimonials", "TESTIMONIAL"));
        permissions.add(
                createPermission("testimonial:create", "Create Testimonial", "Create testimonials", "TESTIMONIAL"));
        permissions.add(
                createPermission("testimonial:update", "Update Testimonial", "Update testimonials", "TESTIMONIAL"));
        permissions.add(
                createPermission("testimonial:delete", "Delete Testimonial", "Delete testimonials", "TESTIMONIAL"));

        // ===========================================
        // CONTENT PERMISSIONS
        // ===========================================
        permissions.add(createPermission("content:read", "Read Content", "View content pages", "CONTENT"));
        permissions.add(createPermission("content:create", "Create Content", "Create content pages", "CONTENT"));
        permissions.add(createPermission("content:update", "Update Content", "Update content pages", "CONTENT"));
        permissions.add(createPermission("content:delete", "Delete Content", "Delete content pages", "CONTENT"));

        // ===========================================
        // PARTNER PERMISSIONS
        // ===========================================
        permissions.add(createPermission("partner:read", "Read Partners", "View partner information", "PARTNER"));
        permissions.add(createPermission("partner:create", "Create Partner", "Create new partners", "PARTNER"));
        permissions.add(createPermission("partner:update", "Update Partner", "Update existing partners", "PARTNER"));
        permissions.add(createPermission("partner:delete", "Delete Partner", "Delete partners", "PARTNER"));

        // ===========================================
        // RIDER PERMISSIONS
        // ===========================================
        permissions.add(createPermission("rider:read", "Read Riders", "View rider information", "RIDER"));
        permissions.add(createPermission("rider:create", "Create Rider", "Create new riders", "RIDER"));
        permissions.add(createPermission("rider:update", "Update Rider", "Update existing riders", "RIDER"));
        permissions.add(createPermission("rider:delete", "Delete Rider", "Delete riders", "RIDER"));

        // ===========================================
        // CACHE PERMISSIONS
        // ===========================================
        permissions.add(createPermission("cache:manage", "Manage Cache", "View, update and clear cache", "CACHE"));

        // ===========================================
        // WEBHOOK PERMISSIONS (Partner Integrations)
        // ===========================================
        permissions.add(
                createPermission("webhook:pos", "POS Webhook", "Handle POS system callbacks and menu sync", "WEBHOOK"));
        permissions.add(createPermission(
                "webhook:delivery", "Delivery Webhook", "Handle delivery system callbacks", "WEBHOOK"));
        permissions.add(
                createPermission("webhook:payment", "Payment Webhook", "Handle payment gateway webhooks", "WEBHOOK"));

        // ===========================================
        // ADMIN PERMISSIONS (Platform Management)
        // ===========================================
        permissions.add(
                createPermission("admin:permissions", "Manage Permissions", "View and manage permissions", "ADMIN"));
        permissions.add(createPermission("admin:roles", "Manage Roles", "View and manage roles", "ADMIN"));
        permissions.add(
                createPermission("admin:policies", "Manage Policies", "View and manage access policies", "ADMIN"));
        permissions.add(createPermission(
                "admin:api-keys", "Manage API Keys", "View and manage API keys for partners", "ADMIN"));

        permissionRepository.saveAll(permissions);
        log.info("Seeded {} default permissions", permissions.size());

        // Refresh the cache
        permissionService.refreshCache();
    }

    private Permission createPermission(String name, String displayName, String description, String category) {
        return Permission.builder()
                .name(name)
                .displayName(displayName)
                .description(description)
                .category(category)
                .isSystem(true)
                .active(true)
                .build();
    }
}
