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
public class AddonGroupDto extends BaseDto {

    @NotBlank(message = "Addon Group Name cannot be empty")
    private String addonGroupName;

    private String addonGroupRank;
    private String active = "1";

    @NotBlank(message = "Selection Max cannot be null")
    @Pattern(regexp = "\\d+", message = "Selection Max must be a valid integer")
    private String addonItemSelectionMax;

    @Pattern(regexp = "\\d+", message = "Selection Min must be a valid integer")
    @NotBlank(message = "Selection Min cannot be null")
    private String addonItemSelectionMin;

    private List<String> addonGroupItems;
    private List<AddonItemDto> addonItems;
}
