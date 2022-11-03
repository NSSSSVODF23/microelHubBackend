package com.microel.microelhub.common.chat;

@FunctionalInterface
public interface NewMessageHandle {
    String apply(String userId, String text);
}
