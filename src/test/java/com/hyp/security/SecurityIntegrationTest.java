package com.hyp.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.*;
import com.hyp.enums.OrderStatusType;
import com.hyp.repository.*;
import com.hyp.security.jwt.JwtTokenProvider;
import com.hyp.security.policy.AccessPolicyService;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.security.service.PermissionService;
import com.hyp.security.service.RoleService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive Security Integration Tests
 *
 * Tests the complete security implementation including:
 * - Public API access (no authentication)
 * - Customer authentication and authorization
 * - Owner-only access control (customers can only access their own data)
 * - Restaurant-scoped access (users can only access their restaurant's data)
 * - Role-based permission checks
 * - Cross-tenant access prevention
 *
 * Test Scenarios:
 * 1. CUSTOMER role - public access, own data access, cross-customer access denial
 * 2. RESTAURANT_USER role - restaurant-scoped access, cross-restaurant denial
 * 3. RESTAURANT_ADMIN role - full restaurant access, cross-restaurant denial
 * 4. PLATFORM_ADMIN role - superadmin bypass
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private AccessPolicyService accessPolicyService;

    // Test Data IDs
    private static final String RESTAURANT_1_ID = "rest_test_001";
    private static final String RESTAURANT_2_ID = "rest_test_002";

    private Customer customer1;
    private Customer customer2;
    private Address customer1Address;
    private Address customer2Address;
    private User restaurantUser1;
    private User restaurantAdmin1;
    private User restaurantUser2;
    private User platformAdmin;
    private Restaurant restaurant1;
    private Restaurant restaurant2;
    private Category category1;
    private Category category2;
    private com.hyp.entity.Order order1;
    private com.hyp.entity.Order order2;

    // Tokens
    private String customer1Token;
    private String customer2Token;
    private String restaurantUser1Token;
    private String restaurantAdmin1Token;
    private String restaurantUser2Token;
    private String platformAdminToken;

    @BeforeAll
    void setupTestData() {
        cleanupTestData();
        createTestRestaurants();
        createTestCustomers();
        createTestAddresses();
        createTestUsers();
        createTestCategories();
        createTestOrders();
        generateTokens();

        // Refresh security caches
        permissionService.refreshCache();
        roleService.refreshCache();
        accessPolicyService.refreshCache();
    }

    @AfterAll
    void cleanupTestData() {
        orderRepository.deleteAllById(List.of("order_test_001", "order_test_002"));
        categoryRepository.deleteAllById(List.of("cat_test_001", "cat_test_002", "cat_test_temp"));
        addressRepository.deleteAllById(List.of("addr_test_001", "addr_test_002"));
        customerRepository.deleteAllById(List.of("cust_test_001", "cust_test_002", "cust_test_temp"));
        userRepository.deleteAllById(List.of("user_test_001", "user_test_002", "user_test_003", "user_test_admin"));
        restaurantRepository.deleteAllById(List.of(RESTAURANT_1_ID, RESTAURANT_2_ID, "rest_test_temp"));
    }

    // =========================================================================
    // TEST DATA SETUP
    // =========================================================================

    private void createTestRestaurants() {
        restaurant1 = new Restaurant();
        restaurant1.setId(RESTAURANT_1_ID);
        restaurant1.setRestaurantName("Test Restaurant 1");
        restaurant1.setActive(true);
        restaurantRepository.save(restaurant1);

        restaurant2 = new Restaurant();
        restaurant2.setId(RESTAURANT_2_ID);
        restaurant2.setRestaurantName("Test Restaurant 2");
        restaurant2.setActive(true);
        restaurantRepository.save(restaurant2);
    }

    private void createTestCustomers() {
        customer1 = Customer.builder()
                .id("cust_test_001")
                .name("Customer One")
                .mobile("9876543210")
                .email("customer1@test.com")
                .isVerified(true)
                .restaurants(Set.of(RESTAURANT_1_ID))
                .build();
        customerRepository.save(customer1);

        customer2 = Customer.builder()
                .id("cust_test_002")
                .name("Customer Two")
                .mobile("9876543211")
                .email("customer2@test.com")
                .isVerified(true)
                .restaurants(Set.of(RESTAURANT_1_ID, RESTAURANT_2_ID))
                .build();
        customerRepository.save(customer2);
    }

    private void createTestAddresses() {
        customer1Address = new Address();
        customer1Address.setId("addr_test_001");
        customer1Address.setCustomerId(customer1.getId());
        customer1Address.setAddressType("home");
        customer1Address.setAddressOne("123 Test Street");
        customer1Address.setCity("Test City");
        customer1Address.setState("Test State");
        customer1Address.setCountry("India");
        customer1Address.setPincode("600001");
        addressRepository.save(customer1Address);

        customer2Address = new Address();
        customer2Address.setId("addr_test_002");
        customer2Address.setCustomerId(customer2.getId());
        customer2Address.setAddressType("office");
        customer2Address.setAddressOne("456 Work Avenue");
        customer2Address.setCity("Work City");
        customer2Address.setState("Work State");
        customer2Address.setCountry("India");
        customer2Address.setPincode("600002");
        addressRepository.save(customer2Address);
    }

    private void createTestUsers() {
        // Restaurant 1 - User (staff)
        restaurantUser1 = new User();
        restaurantUser1.setId("user_test_001");
        restaurantUser1.setName("Restaurant User 1");
        restaurantUser1.setEmail("user1@restaurant1.com");
        restaurantUser1.setPassword(passwordEncoder.encode("password123"));
        restaurantUser1.setRole("RESTAURANT_USER");
        restaurantUser1.setRestaurantIds(Set.of(RESTAURANT_1_ID));
        restaurantUser1.setActive(true);
        userRepository.save(restaurantUser1);

        // Restaurant 1 - Admin
        restaurantAdmin1 = new User();
        restaurantAdmin1.setId("user_test_002");
        restaurantAdmin1.setName("Restaurant Admin 1");
        restaurantAdmin1.setEmail("admin1@restaurant1.com");
        restaurantAdmin1.setPassword(passwordEncoder.encode("password123"));
        restaurantAdmin1.setRole("RESTAURANT_ADMIN");
        restaurantAdmin1.setRestaurantIds(Set.of(RESTAURANT_1_ID));
        restaurantAdmin1.setActive(true);
        userRepository.save(restaurantAdmin1);

        // Restaurant 2 - User
        restaurantUser2 = new User();
        restaurantUser2.setId("user_test_003");
        restaurantUser2.setName("Restaurant User 2");
        restaurantUser2.setEmail("user2@restaurant2.com");
        restaurantUser2.setPassword(passwordEncoder.encode("password123"));
        restaurantUser2.setRole("RESTAURANT_USER");
        restaurantUser2.setRestaurantIds(Set.of(RESTAURANT_2_ID));
        restaurantUser2.setActive(true);
        userRepository.save(restaurantUser2);

        // Platform Admin (superadmin)
        platformAdmin = new User();
        platformAdmin.setId("user_test_admin");
        platformAdmin.setName("Platform Admin");
        platformAdmin.setEmail("admin@platform.com");
        platformAdmin.setPassword(passwordEncoder.encode("admin123"));
        platformAdmin.setRole("PLATFORM_ADMIN");
        platformAdmin.setActive(true);
        userRepository.save(platformAdmin);
    }

    private void createTestCategories() {
        category1 = new Category();
        category1.setId("cat_test_001");
        category1.setCategoryName("Category 1");
        category1.setRestaurantId(RESTAURANT_1_ID);
        category1.setActive("true");
        categoryRepository.save(category1);

        category2 = new Category();
        category2.setId("cat_test_002");
        category2.setCategoryName("Category 2");
        category2.setRestaurantId(RESTAURANT_2_ID);
        category2.setActive("true");
        categoryRepository.save(category2);
    }

    private void createTestOrders() {
        order1 = new com.hyp.entity.Order();
        order1.setId("order_test_001");
        order1.setCustomerId(customer1.getId());
        order1.setRestaurantId(RESTAURANT_1_ID);
        order1.setStatus(OrderStatusType.CREATED);
        orderRepository.save(order1);

        order2 = new com.hyp.entity.Order();
        order2.setId("order_test_002");
        order2.setCustomerId(customer2.getId());
        order2.setRestaurantId(RESTAURANT_2_ID);
        order2.setStatus(OrderStatusType.CREATED);
        orderRepository.save(order2);
    }

    private void generateTokens() {
        // Customer 1 token
        customer1Token = jwtTokenProvider.generateAccessToken(UserPrincipal.builder()
                .userId(customer1.getId())
                .userType("CUSTOMER")
                .role("CUSTOMER")
                .mobile(customer1.getMobile())
                .name(customer1.getName())
                .restaurantId(RESTAURANT_1_ID)
                .restaurants(customer1.getRestaurants())
                .build());

        // Customer 2 token
        customer2Token = jwtTokenProvider.generateAccessToken(UserPrincipal.builder()
                .userId(customer2.getId())
                .userType("CUSTOMER")
                .role("CUSTOMER")
                .mobile(customer2.getMobile())
                .name(customer2.getName())
                .restaurantId(RESTAURANT_1_ID)
                .restaurants(customer2.getRestaurants())
                .build());

        // Restaurant User 1 token
        restaurantUser1Token = jwtTokenProvider.generateAccessToken(UserPrincipal.builder()
                .userId(restaurantUser1.getId())
                .userType("USER")
                .role("RESTAURANT_USER")
                .email(restaurantUser1.getEmail())
                .name(restaurantUser1.getName())
                .restaurantId(RESTAURANT_1_ID)
                .restaurants(Set.of(RESTAURANT_1_ID))
                .build());

        // Restaurant Admin 1 token
        restaurantAdmin1Token = jwtTokenProvider.generateAccessToken(UserPrincipal.builder()
                .userId(restaurantAdmin1.getId())
                .userType("USER")
                .role("RESTAURANT_ADMIN")
                .email(restaurantAdmin1.getEmail())
                .name(restaurantAdmin1.getName())
                .restaurantId(RESTAURANT_1_ID)
                .restaurants(Set.of(RESTAURANT_1_ID))
                .build());

        // Restaurant User 2 token
        restaurantUser2Token = jwtTokenProvider.generateAccessToken(UserPrincipal.builder()
                .userId(restaurantUser2.getId())
                .userType("USER")
                .role("RESTAURANT_USER")
                .email(restaurantUser2.getEmail())
                .name(restaurantUser2.getName())
                .restaurantId(RESTAURANT_2_ID)
                .restaurants(Set.of(RESTAURANT_2_ID))
                .build());

        // Platform Admin token
        platformAdminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.builder()
                .userId(platformAdmin.getId())
                .userType("USER")
                .role("PLATFORM_ADMIN")
                .email(platformAdmin.getEmail())
                .name(platformAdmin.getName())
                .isSuperAdmin(true)
                .build());
    }

    // =========================================================================
    // 1. PUBLIC API ACCESS TESTS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("PUBLIC: Unauthenticated user can access public menu endpoints")
    void publicUser_canAccessPublicMenuEndpoints() throws Exception {
        // GET /category - public read
        mockMvc.perform(get("/api/v2/category")
                        .param("restaurantId", RESTAURANT_1_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // GET /restaurant - public read
        mockMvc.perform(get("/api/v2/restaurant").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    @DisplayName("PUBLIC: Unauthenticated user can track order publicly")
    void publicUser_canTrackOrder() throws Exception {
        mockMvc.perform(get("/api/v2/order/track/" + order1.getId()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @DisplayName("PUBLIC: Unauthenticated user cannot access protected endpoints")
    void publicUser_cannotAccessProtectedEndpoints() throws Exception {
        // GET /order - requires authentication
        mockMvc.perform(get("/api/v2/order").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // GET /address - requires authentication
        mockMvc.perform(get("/api/v2/address").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // GET /customer - requires authentication
        mockMvc.perform(get("/api/v2/customer").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. CUSTOMER ROLE TESTS - Owner-Only Access
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("CUSTOMER: Can access own addresses")
    void customer_canAccessOwnAddresses() throws Exception {
        mockMvc.perform(get("/api/v2/address")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v2/address/" + customer1Address.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("CUSTOMER: Cannot access another customer's addresses")
    void customer_cannotAccessOtherCustomerAddresses() throws Exception {
        // Customer 1 trying to access Customer 2's address
        mockMvc.perform(get("/api/v2/address/" + customer2Address.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(12)
    @DisplayName("CUSTOMER: Can access own orders")
    void customer_canAccessOwnOrders() throws Exception {
        mockMvc.perform(get("/api/v2/order/" + order1.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(13)
    @DisplayName("CUSTOMER: Cannot access another customer's orders")
    void customer_cannotAccessOtherCustomerOrders() throws Exception {
        // Customer 1 trying to access Customer 2's order
        mockMvc.perform(get("/api/v2/order/" + order2.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(14)
    @DisplayName("CUSTOMER: Can update own profile")
    void customer_canUpdateOwnProfile() throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "Updated Customer One");

        mockMvc.perform(patch("/api/v2/customer/" + customer1.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(15)
    @DisplayName("CUSTOMER: Cannot update another customer's profile")
    void customer_cannotUpdateOtherCustomerProfile() throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "Hacked Name");

        mockMvc.perform(patch("/api/v2/customer/" + customer2.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(16)
    @DisplayName("CUSTOMER: Cannot access admin endpoints")
    void customer_cannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v2/admin/permissions")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v2/admin/roles")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. RESTAURANT_USER ROLE TESTS - Restaurant Scoped Access
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("RESTAURANT_USER: Can read customers in own restaurant")
    void restaurantUser_canReadCustomersInOwnRestaurant() throws Exception {
        mockMvc.perform(get("/api/v2/customer")
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .param("restaurantId", RESTAURANT_1_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(21)
    @DisplayName("RESTAURANT_USER: Can read orders in own restaurant")
    void restaurantUser_canReadOrdersInOwnRestaurant() throws Exception {
        mockMvc.perform(get("/api/v2/order")
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .param("restaurantId", RESTAURANT_1_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(22)
    @DisplayName("RESTAURANT_USER: Can update orders in own restaurant")
    void restaurantUser_canUpdateOrdersInOwnRestaurant() throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", "CONFIRMED");

        mockMvc.perform(patch("/api/v2/order/" + order1.getId())
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(23)
    @DisplayName("RESTAURANT_USER: Cannot access orders from other restaurant")
    void restaurantUser_cannotAccessOrdersFromOtherRestaurant() throws Exception {
        // Restaurant User 1 trying to access Restaurant 2's order
        mockMvc.perform(get("/api/v2/order/" + order2.getId())
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(24)
    @DisplayName("RESTAURANT_USER: Cannot update orders in other restaurant")
    void restaurantUser_cannotUpdateOrdersInOtherRestaurant() throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", "CANCELLED");

        mockMvc.perform(patch("/api/v2/order/" + order2.getId())
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(25)
    @DisplayName("RESTAURANT_USER: Can create categories in own restaurant")
    void restaurantUser_canCreateCategoryInOwnRestaurant() throws Exception {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("categoryName", "New Category");
        categoryData.put("restaurantId", RESTAURANT_1_ID);
        categoryData.put("active", "true");

        mockMvc.perform(post("/api/v2/category")
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryData)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(26)
    @DisplayName("RESTAURANT_USER: Cannot create categories in other restaurant")
    void restaurantUser_cannotCreateCategoryInOtherRestaurant() throws Exception {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("categoryName", "Unauthorized Category");
        categoryData.put("restaurantId", RESTAURANT_2_ID);
        categoryData.put("active", "true");

        mockMvc.perform(post("/api/v2/category")
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryData)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(27)
    @DisplayName("RESTAURANT_USER: Cannot delete categories (no delete permission)")
    void restaurantUser_cannotDeleteCategories() throws Exception {
        mockMvc.perform(delete("/api/v2/category/" + category1.getId())
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(28)
    @DisplayName("RESTAURANT_USER: Cannot access user management")
    void restaurantUser_cannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/v2/user")
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. RESTAURANT_ADMIN ROLE TESTS - Full Restaurant Access
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("RESTAURANT_ADMIN: Can manage users in own restaurant")
    void restaurantAdmin_canManageUsersInOwnRestaurant() throws Exception {
        mockMvc.perform(get("/api/v2/user")
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .param("restaurantId", RESTAURANT_1_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(31)
    @DisplayName("RESTAURANT_ADMIN: Can delete categories in own restaurant")
    void restaurantAdmin_canDeleteCategoryInOwnRestaurant() throws Exception {
        // First create a category to delete
        Category tempCategory = new Category();
        tempCategory.setId("cat_test_temp");
        tempCategory.setCategoryName("Temp Category");
        tempCategory.setRestaurantId(RESTAURANT_1_ID);
        tempCategory.setActive("true");
        categoryRepository.save(tempCategory);

        mockMvc.perform(delete("/api/v2/category/" + tempCategory.getId())
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(32)
    @DisplayName("RESTAURANT_ADMIN: Cannot delete categories in other restaurant")
    void restaurantAdmin_cannotDeleteCategoryInOtherRestaurant() throws Exception {
        mockMvc.perform(delete("/api/v2/category/" + category2.getId())
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(33)
    @DisplayName("RESTAURANT_ADMIN: Can update own restaurant settings")
    void restaurantAdmin_canUpdateOwnRestaurant() throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("restaurantName", "Updated Restaurant 1");

        mockMvc.perform(patch("/api/v2/restaurant/" + RESTAURANT_1_ID)
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(34)
    @DisplayName("RESTAURANT_ADMIN: Cannot update other restaurant settings")
    void restaurantAdmin_cannotUpdateOtherRestaurant() throws Exception {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("restaurantName", "Hacked Restaurant 2");

        mockMvc.perform(patch("/api/v2/restaurant/" + RESTAURANT_2_ID)
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(35)
    @DisplayName("RESTAURANT_ADMIN: Cannot create new restaurants (platform only)")
    void restaurantAdmin_cannotCreateRestaurant() throws Exception {
        Map<String, Object> restaurantData = new HashMap<>();
        restaurantData.put("restaurantName", "New Restaurant");
        restaurantData.put("active", true);

        mockMvc.perform(post("/api/v2/restaurant")
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restaurantData)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(36)
    @DisplayName("RESTAURANT_ADMIN: Cannot access admin endpoints")
    void restaurantAdmin_cannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v2/admin/permissions")
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v2/partner")
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 5. PLATFORM_ADMIN ROLE TESTS - Superadmin Access
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("PLATFORM_ADMIN: Can access admin permissions endpoint")
    void platformAdmin_canAccessAdminPermissions() throws Exception {
        mockMvc.perform(get("/api/v2/admin/permissions")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(41)
    @DisplayName("PLATFORM_ADMIN: Can access admin roles endpoint")
    void platformAdmin_canAccessAdminRoles() throws Exception {
        mockMvc.perform(get("/api/v2/admin/roles")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(42)
    @DisplayName("PLATFORM_ADMIN: Can access admin policies endpoint")
    void platformAdmin_canAccessAdminPolicies() throws Exception {
        mockMvc.perform(get("/api/v2/admin/policies")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(43)
    @DisplayName("PLATFORM_ADMIN: Can create new restaurant")
    void platformAdmin_canCreateRestaurant() throws Exception {
        Map<String, Object> restaurantData = new HashMap<>();
        restaurantData.put("restaurantName", "Platform Created Restaurant");
        restaurantData.put("active", true);

        mockMvc.perform(post("/api/v2/restaurant")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restaurantData)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(44)
    @DisplayName("PLATFORM_ADMIN: Can delete restaurant")
    void platformAdmin_canDeleteRestaurant() throws Exception {
        // Create a restaurant to delete
        Restaurant tempRestaurant = new Restaurant();
        tempRestaurant.setId("rest_test_temp");
        tempRestaurant.setRestaurantName("Temp Restaurant");
        tempRestaurant.setActive(true);
        restaurantRepository.save(tempRestaurant);

        mockMvc.perform(delete("/api/v2/restaurant/" + tempRestaurant.getId())
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(45)
    @DisplayName("PLATFORM_ADMIN: Can access any restaurant's data (superadmin bypass)")
    void platformAdmin_canAccessAnyRestaurantData() throws Exception {
        // Access Restaurant 1 data
        mockMvc.perform(get("/api/v2/order/" + order1.getId())
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Access Restaurant 2 data
        mockMvc.perform(get("/api/v2/order/" + order2.getId())
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(46)
    @DisplayName("PLATFORM_ADMIN: Can manage partners")
    void platformAdmin_canManagePartners() throws Exception {
        mockMvc.perform(get("/api/v2/partner")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Order(47)
    @DisplayName("PLATFORM_ADMIN: Can manage cache")
    void platformAdmin_canManageCache() throws Exception {
        mockMvc.perform(get("/api/v2/cache")
                        .header("Authorization", "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 6. CROSS-RESTAURANT ACCESS PREVENTION TESTS
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("CROSS-RESTAURANT: User from Restaurant 1 cannot access Restaurant 2 categories")
    void crossRestaurant_cannotAccessOtherRestaurantCategories() throws Exception {
        mockMvc.perform(patch("/api/v2/category/" + category2.getId())
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\": \"Hacked Category\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(51)
    @DisplayName("CROSS-RESTAURANT: User from Restaurant 2 cannot access Restaurant 1 orders")
    void crossRestaurant_cannotAccessOtherRestaurantOrders() throws Exception {
        mockMvc.perform(get("/api/v2/order/" + order1.getId())
                        .header("Authorization", "Bearer " + restaurantUser2Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(52)
    @DisplayName("CROSS-RESTAURANT: Restaurant Admin cannot manage users from other restaurant")
    void crossRestaurant_adminCannotManageOtherRestaurantUsers() throws Exception {
        mockMvc.perform(get("/api/v2/user/" + restaurantUser2.getId())
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 7. TOKEN VALIDATION TESTS
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("TOKEN: Invalid token is rejected")
    void invalidToken_isRejected() throws Exception {
        mockMvc.perform(get("/api/v2/order")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(61)
    @DisplayName("TOKEN: Missing Authorization header is rejected for protected endpoints")
    void missingAuthHeader_isRejected() throws Exception {
        mockMvc.perform(get("/api/v2/order").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(62)
    @DisplayName("TOKEN: Malformed Authorization header is rejected")
    void malformedAuthHeader_isRejected() throws Exception {
        mockMvc.perform(get("/api/v2/order")
                        .header("Authorization", "NotBearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 8. PERMISSION BOUNDARY TESTS
    // =========================================================================

    @Test
    @Order(70)
    @DisplayName("PERMISSION: Customer cannot access customer:read (staff only)")
    void customer_cannotReadOtherCustomers() throws Exception {
        // Customer trying to list all customers (staff permission)
        mockMvc.perform(get("/api/v2/customer")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(71)
    @DisplayName("PERMISSION: Restaurant User cannot delete customers")
    void restaurantUser_cannotDeleteCustomers() throws Exception {
        mockMvc.perform(delete("/api/v2/customer/" + customer1.getId())
                        .header("Authorization", "Bearer " + restaurantUser1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(72)
    @DisplayName("PERMISSION: Restaurant Admin can delete customers")
    void restaurantAdmin_canDeleteCustomers() throws Exception {
        // Create a temp customer to delete
        Customer tempCustomer = Customer.builder()
                .id("cust_test_temp")
                .name("Temp Customer")
                .mobile("9999999999")
                .restaurants(Set.of(RESTAURANT_1_ID))
                .build();
        customerRepository.save(tempCustomer);

        mockMvc.perform(delete("/api/v2/customer/" + tempCustomer.getId())
                        .header("Authorization", "Bearer " + restaurantAdmin1Token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
