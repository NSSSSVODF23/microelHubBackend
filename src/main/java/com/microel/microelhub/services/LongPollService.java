package com.microel.microelhub.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LongPollService {

    private final Map<LongPollIdentical, LongPollResolver> subscribers = new ConcurrentHashMap<>();
    private final Map<LongPollIdentical, List<LongPollResult>> cache = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    private void clearDataMapScheduled() {
        List<LongPollIdentical> expiredKeys = subscribers.keySet().stream().filter(key -> key.getActive().plus(30, ChronoUnit.MINUTES).isBefore(Instant.now())).collect(Collectors.toList());
        for (LongPollIdentical id : expiredKeys) {
            subscribers.remove(id);
            cache.remove(id);
        }
    }

    public void push(String id, LongPollResult data) {
        LongPollIdentical identical = new LongPollIdentical(id);
        LongPollResolver resolver = subscribers.get(identical);

        if (resolver == null) {
            if (!cache.containsKey(identical)) cache.put(identical, new ArrayList<>());
            cache.get(identical).add(data);
            return;
        }

        subscribers.remove(identical);
        cache.remove(identical);
        resolver.apply(data);
    }

    public void subscribe(String id, LongPollResolver callback) {
        LongPollIdentical identical = new LongPollIdentical(id);
        if (cache.containsKey(identical)) {
            List<LongPollResult> results = cache.get(identical);
            if (results.isEmpty()) {
                cache.remove(identical);
            } else {
                callback.apply(cache.get(identical).remove(0));
                return;
            }
        }
        subscribers.put(identical, callback);
    }
}
