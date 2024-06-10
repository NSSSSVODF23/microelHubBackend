package com.microel.microelhub.services.telegram;

import com.microel.microelhub.common.AttachmentsController;
import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.services.MessageSenderWrapper;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import com.microel.microelhub.storage.entity.Configuration;
import com.microel.microelhub.storage.entity.MessageAttachment;
import com.microel.tdo.network.NetworkFile;
import com.microel.tdo.network.NetworkMediaGroup;
import com.microel.tdo.network.NetworkSendPhoto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramService implements MessageSenderWrapper {

    private final RestTemplate restTemplate = new RestTemplateBuilder().build();
    private final MessageAggregatorService messageAggregatorService;
    private final ConfigurationDispatcher configurationDispatcher;
    private final AttachmentsController attachmentsController;
    private Configuration config;

    public TelegramService(@Lazy MessageAggregatorService messageAggregatorService, ConfigurationDispatcher configurationDispatcher, AttachmentsController attachmentsController) {
        this.messageAggregatorService = messageAggregatorService;
        this.configurationDispatcher = configurationDispatcher;
        this.attachmentsController = attachmentsController;
    }

    public void updateCredentials() throws Exception {
        config = configurationDispatcher.getLastConfig();
        if (config == null || config.getTlgBotUsername() == null || config.getTlgBotToken() == null)
            throw new Exception("Реквизиты для инициализации API пусты");
    }

    public String getBotUsername() {
        return config.getTlgBotUsername();
    }

    public String getBotToken() {
        return config.getTlgBotToken();
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Message replyMessage = message.getReplyToMessage();

            User user = message.getFrom();
//            if (message.isCommand() && message.getText().equals("/start")) {
//                SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), "Вы можете написать сообщение этому боту, и в ближайшее время Вам ответит наш менеджер.");
//                execute(sendMessage);
//                return;
//            }
            String fullName = user.getFirstName();
            if (user.getLastName() != null) {
                fullName += " " + user.getLastName();
            }

            ParsedMessage mainMessage = parseMessage(message);
            if (replyMessage != null) {
                mainMessage.unite(parseMessage(replyMessage));
            }

            messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), mainMessage.text.toString(), message.getMessageId().toString(), fullName, Platform.TELEGRAM, mainMessage.getAttachments().toArray(MessageAttachment[]::new));
        } else if (update.hasEditedMessage()) {
            Message editedMessage = update.getEditedMessage();
            messageAggregatorService.editMessageFromUser(editedMessage.getChatId().toString(), editedMessage.getText(), editedMessage.getMessageId().toString(), Platform.TELEGRAM);
        }
    }

    private ParsedMessage parseMessage(Message message) {
        ParsedMessage parsedMessage = new ParsedMessage();
        if (message.hasVideo()) {
            MessageAttachment messageAttachment = this.saveAttachment(message.getVideo());
            if (messageAttachment != null) {
                parsedMessage.getText().append(message.getCaption());
                parsedMessage.getAttachments().add(messageAttachment);
                return parsedMessage;
            }
        } else if (message.hasVideoNote()) {
            MessageAttachment messageAttachment = this.saveAttachment(message.getVideoNote());
            if (messageAttachment != null) {
                parsedMessage.getText().append(message.getCaption());
                parsedMessage.getAttachments().add(messageAttachment);
                return parsedMessage;
            }
        } else if (message.hasAudio()) {
            MessageAttachment messageAttachment = this.saveAttachment(message.getAudio());
            if (messageAttachment != null) {
                parsedMessage.getText().append(message.getCaption());
                parsedMessage.getAttachments().add(messageAttachment);
                return parsedMessage;
            }
        } else if (message.hasVoice()) {
            MessageAttachment messageAttachment = this.saveAttachment(message.getVoice());
            if (messageAttachment != null) {
                parsedMessage.getText().append(message.getCaption());
                parsedMessage.getAttachments().add(messageAttachment);
                return parsedMessage;
            }
        } else if (message.hasPhoto()) {
            MessageAttachment messageAttachment = this.saveAttachment(message.getPhoto().get(message.getPhoto().size() - 1));
            if (messageAttachment != null) {
                parsedMessage.getText().append(message.getCaption());
                parsedMessage.getAttachments().add(messageAttachment);
                return parsedMessage;
            }
        }
        parsedMessage.getText().append(message.getText());
        return parsedMessage;
    }

    @Override
    public String sendMessage(String userId, String text, List<MessageAttachment> imageAttachments) {
        if (imageAttachments.size() > 1) {
            List<NetworkFile> files  = new ArrayList<>();
            for(int i = 0; i < imageAttachments.size(); i++)  {
                final MessageAttachment attachment = imageAttachments.get(i);
                java.io.File file = attachmentsController.getFile(attachment);
                try {
                    files.add(NetworkFile.from(file));
                } catch (IOException ignored) {
                }
            }
            final NetworkMediaGroup mediaGroup = NetworkMediaGroup.from(userId, text, files);
//            imageAttachments.stream().map((a) -> {
//                InputMediaPhoto photo = new InputMediaPhoto();
//                photo.setMedia(attachmentsController.getFile(a), a.getAttachmentId().toString());
//                if (first.getAndSet(false)) {
//                    photo.setCaption(text);
//                }
//                return photo;
//            }).collect(Collectors.toList())
//            AtomicBoolean first = new AtomicBoolean(true);
//            SendMediaGroup mediaGroup = new SendMediaGroup(userId, );
            try {
                List<Message> messages = execute(mediaGroup);
                return messages.get(0).getMessageId().toString();
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        } else {
            try {
                Message message;
                if (imageAttachments.size() == 1) {
//                    new InputFile()
//                    SendPhoto photo = new SendPhoto(userId, new InputFile());
//                    photo.setCaption(text);
                    message = execute(NetworkSendPhoto.from(userId, text, NetworkFile.from(attachmentsController.getFile(imageAttachments.get(0)))));
                } else {
                    message = execute(new SendMessage(userId, text));
                }
                return message.getMessageId().toString();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public void editMessage(String userId, String chatMsgId, String text) throws Exception {
        EditMessageCaption editMessageCaption = EditMessageCaption.builder().chatId(userId).messageId(Integer.valueOf(chatMsgId)).caption(text).build();
        EditMessageText editMessageText = EditMessageText.builder().chatId(userId).messageId(Integer.valueOf(chatMsgId)).text(text).build();
        try {
            execute(editMessageText);
        } catch (Exception e) {
            try {
                execute(editMessageCaption);
            } catch (Exception e1) {
                throw new Exception("Не удалось отредактировать сообщение: " + e.getMessage() + e1.getMessage());
            }
        }
    }

    @Override
    public void deleteMessage(String userId, String chatMsgId) throws Exception {
        DeleteMessage deleteMessage = DeleteMessage.builder().chatId(userId).messageId(Integer.valueOf(chatMsgId)).build();
        execute(deleteMessage);
    }

    private MessageAttachment saveAttachment(PhotoSize photo) {
        GetFile getFile = new GetFile(photo.getFileId());
        String url = execute(getFile);
        return attachmentsController.downloadAndSave(url, AttachmentType.PHOTO);
    }

    private MessageAttachment saveAttachment(Video video) {
        GetFile getFile = new GetFile(video.getFileId());
        String url = execute(getFile);
        return attachmentsController.downloadAndSave(url, AttachmentType.VIDEO, video.getDuration());
    }

    private MessageAttachment saveAttachment(VideoNote video) {
        GetFile getFile = new GetFile(video.getFileId());
        String url = execute(getFile);
        return attachmentsController.downloadAndSave(url, AttachmentType.VIDEO, video.getDuration());
    }

    private MessageAttachment saveAttachment(Audio audio) {
        GetFile getFile = new GetFile(audio.getFileId());
        String url = execute(getFile);
        return attachmentsController.downloadAndSave(url, AttachmentType.AUDIO, audio.getDuration());
    }

    private MessageAttachment saveAttachment(Voice audio) {
        GetFile getFile = new GetFile(audio.getFileId());
        String url = execute(getFile);
        return attachmentsController.downloadAndSave(url, AttachmentType.AUDIO, audio.getDuration());
    }

    public void sendNotification(String text) {
        Configuration config = configurationDispatcher.getLastConfig();
        try {
            if (config != null) {
                if (config.getTlgNotificationChatId() != null) {
                    sendMessage(config.getTlgNotificationChatId(), text, List.of());
                }
            }
        } catch (Exception ignored) {

        }
    }

    private Message execute(SendMessage message) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url("send-message"));
        return restTemplate.exchange(request.body(message), new ParameterizedTypeReference<Message>() {
        }).getBody();
    }

    private List<Message> execute(NetworkMediaGroup message) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url("send-media-group"));
        return restTemplate.exchange(request.body(message), new ParameterizedTypeReference<List<Message>>() {
        }).getBody();
    }

    private Message execute(NetworkSendPhoto message) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url("send-photo"));
        return restTemplate.exchange(request.body(message), new ParameterizedTypeReference<Message>() {
        }).getBody();
    }

    private Message execute(EditMessageText message) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url("edit-message-text"));
        return restTemplate.exchange(request.body(message), new ParameterizedTypeReference<Message>() {
        }).getBody();
    }

    private Message execute(EditMessageCaption message) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url("edit-message-caption"));
        return restTemplate.exchange(request.body(message), new ParameterizedTypeReference<Message>() {
        }).getBody();
    }

    private Message execute(DeleteMessage message) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url("delete-message"));
        return restTemplate.exchange(request.body(message), new ParameterizedTypeReference<Message>() {
        }).getBody();
    }

    private String execute(GetFile getFile) {
        RequestEntity.BodyBuilder request = RequestEntity.post(url("get-file"));
        return restTemplate.exchange(request.body(getFile), String.class).getBody();
    }

    private String url(String... params) {
//        return "http://10.128.227.39:8080/api/public/telegram/" + String.join("/", params);
        return "http://localhost:8080/api/public/telegram/" + String.join("/", params);
    }

    @Getter
    @Setter
    private static class ParsedMessage {
        private StringBuilder text = new StringBuilder();
        private List<MessageAttachment> attachments = new ArrayList<>();

        public void unite(ParsedMessage message) {
            text.append("\r\n").append(message.getText());
            attachments.addAll(message.getAttachments());
        }
    }
}
