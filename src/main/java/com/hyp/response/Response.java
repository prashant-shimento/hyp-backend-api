package com.hyp.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private Integer code;
    private Integer http_code;
    private String message;
    private String status;
    private String store_status;

    public static class Builder {
        private Integer code;
        private Integer http_code;
        private String message;
        private String status;
        private String store_status;

        public Builder code(Integer code) {
            this.code = code;
            return this;
        }

        public Builder httpCode(Integer httpCode) {
            this.http_code = httpCode;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder storeStatus(String storeStatus) {
            this.store_status = storeStatus;
            return this;
        }

        public Response build() {
            Response response = new Response();
            response.code = this.code;
            response.http_code = this.http_code;
            response.message = this.message;
            response.status = this.status;
            response.store_status = this.store_status;
            return response;
        }
    }
}

