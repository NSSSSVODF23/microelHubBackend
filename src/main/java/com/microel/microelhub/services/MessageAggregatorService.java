package com.microel.microelhub.services;

import com.microel.microelhub.api.ChatMessageWS;
import com.microel.microelhub.api.ChatWS;
import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.common.chat.DeleteMessageHandle;
import com.microel.microelhub.common.chat.EditMessageHandle;
import com.microel.microelhub.common.chat.NewMessageHandle;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.internal.InternalService;
import com.microel.microelhub.services.telegram.TelegramService;
import com.microel.microelhub.services.vk.VkService;
import com.microel.microelhub.storage.ChatDispatcher;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import com.microel.microelhub.storage.MessageDispatcher;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Configuration;
import com.microel.microelhub.storage.entity.Message;
import com.microel.microelhub.storage.entity.MessageAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MessageAggregatorService {
    private final ChatDispatcher chatDispatcher;
    private final MessageDispatcher messageDispatcher;
    private final ConfigurationDispatcher configurationDispatcher;
    private final ChatMessageWS chatMessageWS;
    private final ChatWS chatWS;
    private final InternalService internalService;
    private final VkService vkService;
    private final TelegramService telegramService;

    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    private void autoCloseChatsSchedule() {
        chatDispatcher.getAllActive().forEach(chat -> {
            Instant lastMessageTime = chat.getLastMessage().toInstant();
            Configuration config = configurationDispatcher.getLastConfig();
            if (config.getChatTimeout() == null) config.setChatTimeout(10);
            if (lastMessageTime.plus(config.getChatTimeout(), ChronoUnit.MINUTES).isBefore(Instant.now()) && chat.getOperator() != null) {
                try {
                    chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.REMOVE, chatDispatcher.changeActive(chat.getChatId().toString(), false), "inactive"));
                } catch (Exception e) {
                    log.warn("Не удалось автоматически завершить чат {}", e.getMessage());
                }
            }
        });
    }

    public MessageAggregatorService(
            ChatDispatcher chatDispatcher,
            MessageDispatcher messageDispatcher,
            ConfigurationDispatcher configurationDispatcher,
            ChatMessageWS chatMessageWS,
            ChatWS chatWS,
            InternalService internalService,
            VkService vkService,
            TelegramService telegramService
    ) {
        this.chatDispatcher = chatDispatcher;
        this.messageDispatcher = messageDispatcher;
        this.configurationDispatcher = configurationDispatcher;
        this.chatMessageWS = chatMessageWS;
        this.chatWS = chatWS;
        this.internalService = internalService;
        this.vkService = vkService;
        this.telegramService = telegramService;
    }

    private void sendGreetingMessage(Chat chat, Platform platform) {
        Configuration config = configurationDispatcher.getLastConfig();
        try {
            if (config != null) {
                if (config.getStartWorkingDay().before(Time.valueOf(LocalTime.now())) && config.getEndWorkingDay().after(Time.valueOf(LocalTime.now()))) {
                    sendMessage(chat.getChatId().toString(), config.getGreeting(), platform, true, new ArrayList<>());
                } else {
                    sendMessage(chat.getChatId().toString(), config.getWarning(), platform, true, new ArrayList<>());
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение приветствие {}", e.getMessage());
        }
        telegramService.sendNotification("\uD83D\uDCAC Новый чат \n"
                + chat.getUser().getName() + " из " + platform.getLocalized());
    }

    public void nextMessageFromUser(String userId, String text, String chatMsgId, String name, Platform platform, MessageAttachment... messageAttachment) {
        Chat chat = chatDispatcher.getLastByUserId(userId, platform);
        if (chat != null && chat.getActive()) {
            chatDispatcher.updateDetailedInfo(chat, false);
            chatDispatcher.increaseUnread(chat);
            chatMessageWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.ADD, messageDispatcher.add(text, chatMsgId, chat, messageAttachment)));
            chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, chat, "unread"));
        } else {
            ChatAndMessageTuple tuple = messageDispatcher.add(text, chatMsgId, userId, name, platform, messageAttachment);
            chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.ADD, tuple.getChat()));
            chatMessageWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.ADD, tuple.getMessage()));
            sendGreetingMessage(tuple.getChat(), platform);
        }
    }

    public void editMessageFromUser(String userId, String text, String chatMsgId, Platform platform) {
        Message message = messageDispatcher.findByChatMsgId(userId, chatMsgId, platform);
        if (message != null) {
            message.setEdited(true);
            message.setText(text);
            chatDispatcher.updateDetailedInfo(message.getChat(), false);
            chatMessageWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, messageDispatcher.update(message)));
        }
    }

    private void nextOperatorMessage(NewMessageHandle handle, String chatId, String text, Platform platform, Boolean isGreetingMsg, List<String> imageAttachments) throws Exception {
        Chat chat = chatDispatcher.getLastByChatId(chatId, platform);
        if (chat != null && chat.getActive()) {
            String chatMsgId = handle.apply(chat.getUser().getUserId(), text);
            if (chatMsgId == null) throw new Exception("Не удалось отправить сообщение");
            chatDispatcher.updateDetailedInfo(chat, !isGreetingMsg);
            chatMessageWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.ADD, messageDispatcher.add(text, chatMsgId, chat, true)));
            return;
        }
        throw new Exception("Не найден активный чат");
    }

    private void editOperatorMessage(EditMessageHandle handle, String chatId, String chatMsgId, String text, Platform platform) throws Exception {
        Chat chat = chatDispatcher.getLastByChatId(chatId, platform);
        if (chat != null && chat.getActive()) {
            handle.apply(chat.getUser().getUserId(), chatMsgId, text);
            chatMessageWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, messageDispatcher.edit(text, chatMsgId, chat)));
        } else {
            throw new Exception("Не найден активный чат");
        }
    }

    private void deleteOperatorMessage(DeleteMessageHandle handle, String chatId, String chatMsgId, Platform platform) throws Exception {
        Chat chat = chatDispatcher.getLastByChatId(chatId, platform);
        if (chat != null && chat.getActive()) {
            handle.apply(chat.getUser().getUserId(), chatMsgId);
            chatMessageWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.REMOVE, messageDispatcher.delete(chatMsgId, chat)));
        }
    }

    public void sendMessage(String chatId, String text, Platform platform, Boolean isGreetingMsg, List<String> imageAttachments) throws Exception {
        switch (platform) {
            case WHATSAPP:
                log.warn("Отправка сообщений в WhatsApp не реализована");
                break;
            case VK:
                nextOperatorMessage(vkService::sendMessage, chatId, text, platform, isGreetingMsg, imageAttachments);
                break;
            case TELEGRAM:
                nextOperatorMessage(telegramService::sendMessage, chatId, text, platform, isGreetingMsg, imageAttachments);
                break;
            case INTERNAL:
                nextOperatorMessage(internalService::sendMessage, chatId, text, platform, isGreetingMsg, imageAttachments);
                break;
            default:
                throw new Exception("Платформа не найдена");
        }
    }

    public void editMessage(String chatId, String chatMsgId, String text, Platform platform) throws Exception {
        switch (platform) {
            case WHATSAPP:
                log.warn("Отправка сообщений в WhatsApp не реализована");
                break;
            case VK:
                editOperatorMessage(vkService::editMessage, chatId, chatMsgId, text, platform);
                break;
            case TELEGRAM:
                editOperatorMessage(telegramService::editMessage, chatId, chatMsgId, text, platform);
                break;
            case INTERNAL:
                editOperatorMessage(internalService::editMessage, chatId, chatMsgId, text, platform);
                break;
            default:
                throw new Exception("Платформа не найдена");
        }
    }

    public void deleteMessage(String chatId, String chatMsgId, Platform platform) throws Exception {
        switch (platform) {
            case WHATSAPP:
                log.warn("Отправка сообщений в WhatsApp не реализована");
                break;
            case VK:
                deleteOperatorMessage(vkService::deleteMessage, chatId, chatMsgId, platform);
                break;
            case TELEGRAM:
                deleteOperatorMessage(telegramService::deleteMessage, chatId, chatMsgId, platform);
                break;
            case INTERNAL:
                deleteOperatorMessage(internalService::deleteMessage, chatId, chatMsgId, platform);
                break;
            default:
                throw new Exception("Платформа не найдена");
        }
    }

}
