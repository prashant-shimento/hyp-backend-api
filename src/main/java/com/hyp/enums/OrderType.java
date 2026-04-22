package com.hyp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderType {
    H("1", "DELIVERY", "H"), // Home Delivery
    D("2", "DINE_IN", "D"), // Dine In
    P("3", "TAKEAWAY", "P"), // Parcel / Takeaway
    B("4", "BOOK_MANUAL", "B"); // Book Your Own Delivery

    private final String value;
    private final String label;
    private final String code;

    public static OrderType fromCode(String code) {
        for (OrderType type : OrderType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OrderType code: " + code);
    }
}
