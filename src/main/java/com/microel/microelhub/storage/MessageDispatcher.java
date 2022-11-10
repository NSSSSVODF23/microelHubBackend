package com.microel.microelhub.storage;

import com.microel.microelhub.api.ChatWS;
import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.ChatAndMessageTuple;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Message;
import com.microel.microelhub.storage.entity.MessageAttachment;
import com.microel.microelhub.storage.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
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

    public Message add(String message, String chatMsgId, Chat chat, Boolean isOperatorMsg, MessageAttachment ...messageAttachment) {
        return messageRepository.save(Message.builder()
                .text(message)
                .timestamp(Timestamp.from(Instant.now()))
                .edited(false)
                .operatorMsg(isOperatorMsg)
                .operator(chat.getOperator())
                .attachments(Set.of(messageAttachment))
                .chatMsgId(chatMsgId)
                .chat(chat)
                .build());
    }

    public Message add(String message, String chatMsgId, Chat chat, MessageAttachment ...messageAttachment) {
        return add(message, chatMsgId, chat, false, messageAttachment);
    }

    public ChatAndMessageTuple add(String message, String chatMsgId, String userId, String name, Platform platform, MessageAttachment ...messageAttachment) {
        Chat newChat = chatDispatcher.create(userId, name, platform);
        return new ChatAndMessageTuple(newChat ,add(message, chatMsgId, newChat, messageAttachment));
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

    public Page<Message> getMessagesFromUser(String userId, Platform platform, Long offset, Integer limit) throws Exception {
        try {
            return messageRepository.findByChat_User_UserIdAndChat_User_Platform(userId, platform, new OffsetRequest(offset, limit, Sort.by(Sort.Direction.DESC, "timestamp")));
        } catch (IllegalArgumentException e) {
            throw new Exception("Не верный UUID");
        }
    }

    public Message getLastMessageFromUser(String userId, Platform platform) {
        return messageRepository.findTopByChat_User_UserIdAndChat_User_Platform(userId,platform,Sort.by(Sort.Direction.DESC,"timestamp")).orElse(null);
    }
}
