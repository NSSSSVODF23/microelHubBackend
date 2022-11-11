package com.microel.microelhub.storage.repository;

import com.microel.microelhub.storage.entity.Call;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CallRepository extends JpaRepository<Call, Long>, JpaSpecificationExecutor<Call> {
    Call findTopByPhone(String phone, Sort sort);
}
