package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyp.model.FeeRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeeDto extends BaseDto {

    private boolean active = true;

    @NotBlank(message = "Partner is required")
    private String partnerId;

    @Valid
    private List<FeeRule> feeRules;
}
