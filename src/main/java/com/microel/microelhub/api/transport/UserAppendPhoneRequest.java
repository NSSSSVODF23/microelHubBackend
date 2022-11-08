package com.microel.microelhub.api.transport;

import com.microel.microelhub.common.chat.Platform;
import lombok.Getter;

@Getter
public class UserAppendPhoneRequest {
    private String userId;
    private Platform platform;
    private String phone;
}
