package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TaxTransformer {

    public List<PosDataRequest.TaxRequest> transform(UrbanPiperMenuRequest.BillComponents billComponents) {
        List<PosDataRequest.TaxRequest> taxes = new ArrayList<>();

        if (billComponents != null && billComponents.getTaxes() != null) {
            for (UrbanPiperMenuRequest.Tax tax : billComponents.getTaxes()) {
                PosDataRequest.TaxRequest taxRequest = PosDataRequest.TaxRequest.builder()
                        .taxid(tax.getRefId())
                        .taxname(tax.getTitle())
                        .tax(tax.getValue() != null ? String.valueOf(tax.getValue()) : null)
                        .taxtype("PERCENTAGE")
                        .description(tax.getDescription())
                        .active("1")
                        .build();
                taxes.add(taxRequest);
            }
        }

        return taxes;
    }
}
