package com.microel.microelhub.services.vk;

import com.microel.microelhub.common.AttachmentsSavingController;
import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.storage.entity.MessageAttachment;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.objects.callback.MessageAllow;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VkUpdateHandler extends com.vk.api.sdk.events.longpoll.GroupLongPollApi {

    private final VkApiClient api;
    private final GroupActor actor;
    private final MessageAggregatorService messageAggregatorService;
    private final AttachmentsSavingController attachmentsSavingController;

    public VkUpdateHandler(VkApiClient client, GroupActor actor, int waitTime, MessageAggregatorService messageAggregatorService, AttachmentsSavingController attachmentsSavingController) {
        super(client, actor, waitTime);
        this.api = client;
        this.actor = actor;
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
                Photo photo = messageAttachment.getPhoto();
                if (photo == null) return;
                PhotoSizes size = photo.getSizes().stream().min((o1, o2) -> (o2.getWidth() + o2.getHeight()) - (o1.getWidth() + o1.getHeight())).orElse(null);
                if (size == null) return;
                MessageAttachment attachment = savePhoto(size);
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

    private MessageAttachment savePhoto(PhotoSizes photo) {
        return attachmentsSavingController.downloadAndSave(photo.getUrl().toString(), AttachmentType.PHOTO);
    }

}
