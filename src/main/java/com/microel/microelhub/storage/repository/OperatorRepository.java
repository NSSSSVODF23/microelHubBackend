package com.microel.microelhub.storage.repository;

import com.microel.microelhub.storage.OffsetRequest;
import com.microel.microelhub.storage.entity.Operator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, String>, JpaSpecificationExecutor<Operator> {
    Optional<Operator> findByLoginAndDeleted(String login, boolean deleted);

    Page<Operator> findByDeleted(boolean deleted, Pageable pageable);
}
