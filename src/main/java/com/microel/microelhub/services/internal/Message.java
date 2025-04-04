package com.microel.microelhub.services.internal;

import com.microel.microelhub.storage.entity.MessageAttachment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String messageId;
    private UUID userId;
    private String message;
    private Boolean operatorMsg;
    private Boolean system;
    private Set<MessageAttachment> attachments;
}
