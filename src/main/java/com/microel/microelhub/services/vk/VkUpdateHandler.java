package com.microel.microelhub.services.vk;

import com.microel.microelhub.common.AttachmentsSavingController;
import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.storage.entity.MessageAttachment;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import com.vk.api.sdk.objects.video.Video;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VkUpdateHandler extends com.vk.api.sdk.events.longpoll.GroupLongPollApi {

    private final VkApiClient api;
    private final GroupActor actor;
    private final UserActor userActor;
    private final MessageAggregatorService messageAggregatorService;
    private final AttachmentsSavingController attachmentsSavingController;

    public VkUpdateHandler(VkApiClient client, GroupActor actor, UserActor userActor, int waitTime, MessageAggregatorService messageAggregatorService, AttachmentsSavingController attachmentsSavingController) {
        super(client, actor, waitTime);
        this.api = client;
        this.actor = actor;
        this.userActor = userActor;
        this.messageAggregatorService = messageAggregatorService;
        this.attachmentsSavingController = attachmentsSavingController;
    }

//    @Override
//    public void messageNew(Integer groupId, String secret, Message message) {
//        messageNew(groupId, message);
//    }

    @Override
    public void messageNew(Integer groupId, Message message) {
        try {
            List<MessageAttachment> messageAttachments = new ArrayList<>();
            message.getAttachments().forEach(messageAttachment -> {
                MessageAttachment attachment = null;
                log.info("Taken: {}", messageAttachment.getType().getValue());
                switch (messageAttachment.getType()) {
                    case VIDEO:
                        Video video = messageAttachment.getVideo();
                        log.info(video.toPrettyString());
                        getVideoUrl(video);
                        attachment = saveAttachment(video);
                        break;
                    case PHOTO:
                        Photo photo = messageAttachment.getPhoto();
                        PhotoSizes size = photo.getSizes().stream().min((o1, o2) -> (o2.getWidth() + o2.getHeight()) - (o1.getWidth() + o1.getHeight())).orElse(null);
                        if (size != null)
                            attachment = saveAttachment(size);
                        break;
                }

                if (attachment != null) {
                    messageAttachments.add(attachment);
                }
            });

            api.messages().markAsRead(actor).peerId(message.getPeerId()).execute();

            List<GetResponse> list = api.users().get(actor).userIds(message.getPeerId().toString()).execute();
            String name = "";

            if (!list.isEmpty()) {
                GetResponse response = list.get(0);
                if (response.getFirstName() == null && response.getLastName() == null) {
                    name = response.getNickname();
                }
                if (response.getFirstName() != null) {
                    name += response.getFirstName();
                }
                if (response.getLastName() != null) {
                    name += " " + response.getLastName();
                }
            }

            messageAggregatorService.nextMessageFromUser(message.getPeerId().toString(), message.getText(), message.getId().toString(), name, Platform.VK, messageAttachments.toArray(MessageAttachment[]::new));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void messageEdit(Integer groupId, Message message) {
        try {
            messageAggregatorService.editMessageFromUser(message.getPeerId().toString(), message.getText(), message.getId().toString(), Platform.VK);
        } catch (Exception ignored) {
        }
    }

    private MessageAttachment saveAttachment(PhotoSizes photo) {
        return attachmentsSavingController.downloadAndSave(photo.getUrl().toString(), AttachmentType.PHOTO);
    }

    private MessageAttachment saveAttachment(Video video) {
        return attachmentsSavingController.downloadAndSave(video.getPlayer().toString(), AttachmentType.VIDEO);
    }

    private String getVideoUrl(Video video) {
        StringBuilder videoToken = new StringBuilder();
        videoToken.append(video.getOwnerId().toString());
        videoToken.append("_").append(video.getId());
        if (video.getAccessKey() != null) videoToken.append("_").append(video.getAccessKey());
        try {
            log.info("Отправка запроса {}", videoToken.toString());
            com.vk.api.sdk.objects.video.responses.GetResponse response = api.videos().get(userActor).videos(videoToken.toString()).execute();
            log.info(response.toPrettyString());
            log.info(response.getItems().get(0).getFiles().toPrettyString());
        } catch (ApiException | ClientException e) {
            log.info("Ошибка получения видеозаписи {}", e.getMessage());
        }
        return null;
    }
}
