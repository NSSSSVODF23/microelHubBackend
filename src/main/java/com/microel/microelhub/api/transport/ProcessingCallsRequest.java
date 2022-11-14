package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProcessingCallsRequest {
    private List<Long> callIds;
    private String operatorLogin;
}
