package com.microel.microelhub.common;

import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.storage.MessageAttachmentDispatcher;
import com.microel.microelhub.storage.entity.MessageAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Component
public class AttachmentsSavingController {
    private final MessageAttachmentDispatcher messageAttachmentDispatcher;

    public AttachmentsSavingController(MessageAttachmentDispatcher messageAttachmentDispatcher) {
        this.messageAttachmentDispatcher = messageAttachmentDispatcher;
    }

    public MessageAttachment downloadAndSave(String uri, AttachmentType type) {
        switch (type) {
            case PHOTO:
                try {
                    byte[] photo = new RestTemplate().getForObject(uri, byte[].class);
                    if (photo != null) {
                        UUID fileName = UUID.randomUUID();
                        Files.createDirectories(Path.of("./attachments","photos"));
                        Files.write(Path.of("./attachments", "photos", fileName + ".jpg"), photo);
                        return messageAttachmentDispatcher.create(fileName,AttachmentType.PHOTO);
                    }
                } catch (Exception e) {
                    log.warn("Не удалось сохранить фото {}", e.getMessage());
                }
                return null;
            case VIDEO:
                log.warn("Сохранение видео не реализовано");
                return null;
            case DOCUMENT:
                log.warn("Сохранение документов не реализовано");
                return null;
        }
        return null;
    }
}
