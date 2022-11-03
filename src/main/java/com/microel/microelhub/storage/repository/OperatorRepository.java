package com.microel.microelhub.storage.repository;

import com.microel.microelhub.storage.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperatorRepository extends JpaRepository<Operator, String>, JpaSpecificationExecutor<Operator> {
}
