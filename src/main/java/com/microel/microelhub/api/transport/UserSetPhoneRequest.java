package com.microel.microelhub.api.transport;

import com.microel.microelhub.common.chat.Platform;
import lombok.Getter;

@Getter
public class UserSetPhoneRequest {
    private String userId;
    private Platform platform;
    private String login;
}
