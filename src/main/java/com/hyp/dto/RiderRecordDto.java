package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiderRecordDto {

    private String id;
    private String riderName;
    private String riderContact;
    private Integer fraudCount;
    private List<String> channels = new ArrayList<>();
}
