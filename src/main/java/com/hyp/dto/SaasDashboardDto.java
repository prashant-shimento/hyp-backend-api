package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaasDashboardDto {

    private String id;

    private String name;

    private String domain;

    private Boolean active;

    private String colorTheme;

    private String logoUrl;

    private Boolean serviceable;

    private String altContact;

    private String launched;

    private String address;

    private String mapUrl;

    private String email;

    private String googleAnalytics;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
