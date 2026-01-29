package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientOnboardingDto {

    private String id;
    private PosPartner posPartner;
    private BusinessDetails businessDetails;
    private OwnerDetails ownerDetails;
    private RestaurantConfig restaurantConfig;
    private LandingPageContent landingPageContent;
    private OrderingPageContent orderingPageContent;
    private Testimonials testimonials;
    private SettlementDetails settlementDetails;
    private String specialInstruction;
    private String businessStatus;

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PosPartner {
        private boolean petPoojaPartner;
        private String menuSharingCode;
        private String swiggyLink;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BusinessDetails {
        private String name;
        private String contactNumber;
        private String gst;
        private String fssaiNumber;
        private String fssaiCertificateCopy;
        private String timings;
        private String address;
        private DomainPreferences domainPreferences;
        private SocialLinks socialLinks;
        private String googleMapsLink;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DomainPreferences {
        private List<String> domainName;
        private boolean customDomain;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SocialLinks {
        private String facebook;
        private String instagram;
        private String youtube;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OwnerDetails {
        private String name;
        private String phone;
        private String alternateContactNumber;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RestaurantConfig {
        private Double discount;
        private Double distance;
        private Double deliveryShare;
        private Location location;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Location {
        private Double latitude;
        private Double longitude;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LandingPageContent {
        private String bannerImage;
        private String logo;
        private List<String> foodImages;
        private String favicon;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderingPageContent {
        private String favicon;
        private List<String> galleryImages;
        private String logo;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Testimonials {
        private List<String> customerTestonomials;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SettlementDetails {
        private String email;
        private BankAccount bankAccount;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BankAccount {
        private String accountHolderName;
        private String accountNumber;
        private String ifscCode;
        private String bankName;
    }
}
