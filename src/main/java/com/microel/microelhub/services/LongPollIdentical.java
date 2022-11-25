package com.microel.microelhub.services;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
public class LongPollIdentical {
    @NonNull
    private String id;
    private Instant active = Instant.now();

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongPollIdentical)) return false;
        LongPollIdentical that = (LongPollIdentical) o;
        return getId().equals(that.getId());
    }
}
