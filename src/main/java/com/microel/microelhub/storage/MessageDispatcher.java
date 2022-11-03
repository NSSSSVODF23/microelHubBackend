package com.microel.microelhub.storage;

import com.microel.microelhub.api.ChatWS;
import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Message;
import com.microel.microelhub.storage.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
public class MessageDispatcher {
    private final MessageRepository messageRepository;
    private final ChatDispatcher chatDispatcher;

    private final ChatWS chatWS;

    public MessageDispatcher(MessageRepository messageRepository, ChatDispatcher chatDispatcher, ChatWS chatWS) {
        this.messageRepository = messageRepository;
        this.chatDispatcher = chatDispatcher;
        this.chatWS = chatWS;
    }

    public Message add(String message, String chatMsgId, Chat chat, Boolean isOperatorMsg) {
        return messageRepository.save(Message.builder()
                .text(message)
                .timestamp(Timestamp.from(Instant.now()))
                .edited(false)
                .operatorMsg(isOperatorMsg)
                .operator(chat.getOperator())
                .chatMsgId(chatMsgId)
                .chat(chat)
                .build());
    }

    public Message add(String message, String chatMsgId, Chat chat) {
        return add(message, chatMsgId, chat, false);
    }

    public Message add(String message, String chatMsgId, String userId, String phone, String name, Platform platform) {
        Chat newChat = chatDispatcher.create(userId, phone, name, platform);
        chatWS.sendMessage(ListUpdateWrapper.of(UpdateType.ADD, newChat));
        return add(message, chatMsgId, newChat);
    }

    public Message edit(String message, String chatMsgId, Chat chat) throws Exception {
        Message foundMessage = messageRepository.findTopByChatAndChatMsgId(chat,chatMsgId).orElse(null);
        if(foundMessage == null) throw new Exception("Сообщение для редактирования не найдено");
        foundMessage.setText(message);
        foundMessage.setEdited(true);
        return messageRepository.save(foundMessage);
    }

    public Message delete(String chatMsgId, Chat chat) throws Exception {
        Message foundMessage = messageRepository.findTopByChatAndChatMsgId(chat,chatMsgId).orElse(null);
        if(foundMessage == null) throw new Exception("Сообщение для удаления не найдено");
        messageRepository.delete(foundMessage);
        return foundMessage;
    }

    public Message update(Message message) {
        return messageRepository.save(message);
    }

    public Message findByChatMsgId(String userId, String chatMsgId, Platform platform) {
        return messageRepository.findTopByChat_User_UserIdAndChatMsgIdAndChat_User_PlatformOrderByTimestampDesc(userId, chatMsgId, platform);
    }

    public Page<Message> getMessagesFromChat(String chatId, Long offset, Integer limit) throws Exception {
        try {
            return messageRepository.findByChat_ChatId(UUID.fromString(chatId), new OffsetRequest(offset, limit, Sort.by(Sort.Direction.DESC, "timestamp")));
        } catch (IllegalArgumentException e) {
            throw new Exception("Не верный UUID");
        }
    }

}
