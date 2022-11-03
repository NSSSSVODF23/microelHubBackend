package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessagesRequest {
    private String chatId;
    private Long offset;
    private Integer limit;
}
