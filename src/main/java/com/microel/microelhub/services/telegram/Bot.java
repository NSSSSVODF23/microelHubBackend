package com.microel.microelhub.services.telegram;

import com.microel.microelhub.common.AttachmentsSavingController;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class Bot extends TelegramLongPollingBot implements MessageSenderWrapper {

    private final MessageAggregatorService messageAggregatorService;
    private final ConfigurationDispatcher configurationDispatcher;
    private final AttachmentsSavingController attachmentsSavingController;
    private Configuration config;

    public Bot(@Lazy MessageAggregatorService messageAggregatorService, ConfigurationDispatcher configurationDispatcher, AttachmentsSavingController attachmentsSavingController) {
        this.messageAggregatorService = messageAggregatorService;
        this.configurationDispatcher = configurationDispatcher;
        this.attachmentsSavingController = attachmentsSavingController;
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

            if (message.hasPhoto()) {
                MessageAttachment messageAttachment = this.savePhoto(message.getPhoto().get(message.getPhoto().size() - 1));
                if (messageAttachment != null)
                    messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getCaption(), message.getMessageId().toString(), null, fullName, Platform.TELEGRAM, messageAttachment);
            } else {
                messageAggregatorService.nextMessageFromUser(message.getChatId().toString(), message.getText(), message.getMessageId().toString(), null, fullName, Platform.TELEGRAM);
            }
        } else if (update.hasEditedMessage()) {
            Message editedMessage = update.getEditedMessage();
            messageAggregatorService.editMessageFromUser(editedMessage.getChatId().toString(), editedMessage.getText(), editedMessage.getMessageId().toString(), Platform.TELEGRAM);
        }
    }

    @Override
    public String sendMessage(String userId, String text) {
        SendMessage message = new SendMessage(userId, text);
        try {
            Message sentMessage = execute(message);
            return sentMessage.getMessageId().toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void editMessage(String userId, String chatMsgId, String text) throws Exception {
        EditMessageText editMessageText = EditMessageText.builder().chatId(userId).messageId(Integer.valueOf(chatMsgId)).text(text).build();
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new Exception("Не удалось отредактировать сообщение: " + e.getMessage());
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

    private MessageAttachment savePhoto(PhotoSize photo) {
        GetFile getFile = new GetFile(photo.getFileId());
        try {
            File file = execute(getFile);
            return attachmentsSavingController.downloadAndSave(file.getFileUrl(getBotToken()), AttachmentType.PHOTO);
        } catch (TelegramApiException e) {
            log.warn("Не удалось получить ссылку на фото от Telegram API");
        }
        return null;
    }
}
