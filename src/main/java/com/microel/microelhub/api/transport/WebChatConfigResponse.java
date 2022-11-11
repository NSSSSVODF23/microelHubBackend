package com.microel.microelhub.api.transport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WebChatConfigResponse {
    private Boolean isWork;
    private String warningMessage;
    private String vkUrl;
    private String telegramUrl;
}
