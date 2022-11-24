package com.microel.microelhub.storage.repository;

import com.microel.microelhub.storage.entity.FastMessage;
import com.microel.microelhub.storage.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface FastMessageRepository extends JpaRepository<FastMessage, Long>, JpaSpecificationExecutor<FastMessage> {
    List<FastMessage> findByOperatorOrderByCreatedDesc(Operator operator);
}
