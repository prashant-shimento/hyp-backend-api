package com.hyp.model;

import java.util.List;

import lombok.Data;

@Data
public class PlacePredictionData {

    private List<Suggestion> suggestions;

    @Data
    public static class Suggestion {
        private PlacePrediction placePrediction;
    }
    
    @Data
    public static class PlacePrediction {
        private String place;
        private String placeId;
        private Text text;
    }

    @Data
    public static class Text {
        private String text;
    }
}
