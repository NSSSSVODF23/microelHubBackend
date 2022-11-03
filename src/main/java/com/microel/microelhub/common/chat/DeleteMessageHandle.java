package com.microel.microelhub.common.chat;

@FunctionalInterface
public interface DeleteMessageHandle {
    void apply(String userId, String chatMsgId) throws Exception;
}
