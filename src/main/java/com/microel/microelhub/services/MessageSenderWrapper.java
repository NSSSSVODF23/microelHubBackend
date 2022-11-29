package com.microel.microelhub.services;

import com.microel.microelhub.storage.entity.MessageAttachment;

import java.util.List;

public interface MessageSenderWrapper {
    String sendMessage(String userId, String text, List<MessageAttachment> imageAttachments);
    void editMessage(String userId, String chatMsgId, String text) throws Exception;
    void deleteMessage(String userId, String chatMsgId) throws Exception;
}
