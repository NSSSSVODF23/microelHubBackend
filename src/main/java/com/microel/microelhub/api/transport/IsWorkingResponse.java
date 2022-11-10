package com.microel.microelhub.api.transport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IsWorkingResponse {
    private Boolean isWork;
    private String warningMessage;
}
