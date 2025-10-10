package com.hyp.model;

import lombok.Data;

@Data
public class DeliveryRiderLocation {

    private Rider rider;
    private String status;
    private Location location;

    @Data
    public static class Rider {
        private String name;
        private String mobile;
    }
}
