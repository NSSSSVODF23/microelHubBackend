package com.microel.microelhub.common.chat;

import com.microel.microelhub.storage.entity.MessageAttachment;

import java.util.List;

@FunctionalInterface
public interface NewMessageHandle {
    String apply(String userId, String text, List<MessageAttachment> imageAttachments);
}
