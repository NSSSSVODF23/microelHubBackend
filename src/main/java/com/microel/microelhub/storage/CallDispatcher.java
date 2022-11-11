package com.microel.microelhub.storage;

import com.microel.microelhub.api.transport.PageRequest;
import com.microel.microelhub.storage.entity.Call;
import com.microel.microelhub.storage.repository.CallRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class CallDispatcher {
    private final CallRepository callRepository;

    public CallDispatcher(CallRepository callRepository) {
        this.callRepository = callRepository;
    }

    public Call create(String phone) {
        return callRepository.save(Call.builder().phone(phone).created(Timestamp.from(Instant.now())).processed(false).build());
    }

    public Page<Call> getPage(PageRequest pageRequest) {
        return callRepository.findAll(new OffsetRequest(pageRequest.getOffset(), pageRequest.getLimit(), Sort.by(Sort.Direction.DESC, "created")));
    }
}
