package com.hyp.enums;

public enum TaxType {
    F, // Fixed
    P; // Percentage

    public static String fromCode(String code) {
        return switch (code) {
            case "1" -> P.toString();
            case "2" -> F.toString();
            default -> P.toString();
        };
    }
}
