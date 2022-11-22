package com.microel.microelhub.storage;

import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.storage.entity.MessageAttachment;
import com.microel.microelhub.storage.repository.MessageAttachmentRepository;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
public class MessageAttachmentDispatcher {
    private final MessageAttachmentRepository messageAttachmentRepository;

    public MessageAttachmentDispatcher(MessageAttachmentRepository messageAttachmentRepository) {
        this.messageAttachmentRepository = messageAttachmentRepository;
    }

    public MessageAttachment create(UUID uuid, AttachmentType type){
        return messageAttachmentRepository.save(MessageAttachment.builder().attachmentId(uuid).attachmentType(type).created(Timestamp.from(Instant.now())).build());
    }

    public MessageAttachment create(UUID uuid, AttachmentType type, String description, String data){
        return messageAttachmentRepository.save(MessageAttachment.builder().attachmentId(uuid).attachmentType(type).description(description).data(data).created(Timestamp.from(Instant.now())).build());
    }
}
