package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class VariationDto extends BaseDto {

    @NotBlank(message = "Variation name cannot be empty")
    private String name;

    @NotBlank(message = "Variation group name cannot be empty")
    private String groupName;

    private String status;

    @NotBlank(message = "Variation price cannot be empty")
    @Pattern(regexp = "\\d+", message = "Variation price must be numeric")
    private String price;

    private String active = "1";
    private String itemPackingCharges;
    private String variationRank;
    private List<String> addonGroupId;
    private int variationAllowAddon;
    private String variationId;
    private transient List<AddonGroupDto> addonGroups;
}
