package com.hyp.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StoreStatusRequest {

	private String restID;
    private String status;
    private String store_status;
    private String turn_on_time;
    private String reason;
    private String http_code;
    private String message;

}
