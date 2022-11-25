package com.microel.microelhub.services;

@FunctionalInterface
public interface LongPollResolver {
    void apply(LongPollResult result);
}
