package com.hyp.adapter;

import com.hyp.adapter.urbanpiper.UrbanPiperMenuTransformer;
import com.hyp.request.PosDataRequest;
import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrbanPiperAdapter {

    private final UrbanPiperMenuTransformer menuTransformer;

    public PosDataRequest transformMenuPayload(UrbanPiperMenuRequest request) {
        return menuTransformer.transform(request);
    }
}
