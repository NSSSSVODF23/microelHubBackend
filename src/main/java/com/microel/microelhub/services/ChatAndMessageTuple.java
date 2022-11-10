package com.microel.microelhub.services;

import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ChatAndMessageTuple {
    private Chat chat;
    private Message message;
}
