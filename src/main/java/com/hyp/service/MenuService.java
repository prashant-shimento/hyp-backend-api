package com.hyp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hyp.request.PosDataRequest;
import com.hyp.translation.SwiggyMenuTranslation;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    public PosDataRequest extract(JsonNode rawJson, boolean useExternalId) {
        return SwiggyMenuTranslation.runExtraction(rawJson, useExternalId);
    }
}
