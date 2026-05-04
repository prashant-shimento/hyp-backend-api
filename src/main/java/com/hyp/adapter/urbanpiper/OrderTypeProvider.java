package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderTypeProvider {

    public List<PosDataRequest.OrderTypeRequest> getDefaultOrderTypes() {
        List<PosDataRequest.OrderTypeRequest> orderTypes = new ArrayList<>();
        orderTypes.add(new PosDataRequest.OrderTypeRequest(1, "Delivery"));
        orderTypes.add(new PosDataRequest.OrderTypeRequest(2, "Pickup"));
        orderTypes.add(new PosDataRequest.OrderTypeRequest(3, "Dine-in"));
        return orderTypes;
    }
}
