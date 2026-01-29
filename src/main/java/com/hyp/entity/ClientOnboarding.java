package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "client_onboarding")
public class ClientOnboarding {

    @Id
    @Field("id")
    @GenerateId(sequenceName = "client_order_sequence")
    private String id;

    @Field("pos_partner")
    private PosPartner posPartner;

    @Field("business_details")
    private BusinessDetails businessDetails;

    @Field("owner_details")
    private OwnerDetails ownerDetails;

    @Field("restaurant_config")
    private RestaurantConfig restaurantConfig;

    @Field("landing_page_content")
    private LandingPageContent landingPageContent;

    @Field("ordering_page_content")
    private OrderingPageContent orderingPageContent;

    private Testimonials testimonials;

    @Field("settlement_details")
    private SettlementDetails settlementDetails;

    @Field("special_instruction")
    private String specialInstruction;

    @Field("business_status")
    private String businessStatus;

    @Data
    public static class PosPartner {
        @Field("pet_pooja_partner")
        private boolean petPoojaPartner;

        @Field("menu_sharing_Code")
        private String menuSharingCode;

        @Field("swiggy_link")
        private String swiggyLink;
    }

    @Data
    public static class BusinessDetails {
        private String name;

        @Field("contact_number")
        private String contactNumber;

        private String gst;

        @Field("fssai_number")
        private String fssaiNumber;

        @Field("fssai_certificate_copy")
        private String fssaiCertificateCopy;

        private String timings;

        private String address;

        @Field("domain_preferences")
        private DomainPreferences domainPreferences;

        @Field("social_links")
        private SocialLinks socialLinks;

        @Field("google_maps_link")
        private String googleMapsLink;
    }

    @Data
    public static class DomainPreferences {
        @Field("domain_name")
        private List<String> domainName;

        @Field("custom_domain")
        private boolean customDomain;

        private String status;
    }

    @Data
    public static class SocialLinks {
        private String facebook;

        private String instagram;

        private String youtube;
    }

    @Data
    public static class OwnerDetails {
        private String name;

        private String phone;

        @Field("alternate_contact_number")
        private String alternateContactNumber;
    }

    @Data
    public static class RestaurantConfig {
        private Double discount;

        private Double distance;

        @Field("delivery_share")
        private Double deliveryShare;

        private Location location;
    }

    @Data
    public static class Location {
        private Double latitude;

        private Double longitude;
    }

    @Data
    public static class LandingPageContent {
        @Field("banner_image")
        private String bannerImage;

        private String logo;

        @Field("food_images")
        private List<String> foodImages;

        private String favicon;

        private String status;
    }

    @Data
    public static class OrderingPageContent {
        private String favicon;

        @Field("gallery_images")
        private List<String> galleryImages;

        private String logo;

        private String status;
    }

    @Data
    public static class Testimonials {
        @Field("customer_testonomials")
        private List<String> customerTestonomials;

        private String status;
    }

    @Data
    public static class SettlementDetails {
        private String email;

        @Field("bank_account")
        private BankAccount bankAccount;
    }

    @Data
    public static class BankAccount {
        @Field("account_holder_name")
        private String accountHolderName;

        @Field("account_number")
        private String accountNumber;

        @Field("ifsc_code")
        private String ifscCode;

        @Field("bank_name")
        private String bankName;
    }
}
