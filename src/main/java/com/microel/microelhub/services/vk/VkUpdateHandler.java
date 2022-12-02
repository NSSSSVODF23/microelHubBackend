package com.microel.microelhub.services.vk;

import com.microel.microelhub.common.AttachmentsController;
import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.storage.entity.MessageAttachment;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.audio.Audio;
import com.vk.api.sdk.objects.messages.AudioMessage;
import com.vk.api.sdk.objects.messages.ForeignMessage;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoSizes;
import com.vk.api.sdk.objects.users.responses.GetResponse;
import com.vk.api.sdk.objects.video.Video;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class VkUpdateHandler extends com.vk.api.sdk.events.longpoll.GroupLongPollApi {

    private final VkApiClient api;
    private final GroupActor actor;
    private final UserActor userActor;
    private final MessageAggregatorService messageAggregatorService;
    private final AttachmentsController attachmentsController;

    public VkUpdateHandler(VkApiClient client, GroupActor actor, UserActor userActor, int waitTime, MessageAggregatorService messageAggregatorService, AttachmentsController attachmentsController) {
        super(client, actor, waitTime);
        this.api = client;
        this.actor = actor;
        this.userActor = userActor;
        this.messageAggregatorService = messageAggregatorService;
        this.attachmentsController = attachmentsController;
    }

//    @Override
//    public void messageNew(Integer groupId, String secret, Message message) {
//        messageNew(groupId, message);
//    }

    @Override
    public void messageNew(Integer groupId, Message message) {
        try {
            StringBuilder messageText = new StringBuilder();
            List<MessageAttachment> attachments = new ArrayList<>(parseAttachments(message.getAttachments()));
            List<ForeignMessage> foreignMessages = new ArrayList<>();
            if (message.getReplyMessage() != null) foreignMessages.add(message.getReplyMessage());
            foreignMessages.addAll(message.getFwdMessages());

            ParsedFwdMessage fwdMessage = parseFwdMessage(foreignMessages);
            messageText.append(fwdMessage.getText());
            attachments.addAll(fwdMessage.getAttachments());

            if(message.getText() != null && !message.getText().isBlank()) messageText.append("\r\n").append(message.getText());

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

            messageAggregatorService.nextMessageFromUser(message.getPeerId().toString(), messageText.toString(), message.getId().toString(), name, Platform.VK, attachments.toArray(MessageAttachment[]::new));
        } catch (Exception e) {
            log.warn(e.toString());
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
        return attachmentsController.downloadAndSave(photo.getUrl().toString(), AttachmentType.PHOTO);
    }

    private MessageAttachment saveAttachment(Audio audio) {
        return attachmentsController.downloadAndSave(audio.getUrl().toString(), AttachmentType.AUDIO, audio.getDuration());
    }

    private MessageAttachment saveAttachment(AudioMessage audio) {
        return attachmentsController.downloadAndSave(audio.getLinkMp3().toString(), AttachmentType.AUDIO, audio.getDuration());
    }

    private ParsedFwdMessage parseFwdMessage(List<ForeignMessage> fwdMessages) {
        boolean first = true;
        ParsedFwdMessage parsedFwdMessage = new ParsedFwdMessage();
        for (ForeignMessage fwdMessage : fwdMessages) {
            if(fwdMessage.getText() != null && !fwdMessage.getText().isBlank()){
                if(!first) {
                    parsedFwdMessage.getText().append("\r\n");
                }
                parsedFwdMessage.getText().append(fwdMessage.getText());
                first=false;
            }
            parsedFwdMessage.getAttachments().addAll(parseAttachments(fwdMessage.getAttachments()));
            List<ForeignMessage> nextForeignMessages = new ArrayList<>();
            if (fwdMessage.getReplyMessage() != null) nextForeignMessages.add(fwdMessage.getReplyMessage());
            if (fwdMessage.getFwdMessages() != null) nextForeignMessages.addAll(fwdMessage.getFwdMessages());
            if (nextForeignMessages.size() > 0) parsedFwdMessage.unite(parseFwdMessage(nextForeignMessages));
        }
        return parsedFwdMessage;
    }

    private List<MessageAttachment> parseAttachments(List<com.vk.api.sdk.objects.messages.MessageAttachment> attachments) {
        List<MessageAttachment> messageAttachments = new ArrayList<>();
        try {
            attachments.forEach(messageAttachment -> {
                MessageAttachment attachment = null;
                switch (messageAttachment.getType()) {
                    case VIDEO:
                        Video video = messageAttachment.getVideo();
                        messageAttachments.addAll(saveAttachment(video));
                        break;
                    case PHOTO:
                        Photo photo = messageAttachment.getPhoto();
                        PhotoSizes size = photo.getSizes().stream().min((o1, o2) -> (o2.getWidth() + o2.getHeight()) - (o1.getWidth() + o1.getHeight())).orElse(null);
                        if (size != null)
                            attachment = saveAttachment(size);
                        break;
                    case AUDIO:
                        Audio audio = messageAttachment.getAudio();
                        if (audio != null)
                            attachment = saveAttachment(audio);
                        break;
                    case AUDIO_MESSAGE:
                        AudioMessage audioMessage = messageAttachment.getAudioMessage();
                        if (audioMessage != null)
                            attachment = saveAttachment(audioMessage);
                        break;
                }

                if (attachment != null) {
                    messageAttachments.add(attachment);
                }
            });
        } catch (Exception ignore) {

        }
        return messageAttachments;
    }

    private List<MessageAttachment> saveAttachment(Video... video) {
        List<MessageAttachment> attachments = new ArrayList<>();

        for (Video v : video) {
            StringBuilder videoToken = new StringBuilder();
            videoToken.append(v.getOwnerId().toString());
            videoToken.append("_").append(v.getId());
            if (v.getAccessKey() != null) videoToken.append("_").append(v.getAccessKey());
            attachments.add(attachmentsController.appendLink("Видео " + v.getTitle(), videoToken.toString()));
        }

        return attachments;
    }

    @Getter
    @Setter
    private static class ParsedFwdMessage {
        private StringBuilder text = new StringBuilder();
        private List<MessageAttachment> attachments = new ArrayList<>();

        public void unite(ParsedFwdMessage message) {
            text.append("\r\n").append(message.getText());
            attachments.addAll(message.getAttachments());
        }
    }
}
