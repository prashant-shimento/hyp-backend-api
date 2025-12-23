package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class OneSignalNotificationRequest {

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("include_aliases")
    private OneSignalNotificationAlias includeAliases;

    @JsonProperty("included_segments")
    private List<String> includedSegments;

    @JsonProperty("data")
    private Map<String, Object> customData;

    @JsonProperty("target_channel")
    private String targetChannel;

    @JsonProperty("contents")
    private Map<String, String> contents;
}
