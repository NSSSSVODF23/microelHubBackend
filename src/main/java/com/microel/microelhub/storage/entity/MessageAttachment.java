package com.microel.microelhub.storage.entity;

import com.microel.microelhub.common.chat.AttachmentType;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "message_attachments")
public class MessageAttachment {
    @Id
    @Column(nullable = false)
    private UUID attachmentId;
    private AttachmentType attachmentType;
    private String description;
    private String data;
    private Timestamp created;
}
