package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeChatOperatorRequest {
    private String chatId;
    private String login;
}
