package com.microel.microelhub.storage;

import com.microel.microelhub.api.transport.PageRequest;
import com.microel.microelhub.common.OperatorGroup;
import com.microel.microelhub.storage.entity.Operator;
import com.microel.microelhub.storage.repository.OperatorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class OperatorDispatcher {
    private final OperatorRepository operatorRepository;

    public OperatorDispatcher(OperatorRepository operatorRepository) {
        this.operatorRepository = operatorRepository;
    }

    public Operator getByLogin(String login) {
        return operatorRepository.findById(login).orElse(null);
    }

    public Operator deleteOperator(String login) throws Exception {
        Operator operator = operatorRepository.findById(login).orElse(null);
        if(operator == null) throw new Exception("Оператор не найден");
        operatorRepository.delete(operator);
        return operator;
    }

    public void create(Operator operator) throws Exception {
        if(operator.getLogin() == null || operator.getLogin().isBlank()) throw new Exception("Логин не может быть пустым");
        if(operator.getPassword() == null || operator.getPassword().isBlank()) throw new Exception("Пароль не может быть пустым");
        if(operator.getRole() == null) operator.setRole(OperatorGroup.USER);
        Operator foundedOperator = operatorRepository.findById(operator.getLogin()).orElse(null);
        if(foundedOperator != null) throw new Exception("Уже существует оператор с логином "+operator.getLogin());
        operator.setCreated(Timestamp.from(Instant.now()));
        operatorRepository.save(operator);
    }

    public void edit(Operator operator) throws Exception {
        if(operator.getLogin() == null || operator.getLogin().isBlank()) throw new Exception("Логин не может быть пустым");
        if(operator.getPassword() == null || operator.getPassword().isBlank()) throw new Exception("Пароль не может быть пустым");
        if(operator.getRole() == null) operator.setRole(OperatorGroup.USER);
        Operator foundedOperator = operatorRepository.findById(operator.getLogin()).orElse(null);
        if(foundedOperator == null) throw new Exception("Не найден оператор для редактирования "+operator.getLogin());
        foundedOperator.setName(operator.getName());
        foundedOperator.setPassword(operator.getPassword());
        foundedOperator.setRole(operator.getRole());
        operatorRepository.save(foundedOperator);
    }

    public Operator setAvatar(String login, String avatar) throws Exception {
        if(login == null || login.isBlank()) throw new Exception("Логин не может быть пустым");
        if(avatar == null || avatar.isBlank()) throw new Exception("Пароль не может быть пустым");

        Operator foundedOperator = operatorRepository.findById(login).orElse(null);
        if(foundedOperator == null) throw new Exception("Не найден оператор для редактирования "+login);
        foundedOperator.setAvatar(avatar);
        return operatorRepository.save(foundedOperator);
    }

    public Page<Operator> getPage(PageRequest body) {
        return operatorRepository.findAll(new OffsetRequest(body.getOffset(),body.getLimit(), Sort.by(Sort.Direction.DESC, "created")));
    }

    public void updateLastLoginTime(Operator operator) {
        operator.setLastLogin(Timestamp.from(Instant.now()));
        operatorRepository.save(operator);
    }
}
