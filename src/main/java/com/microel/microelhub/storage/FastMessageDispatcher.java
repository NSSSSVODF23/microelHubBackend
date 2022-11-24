package com.microel.microelhub.storage;

import com.microel.microelhub.storage.entity.FastMessage;
import com.microel.microelhub.storage.entity.Operator;
import com.microel.microelhub.storage.repository.FastMessageRepository;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class FastMessageDispatcher {
    private final FastMessageRepository fastMessageRepository;
    private final OperatorDispatcher operatorDispatcher;

    public FastMessageDispatcher(FastMessageRepository fastMessageRepository, OperatorDispatcher operatorDispatcher) {
        this.fastMessageRepository = fastMessageRepository;
        this.operatorDispatcher = operatorDispatcher;
    }

    public List<FastMessage> get(String login) throws Exception {
        if (login == null || login.isBlank()) throw new Exception("Пустой логин");
        final Operator operator = operatorDispatcher.getByLogin(login);
        if (operator == null) throw new Exception("Оператор не найден");
        return fastMessageRepository.findByOperatorOrderByCreatedDesc(operator);
    }

    public FastMessage add(String login, String message) throws Exception {
        if (login == null || login.isBlank()) throw new Exception("Пустой логин");
        if (message == null || message.isBlank()) throw new Exception("Пустое сообщение");
        final Operator operator = operatorDispatcher.getByLogin(login);
        if (operator == null) throw new Exception("Оператор не найден");
        return fastMessageRepository.save(FastMessage.builder().operator(operator).message(message).created(Timestamp.from(Instant.now())).build());
    }

    public FastMessage edit(Long fastMessageId, String message) throws Exception {
        if(fastMessageId == null) throw new Exception("Идентификатор сообщения пустой");
        final FastMessage fastMessage = fastMessageRepository.findById(fastMessageId).orElse(null);
        if (fastMessage == null) throw new Exception("Сообщение не найдено");
        fastMessage.setMessage(message);
        return fastMessageRepository.save(fastMessage);
    }

    public void remove(Long fastMessageId) throws Exception {
        if(fastMessageId == null) throw new Exception("Идентификатор сообщения пустой");
        final FastMessage fastMessage = fastMessageRepository.findById(fastMessageId).orElse(null);
        if (fastMessage == null) throw new Exception("Сообщение не найдено");
        fastMessageRepository.delete(fastMessage);
    }
}
