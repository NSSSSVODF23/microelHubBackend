package com.microel.microelhub.storage.repository;

import com.microel.microelhub.storage.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID>, JpaSpecificationExecutor<MessageAttachment> {
}
