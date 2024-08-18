package com.hyp.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDto extends BaseDto {

    private String deliveryOrderId;
    private String orderId;
    private String referenceId;
    private String channel;
    private DeliveryOrderStatusType status;
    private ContactDetailDto senderDetail;
    private ContactDetailDto pocDetail;
    private ContactDetailDto receiverDetail;
    private double amount;
    private int networkId;
    private boolean pickupNow;
    private String service;
    private String networkToken;
    private DeliveryFulfillmentDto fulfillment;
    private String fulfillmentType;
    private LocalDateTime fulfillmentAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactDetailDto {
        private AddressDto address;
        private String name;
        private String mobile;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String addressLine1;
        private String addressLine2;
        private String label;
        private String landmark;
        private String city;
        private String state;
        private String country;
        private String pincode;
        private double latitude;
        private double longitude;
        private String instructionsToReach;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryFulfillmentDto {
        private ChannelDto channel;
        private List<LogDto> logs;
        private DeliveryFulfillStatusType status;
        private LogisticsInfoDto pickup;
        private RiderDto rider;
        private LogisticsInfoDto drop;
        private MtgDto mtg;
        private String trackCode;
        private double deliveryCharge;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelDto {
        private String name;
        private String orderId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogDto {
        private String timestamp;
        private String status;
        private LocationDto location;
        private String remark;
        private RiderDto rider;
        private ChannelDto channel;
        private String attemptType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private double latitude;
        private double longitude;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiderDto {
        private String id;
        private String name;
        private String mobile;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogisticsInfoDto {
        private String eta;
        private LocationDto location;
        private String timestamp;
        private List<Object> proof;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MtgDto {
        private int tripId;
        private int groupId;
        private int riderId;
        private int bundleId;
        private int sequenceNumber;
    }
}
