package com.microel.microelhub.storage.repository;

import com.microel.microelhub.storage.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ConfigurationRepository extends JpaRepository<Configuration, Long>, JpaSpecificationExecutor<Configuration> {
    Optional<Configuration> findTopByOrderByChangeDesc();
}
