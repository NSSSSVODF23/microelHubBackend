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
                        Files.createDirectories(Path.of("./attachments", "photos"));
                        Files.write(Path.of("./attachments", "photos", fileName + ".jpg"), photo);
                        return messageAttachmentDispatcher.create(fileName, AttachmentType.PHOTO);
                    }
                } catch (Exception e) {
                    log.warn("Не удалось сохранить фото {}", e.getMessage());
                }
                return null;
            case VIDEO:
                try {
                    log.info("Ссылка на видео {}", uri);
                    byte[] video = new RestTemplate().getForObject(uri, byte[].class);
                    if (video != null) {
                        log.info("Размер видео {}", video.length);
                        UUID fileName = UUID.randomUUID();
                        Files.createDirectories(Path.of("./attachments", "videos"));
                        Files.write(Path.of("./attachments", "videos", fileName + ".mp4"), video);
                        return messageAttachmentDispatcher.create(fileName, AttachmentType.VIDEO);
                    }
                } catch (Exception e) {
                    log.warn("Не удалось сохранить видео {}", e.getMessage());
                }
                return null;
            case DOCUMENT:
                log.warn("Сохранение документов не реализовано");
                return null;
        }
        return null;
    }
}
