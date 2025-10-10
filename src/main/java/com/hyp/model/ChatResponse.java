package com.hyp.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {

    private List<ChatData> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatData {

        private String mobileNumber;

        private String lastMessageDateTimeUTC;

        private String lastMessage;
    }
}
