package com.microel.microelhub.api.transport;

import com.microel.microelhub.common.chat.Platform;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessagesRequest {
    private String chatId;
    private Long offset;
    private Integer limit;
}
