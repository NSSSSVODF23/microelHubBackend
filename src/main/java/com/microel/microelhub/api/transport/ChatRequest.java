package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    private Boolean isActive;
    private Boolean isProcessed;
    private Long offset;
    private Integer limit;
}
