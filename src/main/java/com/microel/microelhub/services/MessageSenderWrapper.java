package com.microel.microelhub.services;

public interface MessageSenderWrapper {
    String sendMessage(String userId, String text);
    void editMessage(String userId, String chatMsgId, String text) throws Exception;
    void deleteMessage(String userId, String chatMsgId) throws Exception;
}
