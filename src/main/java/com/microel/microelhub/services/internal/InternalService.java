package com.microel.microelhub.services.internal;

import com.microel.microelhub.api.WebChatWS;
import com.microel.microelhub.api.WebOperatorWS;
import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.api.transport.WebChatOperatorData;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.services.MessageSenderWrapper;
import com.microel.microelhub.storage.MessageDispatcher;
import com.microel.microelhub.storage.entity.Operator;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InternalService implements MessageSenderWrapper {

    private final MessageAggregatorService messageAggregatorService;
    private final MessageDispatcher messageDispatcher;
    private final WebChatWS webChatWS;
    private final WebOperatorWS webOperatorWS;

    public InternalService(@Lazy MessageAggregatorService messageAggregatorService, MessageDispatcher messageDispatcher, WebChatWS webChatWS, WebOperatorWS webOperatorWS) {
        this.messageAggregatorService = messageAggregatorService;
        this.messageDispatcher = messageDispatcher;
        this.webChatWS = webChatWS;
        this.webOperatorWS = webOperatorWS;
    }

    private String getNextChatMessageId(String userId) {
        int chatMsgId = 0;
        com.microel.microelhub.storage.entity.Message lastMessage = messageDispatcher.getLastMessageFromUser(userId, Platform.INTERNAL);
        if (lastMessage != null) chatMsgId = Integer.parseInt(lastMessage.getChatMsgId()) + 1;
        return String.valueOf(chatMsgId);
    }

    public void onMessageReceived(Message message) {
        String nextMessageId = getNextChatMessageId(message.getUserId().toString());
        messageAggregatorService.nextMessageFromUser(message.getUserId().toString(), message.getMessage(), nextMessageId, message.getUserId().toString(), Platform.INTERNAL);
        try {
            webChatWS.sendUnicast(message.getUserId().toString(),ListUpdateWrapper.of(UpdateType.ADD, new Message(nextMessageId, message.getUserId(),message.getMessage(), false, false)));
        } catch (Exception ignored) {
        }
    }

    public void setOperatorToWebChat(String userId, Operator operator) {
        try {
            webOperatorWS.sendUnicast(userId, WebChatOperatorData.from(operator));
        } catch (Exception ignored) {
        }
    }

    @Override
    public String sendMessage(String userId, String text) {
        try {
            String messageId = getNextChatMessageId(userId);
            webChatWS.sendUnicast(userId, ListUpdateWrapper.of(UpdateType.ADD, new Message(messageId, UUID.fromString(userId), text, true, false)));
            return messageId;
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void editMessage(String userId, String chatMsgId, String text) throws Exception {
        webChatWS.sendUnicast(userId, ListUpdateWrapper.of(UpdateType.UPDATE, new Message(chatMsgId, UUID.fromString(userId), text, true, false)));
    }

    @Override
    public void deleteMessage(String userId, String chatMsgId) throws Exception {
        webChatWS.sendUnicast(userId, ListUpdateWrapper.of(UpdateType.REMOVE, new Message(chatMsgId, UUID.fromString(userId), null, true, false)));
    }
}
