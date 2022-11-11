package com.microel.microelhub.storage;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.telegram.TelegramService;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Configuration;
import com.microel.microelhub.storage.entity.Operator;
import com.microel.microelhub.storage.repository.ChatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ChatDispatcher {
    private final ChatRepository chatRepository;
    private final UserDispatcher userDispatcher;
    private final ConfigurationDispatcher configurationDispatcher;
    private final TelegramService telegramService;
    private final OperatorDispatcher operatorDispatcher;

    public ChatDispatcher(ChatRepository chatRepository, UserDispatcher userDispatcher, ConfigurationDispatcher configurationDispatcher, TelegramService telegramService, OperatorDispatcher operatorDispatcher) {
        this.chatRepository = chatRepository;
        this.userDispatcher = userDispatcher;
        this.configurationDispatcher = configurationDispatcher;
        this.telegramService = telegramService;
        this.operatorDispatcher = operatorDispatcher;
    }

    public Chat getLastByUserId(String userId, Platform platform) {
        return chatRepository.findTopByUser_UserIdAndUser_PlatformOrderByCreatedDesc(userId, platform);
    }

    public Chat getLastByChatId(String chatId, Platform platform) {
        UUID uuid = UUID.fromString(chatId);
        return chatRepository.findTopByChatIdAndUser_PlatformOrderByCreatedDesc(uuid, platform);
    }

    public Chat create(String userId, String name, Platform platform) {
        return chatRepository.save(Chat.builder()
                .chatId(UUID.randomUUID())
                .created(Timestamp.from(Instant.now()))
                .lastMessage(Timestamp.from(Instant.now()))
                .active(true)
                .user(userDispatcher.upsert(userId, name, platform))
                .build());
    }

    public Chat changeOperator(String chatId, String login) throws Exception {
        Chat chat = null;
        try {
            chat = chatRepository.findById(UUID.fromString(chatId)).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new Exception("Не правильный UUID чата");
        }
        if (chat == null) throw new Exception("Не найден чат");
        Operator operator = operatorDispatcher.getByLogin(login);
        if (operator == null) throw new Exception("Не найден оператор по логину");
        Configuration config = configurationDispatcher.getLastConfig();
        if (config != null) {
            if (config.getTlgNotificationChatId() != null) {
                telegramService.sendMessage(config.getTlgNotificationChatId(), "⌨ Чат взят в работу\n"
                        + chat.getUser().getPlatform().getLocalized() + " " + chat.getUser().getName() + "\nОператор: " + operator.getName());
            }
        }
        chat.setOperator(operator);
        chat.setLastMessage(Timestamp.from(Instant.now()));
        return chatRepository.save(chat);
    }

    public Chat changeActive(String chatId, Boolean active) throws Exception {
        Chat chat = null;
        try {
            chat = chatRepository.findById(UUID.fromString(chatId)).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new Exception("Не правильный UUID чата");
        }
        if (chat == null) throw new Exception("Не найден чат");
        chat.setActive(active);
        return chatRepository.save(chat);
    }

    public void updateLastMessageStamp(Chat chat) {
        chat.setLastMessage(Timestamp.from(Instant.now()));
        chatRepository.save(chat);
    }

    public Page<Chat> getByActive(Boolean isActive, Long offset, Integer limit) {
        return chatRepository.findByActive(isActive, new OffsetRequest(offset, limit, Sort.by(Sort.Direction.DESC, "created")));
    }

    public List<Chat> getAllActive() {
        return chatRepository.findAllByActiveOrderByLastMessageDesc(true);
    }

    public void unreadIncrease(Chat chat) {
        if (chat.getUnreadCount() == null) {
            chat.setUnreadCount(1);
        } else {
            chat.setUnreadCount(chat.getUnreadCount() + 1);
        }
        chatRepository.save(chat);
    }

    public Chat clearUnread(String chatId) throws Exception {
        Chat chat = null;
        try {
            chat = chatRepository.findById(UUID.fromString(chatId)).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new Exception("Не правильный UUID чата");
        }
        if (chat == null) throw new Exception("Не найден чат");
        chat.setUnreadCount(0);
        return chatRepository.save(chat);
    }
}
