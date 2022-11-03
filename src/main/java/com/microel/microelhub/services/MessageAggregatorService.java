package com.microel.microelhub.services;

import com.microel.microelhub.api.ChatMessageWS;
import com.microel.microelhub.api.ChatWS;
import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.common.chat.DeleteMessageHandle;
import com.microel.microelhub.common.chat.EditMessageHandle;
import com.microel.microelhub.common.chat.NewMessageHandle;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.telegram.Bot;
import com.microel.microelhub.services.vk.VkService;
import com.microel.microelhub.storage.ChatDispatcher;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import com.microel.microelhub.storage.MessageDispatcher;
import com.microel.microelhub.storage.OperatorDispatcher;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Configuration;
import com.microel.microelhub.storage.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MessageAggregatorService {
    private final ChatDispatcher chatDispatcher;
    private final MessageDispatcher messageDispatcher;
    private final OperatorDispatcher operatorDispatcher;
    private final ConfigurationDispatcher configurationDispatcher;
    private final ChatMessageWS chatMessageWS;
    private final ChatWS chatWS;
    private final VkService vkService;
    private final Bot telegramBot;

    @Scheduled(initialDelay = 1, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    private void autoCloseChatsSchedule() {
        chatDispatcher.getAllActive().forEach(chat -> {
            Instant lastMessageTime = chat.getLastMessage().toInstant();
            if (lastMessageTime.plus(3, ChronoUnit.HOURS).isBefore(Instant.now()) && chat.getOperator() != null) {
                try {
                    chatWS.sendMessage(ListUpdateWrapper.of(UpdateType.REMOVE, chatDispatcher.changeActive(chat.getChatId().toString(), false), "inactive"));
                } catch (Exception e) {
                    log.warn("Не удалось автоматически завершить чат {}", e.getMessage());
                }
            }
        });
    }

    public MessageAggregatorService(ChatDispatcher chatDispatcher, MessageDispatcher messageDispatcher, OperatorDispatcher operatorDispatcher, ConfigurationDispatcher configurationDispatcher, ChatMessageWS chatMessageWS, ChatWS chatWS, VkService vkService, Bot telegramBot) {
        this.chatDispatcher = chatDispatcher;
        this.messageDispatcher = messageDispatcher;
        this.operatorDispatcher = operatorDispatcher;
        this.configurationDispatcher = configurationDispatcher;
        this.chatMessageWS = chatMessageWS;
        this.chatWS = chatWS;
        this.vkService = vkService;
        this.telegramBot = telegramBot;
    }

    private void sendGreetingMessage(String chatId, Platform platform) {
        Configuration config = configurationDispatcher.getLastConfig();
        try {
            if (config.getStartWorkingDay().before(Time.valueOf(LocalTime.now())) && config.getEndWorkingDay().after(Time.valueOf(LocalTime.now()))) {
                sendMessage(chatId, config.getGreeting(), platform);
            } else {
                sendMessage(chatId, config.getWarning(), platform);
            }
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение приветствие {}", e.getMessage());
        }
    }

    public void nextMessageFromUser(String userId, String text, String chatMsgId, String phone, String name, Platform platform) {
        Chat chat = chatDispatcher.getLastByUserId(userId, platform);
        if (chat != null && chat.getActive()) {
            chatDispatcher.updateLastMessageStamp(chat);
            chatMessageWS.sendMessage(ListUpdateWrapper.of(UpdateType.ADD, messageDispatcher.add(text, chatMsgId, chat)));
        } else {
            Message message = messageDispatcher.add(text, chatMsgId, userId, phone, name, platform);
            chatMessageWS.sendMessage(ListUpdateWrapper.of(UpdateType.ADD, message));
            sendGreetingMessage(message.getChat().getChatId().toString(), platform);
        }
    }

    public void editMessageFromUser(String userId, String text, String chatMsgId, Platform platform) {
        Message message = messageDispatcher.findByChatMsgId(userId, chatMsgId, platform);
        if (message != null) {
            message.setEdited(true);
            message.setText(text);
            chatDispatcher.updateLastMessageStamp(message.getChat());
            chatMessageWS.sendMessage(ListUpdateWrapper.of(UpdateType.UPDATE, messageDispatcher.update(message)));
        }
    }

    private void nextOperatorMessage(NewMessageHandle handle, String chatId, String text, Platform platform) throws Exception {
        Chat chat = chatDispatcher.getLastByChatId(chatId, platform);
        if (chat != null && chat.getActive()) {
            String chatMsgId = handle.apply(chat.getUser().getUserId(), text);
            if (chatMsgId == null) throw new Exception("Не удалось отправить сообщение");
            chatDispatcher.updateLastMessageStamp(chat);
            chatMessageWS.sendMessage(ListUpdateWrapper.of(UpdateType.ADD, messageDispatcher.add(text, chatMsgId, chat, true)));
            return;
        }
        throw new Exception("Не найден активный чат");
    }

    private void editOperatorMessage(EditMessageHandle handle, String chatId, String chatMsgId, String text, Platform platform) throws Exception {
        Chat chat = chatDispatcher.getLastByChatId(chatId, platform);
        if (chat != null && chat.getActive()) {
            handle.apply(chat.getUser().getUserId(), chatMsgId, text);
            chatMessageWS.sendMessage(ListUpdateWrapper.of(UpdateType.UPDATE, messageDispatcher.edit(text, chatMsgId, chat)));
        } else {
            throw new Exception("Не найден активный чат");
        }
    }

    private void deleteOperatorMessage(DeleteMessageHandle handle, String chatId, String chatMsgId, Platform platform) throws Exception {
        Chat chat = chatDispatcher.getLastByChatId(chatId, platform);
        if (chat != null && chat.getActive()) {
            handle.apply(chat.getUser().getUserId(), chatMsgId);
            chatMessageWS.sendMessage(ListUpdateWrapper.of(UpdateType.REMOVE, messageDispatcher.delete(chatMsgId, chat)));
        }
    }

    public void sendMessage(String chatId, String text, Platform platform) throws Exception {
        switch (platform) {
            case WHATSAPP:
                log.warn("Отправка сообщений в WhatsApp не реализована");
                break;
            case VK:
                nextOperatorMessage(vkService::sendMessage, chatId, text, platform);
                break;
            case TELEGRAM:
                nextOperatorMessage(telegramBot::sendMessage, chatId, text, platform);
                break;
            case INTERNAL:
                log.info("internal");
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
                editOperatorMessage(telegramBot::editMessage, chatId, chatMsgId, text, platform);
                break;
            case INTERNAL:
                log.info("internal");
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
                deleteOperatorMessage(telegramBot::deleteMessage, chatId, chatMsgId, platform);
                break;
            case INTERNAL:
                log.info("internal");
                break;
            default:
                throw new Exception("Платформа не найдена");
        }
    }
}
