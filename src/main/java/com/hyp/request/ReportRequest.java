package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyp.enums.ReportFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportRequest {

    @NotNull(message = "Parameters cannot be null")
    @NotEmpty(message = "Parameters cannot be empty")
    private Map<String, Object> parameters;

    @NotNull(message = "Report format cannot be null")
    private ReportFormat format;
}
