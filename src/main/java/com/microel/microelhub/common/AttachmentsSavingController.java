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
        return  downloadAndSave(uri,type,null);
    }

    public MessageAttachment downloadAndSave(String uri, AttachmentType type, Integer duration) {
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
                    byte[] video = new RestTemplate().getForObject(uri, byte[].class);
                    if (video != null) {
                        UUID fileName = UUID.randomUUID();
                        Files.createDirectories(Path.of("./attachments", "videos"));
                        Files.write(Path.of("./attachments", "videos", fileName + ".mp4"), video);
                        return messageAttachmentDispatcher.create(fileName, AttachmentType.VIDEO, duration);
                    }
                } catch (Exception e) {
                    log.warn("Не удалось сохранить видео {}", e.getMessage());
                }
                return null;
            case AUDIO:
                try {
                    byte[] audio = new RestTemplate().getForObject(uri, byte[].class);
                    if (audio != null) {
                        UUID fileName = UUID.randomUUID();
                        Files.createDirectories(Path.of("./attachments", "audios"));
                        Files.write(Path.of("./attachments", "audios", fileName + ".mp3"), audio);
                        return messageAttachmentDispatcher.create(fileName, AttachmentType.AUDIO, duration);
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

    public MessageAttachment appendLink(String description, String data){
        UUID uuid = UUID.randomUUID();
        return messageAttachmentDispatcher.create(uuid, AttachmentType.VIDEO_LINK, description, data);
    }
}
