package com.microel.microelhub.common.chat;

@FunctionalInterface
public interface EditMessageHandle {
    void apply(String userId, String chatMsgId, String text) throws Exception;
}
