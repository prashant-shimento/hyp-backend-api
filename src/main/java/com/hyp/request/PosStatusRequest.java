package com.hyp.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosStatusRequest {

	private String restID;
    private String status;
    private String store_status;
    private String turn_on_time;
    private String reason;
    private String http_code;
    private String message;

}
