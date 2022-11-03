package com.microel.microelhub.api.transport;

import com.microel.microelhub.common.chat.Platform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiStatus {
    private Platform api;
    private String status;
}
