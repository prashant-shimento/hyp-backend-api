package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class IdentityRequest {

    private Identity identity;

    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    @Builder
    public static class Identity {

        @JsonProperty("external_id")
        private String externalId;
    }
}
