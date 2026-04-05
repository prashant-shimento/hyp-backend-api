package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
