package com.microel.microelhub.api.transport;

import com.microel.microelhub.common.chat.Platform;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageBody {
    private String chatId;
    private Platform platform;
    private String text;
}
