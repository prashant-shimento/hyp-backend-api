package com.hyp.model;

import com.hyp.dto.AddressDto;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaceData {

    private AddressDto address;
    private List<String> restaurants;
}
