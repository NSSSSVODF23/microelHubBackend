package com.microel.microelhub.common;

import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.storage.MessageAttachmentDispatcher;
import com.microel.microelhub.storage.entity.MessageAttachment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Component
public class AttachmentsController {
    private final MessageAttachmentDispatcher messageAttachmentDispatcher;

    public AttachmentsController(MessageAttachmentDispatcher messageAttachmentDispatcher) {
        this.messageAttachmentDispatcher = messageAttachmentDispatcher;
    }

    public MessageAttachment downloadAndSave(String uri, AttachmentType type) {
        return downloadAndSave(uri, type, null);
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

    public MessageAttachment appendLink(String description, String data) {
        UUID uuid = UUID.randomUUID();
        return messageAttachmentDispatcher.create(uuid, AttachmentType.VIDEO_LINK, description, data);
    }

    public MessageAttachment decodeImageAndSave(String base64encoded) throws Exception {
        byte[] photo = Base64.decodeBase64(base64encoded.split(",")[1]);
        if (photo == null || photo.length == 0)
            throw new Exception("Пустое декодированное изображение");

        try {
            final InputStream inputStream = new ByteArrayInputStream(photo);
            final BufferedImage image = ImageIO.read(inputStream);
            inputStream.close();

            final BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            convertedImage.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);

            UUID fileName = UUID.randomUUID();
            Files.createDirectories(Path.of("./attachments", "photos"));
            final FileOutputStream fileOutputStream = new FileOutputStream(Path.of("./attachments", "photos", fileName + ".jpg").toFile());
            final boolean canWrite = ImageIO.write(convertedImage, "jpg", fileOutputStream);
            fileOutputStream.close();

            if (!canWrite) {
                throw new IllegalStateException("Failed to write image.");
            }
            return messageAttachmentDispatcher.create(fileName, AttachmentType.PHOTO);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public File getFile(String id, AttachmentType type) {
        switch (type) {
            case PHOTO:
                return Path.of("./attachments", "photos", id + ".jpg").toFile();
            case VIDEO:
                return Path.of("./attachments", "videos", id + ".mp4").toFile();
            case AUDIO:
                return Path.of("./attachments", "audios", id + ".mp3").toFile();
            case DOCUMENT:
                log.warn("Сохранение документов не реализовано");
                return null;
        }
        return null;
    }

    public File getFile(MessageAttachment attachment) {
        return getFile(attachment.getAttachmentId().toString(), attachment.getAttachmentType());
    }
}
