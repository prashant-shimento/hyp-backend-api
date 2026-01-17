package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferralCodeDto {

    private String id;

    private String code;

    @NotBlank(message = "Referrer name cannot be blank")
    private String referrerName;

    private boolean active;

    private Integer attributionWindowDays;

    private String partnerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean isDeleted;
}
