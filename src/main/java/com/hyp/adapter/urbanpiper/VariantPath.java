package com.hyp.adapter.urbanpiper;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VariantPath {
    private List<VariantInfo> variants;
}
