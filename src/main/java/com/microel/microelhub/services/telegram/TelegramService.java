package com.microel.microelhub.services.telegram;

import com.microel.microelhub.common.AttachmentsController;
import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.services.MessageSenderWrapper;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import com.microel.microelhub.storage.entity.Configuration;
import com.microel.microelhub.storage.entity.MessageAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TelegramService extends TelegramLongPollingBot implements MessageSenderWrapper {

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

    @Override
    public String getBotUsername() {
        return config.getTlgBotUsername();
    }

    @Override
    public String getBotToken() {
        return config.getTlgBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();

            User user = message.getFrom();
            if (message.isCommand() && message.getText().equals("/start")) {
                SendMessage sendMessage = new SendMessage(String.valueOf(message.getChatId()), "Вы можете написать сообщение этому боту, и в ближайшее время Вам ответит наш менеджер.");
                try {
                    execute(sendMessage);
                } catch (TelegramApiException ignored) {
                }
                return;
            }
            String fullName = user.getFirstName();
            if (user.getLastName() != null) {
                fullName += " " + user.getLastName();
            }

            if (message.hasVideo()) {
                MessageAttachment messageAttachment = this.saveAttachment(message.getVideo());
                if (messageAttachment != null)
                    messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getCaption(), message.getMessageId().toString(), fullName, Platform.TELEGRAM, messageAttachment);
            } else if (message.hasVideoNote()) {
                MessageAttachment messageAttachment = this.saveAttachment(message.getVideoNote());
                if (messageAttachment != null)
                    messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getCaption(), message.getMessageId().toString(), fullName, Platform.TELEGRAM, messageAttachment);
            } else if (message.hasAudio()) {
                MessageAttachment messageAttachment = this.saveAttachment(message.getAudio());
                if (messageAttachment != null)
                    messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getCaption(), message.getMessageId().toString(), fullName, Platform.TELEGRAM, messageAttachment);
            } else if (message.hasVoice()) {
                MessageAttachment messageAttachment = this.saveAttachment(message.getVoice());
                if (messageAttachment != null)
                    messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getCaption(), message.getMessageId().toString(), fullName, Platform.TELEGRAM, messageAttachment);
            } else if (message.hasPhoto()) {
                MessageAttachment messageAttachment = this.saveAttachment(message.getPhoto().get(message.getPhoto().size() - 1));
                if (messageAttachment != null)
                    messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getCaption(), message.getMessageId().toString(), fullName, Platform.TELEGRAM, messageAttachment);
            } else {
                messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getText(), message.getMessageId().toString(), fullName, Platform.TELEGRAM);
            }

        } else if (update.hasEditedMessage()) {
            Message editedMessage = update.getEditedMessage();
            messageAggregatorService.editMessageFromUser(editedMessage.getChatId().toString(), editedMessage.getText(), editedMessage.getMessageId().toString(), Platform.TELEGRAM);
        }
    }

    @Override
    public String sendMessage(String userId, String text, List<MessageAttachment> imageAttachments) {
        if (imageAttachments.size() > 1) {
            AtomicBoolean first = new AtomicBoolean(true);
            SendMediaGroup mediaGroup = new SendMediaGroup(userId, imageAttachments.stream().map((a) -> {
                InputMediaPhoto photo = new InputMediaPhoto();
                photo.setMedia(attachmentsController.getFile(a), a.getAttachmentId().toString());
                if (first.getAndSet(false)) {
                    photo.setCaption(text);
                }
                return photo;
            }).collect(Collectors.toList()));
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
                    SendPhoto photo = new SendPhoto(userId, new InputFile(attachmentsController.getFile(imageAttachments.get(0))));
                    photo.setCaption(text);
                    message = execute(photo);
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
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new Exception("Не удалось отредактировать сообщение: " + e.getMessage());
        }
    }

    private MessageAttachment saveAttachment(PhotoSize photo) {
        GetFile getFile = new GetFile(photo.getFileId());
        try {
            File file = execute(getFile);
            return attachmentsController.downloadAndSave(file.getFileUrl(getBotToken()), AttachmentType.PHOTO);
        } catch (TelegramApiException e) {
            log.warn("Не удалось получить ссылку на фото от Telegram API");
        }
        return null;
    }

    private MessageAttachment saveAttachment(Video video) {
        GetFile getFile = new GetFile(video.getFileId());
        try {
            File file = execute(getFile);
            return attachmentsController.downloadAndSave(file.getFileUrl(getBotToken()), AttachmentType.VIDEO, video.getDuration());
        } catch (TelegramApiException e) {
            log.warn("Не удалось получить ссылку на видео от Telegram API");
        }
        return null;
    }

    private MessageAttachment saveAttachment(VideoNote video) {
        GetFile getFile = new GetFile(video.getFileId());
        try {
            File file = execute(getFile);
            return attachmentsController.downloadAndSave(file.getFileUrl(getBotToken()), AttachmentType.VIDEO, video.getDuration());
        } catch (TelegramApiException e) {
            log.warn("Не удалось получить ссылку на видео от Telegram API");
        }
        return null;
    }

    private MessageAttachment saveAttachment(Audio audio) {
        GetFile getFile = new GetFile(audio.getFileId());
        try {
            File file = execute(getFile);
            return attachmentsController.downloadAndSave(file.getFileUrl(getBotToken()), AttachmentType.AUDIO, audio.getDuration());
        } catch (TelegramApiException e) {
            log.warn("Не удалось получить ссылку на аудио от Telegram API");
        }
        return null;
    }

    private MessageAttachment saveAttachment(Voice audio) {
        GetFile getFile = new GetFile(audio.getFileId());
        try {
            File file = execute(getFile);
            return attachmentsController.downloadAndSave(file.getFileUrl(getBotToken()), AttachmentType.AUDIO, audio.getDuration());
        } catch (TelegramApiException e) {
            log.warn("Не удалось получить ссылку на аудио от Telegram API");
        }
        return null;
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
}
