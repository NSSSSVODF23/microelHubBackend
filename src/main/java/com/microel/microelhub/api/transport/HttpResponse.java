package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HttpResponse {
    private String error;
    private Boolean isError = false;
    private Object payload;

    public void setError(String error){
        this.error = error;
        this.isError = true;
    }

    public void setPayload(Object object){
        this.payload = object;
    }

    public static HttpResponse error(String error) {
        final HttpResponse response = new HttpResponse();
        response.setError(error);
        return response;
    }

    public static HttpResponse of(Object payload){
        final HttpResponse response = new HttpResponse();
        response.setPayload(payload);
        return response;
    }
}
