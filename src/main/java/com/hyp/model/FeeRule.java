package com.hyp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyp.enums.FeeComponent;
import com.hyp.enums.FeeType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeeRule {

    @NotNull(message = "Fee component is mandatory")
    private FeeComponent fee;

    @NotNull(message = "Fee type is mandatory")
    private FeeType type;

    private Double value;
    private Double thresholdValue;
    private Double minValue;
    private Double maxValue;
    private boolean active = true;

    @AssertTrue(message = "Value is mandatory for FIXED or PERCENTAGE type and cannot be 0")
    private boolean isValueValid() {
        if (type == null) return true;
        if (type == FeeType.FIXED || type == FeeType.PERCENTAGE) {
            return value != null && value > 0;
        }
        return true;
    }

    @AssertTrue(message = "thresholdValue, minValue, and maxValue are mandatory for THRESHOLD type and cannot be 0")
    private boolean isThresholdValid() {
        if (type == FeeType.THRESHOLD) {
            return thresholdValue != null && minValue != null && maxValue != null && minValue > 0 && maxValue > 0;
        }
        return true;
    }
}
