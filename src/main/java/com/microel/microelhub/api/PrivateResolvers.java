package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.*;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.storage.ChatDispatcher;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import com.microel.microelhub.storage.MessageDispatcher;
import com.microel.microelhub.storage.OperatorDispatcher;
import com.microel.microelhub.storage.entity.Message;
import com.microel.microelhub.storage.entity.Operator;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("api/private")
public class PrivateResolvers {
    private final MessageAggregatorService messageAggregatorService;
    private final MessageDispatcher messageDispatcher;
    private final ChatDispatcher chatDispatcher;
    private final ConfigurationDispatcher configurationDispatcher;
    private final OperatorDispatcher operatorDispatcher;
    private final OperatorWS operatorWS;

    private final ChatWS chatWS;

    public PrivateResolvers(MessageAggregatorService messageAggregatorService, MessageDispatcher messageDispatcher, ChatDispatcher chatDispatcher, ConfigurationDispatcher configurationDispatcher, OperatorDispatcher operatorDispatcher, OperatorWS operatorWS, ChatWS chatWS) {
        this.messageAggregatorService = messageAggregatorService;
        this.messageDispatcher = messageDispatcher;
        this.chatDispatcher = chatDispatcher;
        this.configurationDispatcher = configurationDispatcher;
        this.operatorDispatcher = operatorDispatcher;
        this.operatorWS = operatorWS;
        this.chatWS = chatWS;
    }

    @PostMapping("send-message")
    private ResponseEntity<HttpResponse> sendMessage(@RequestBody SendMessageBody body) {
        try {
            messageAggregatorService.sendMessage(body.getChatId(), body.getText(), body.getPlatform());
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PostMapping("edit-message")
    private ResponseEntity<HttpResponse> editMessage(@RequestBody EditMessageBody body) {
        try {
            messageAggregatorService.editMessage(body.getChatId(), body.getChatMsgId(), body.getText(), body.getPlatform());
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PostMapping("delete-message")
    private ResponseEntity<HttpResponse> deleteMessage(@RequestBody DeleteMessageBody body) {
        try {
            messageAggregatorService.deleteMessage(body.getChatId(), body.getChatMsgId(), body.getPlatform());
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PostMapping("messages")
    private ResponseEntity<HttpResponse> getMessages(@RequestBody ChatMessagesRequest request) {
        if (request.getOffset() == null || request.getOffset() < 0) {
            return ResponseEntity.ok(HttpResponse.error("Не верный offset"));
        }
        if (request.getLimit() == null || request.getLimit() < 1) {
            return ResponseEntity.ok(HttpResponse.error("Не верный limit"));
        }
        Page<Message> messages = null;
        try {
            messages = messageDispatcher.getMessagesFromChat(request.getChatId(), request.getOffset(), request.getLimit());
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(messages));
    }

    @PostMapping("chats")
    private ResponseEntity<HttpResponse> getChats(@RequestBody ChatRequest request) {
        if (request.getOffset() == null || request.getOffset() < 0) {
            return ResponseEntity.ok(HttpResponse.error("Не верный offset"));
        }
        if (request.getLimit() == null || request.getLimit() < 1) {
            return ResponseEntity.ok(HttpResponse.error("Не верный limit"));
        }
        try {
            return ResponseEntity.ok(HttpResponse.of(chatDispatcher.getByActive(request.getIsActive(), request.getOffset(), request.getLimit())));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("chat-operator")
    private ResponseEntity<HttpResponse> changeChatOperator(@RequestBody ChangeChatOperatorRequest request) {
        if (request.getChatId() == null || request.getChatId().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор чата"));
        if (request.getLogin() == null || request.getLogin().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Пустой логин оператора"));
        try {
            chatWS.sendMessage(ListUpdateWrapper.of(UpdateType.UPDATE, chatDispatcher.changeOperator(request.getChatId(), request.getLogin()), "operator"));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PatchMapping("chat-inactive")
    private ResponseEntity<HttpResponse> doChatInactive(@RequestBody String chatId) {
        if (chatId == null || chatId.isBlank())
            return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор чата"));
        try {
            chatWS.sendMessage(ListUpdateWrapper.of(UpdateType.REMOVE, chatDispatcher.changeActive(chatId, false), "inactive"));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PostMapping("config")
    private ResponseEntity<HttpResponse> getConfig() {
        return ResponseEntity.ok(HttpResponse.of(configurationDispatcher.getLastConfig()));
    }

    @PatchMapping("config")
    private ResponseEntity<HttpResponse> setConfig(@RequestBody ChangeConfigBody body) {
        configurationDispatcher.update(body);
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PostMapping("operators")
    private ResponseEntity<HttpResponse> getOperators(@RequestBody PageRequest body){
        if(body.getOffset() == null || body.getOffset()<0) return ResponseEntity.ok(HttpResponse.error("Offset не может быть отрицательным"));
        if(body.getLimit() == null || body.getLimit() <1) return ResponseEntity.ok(HttpResponse.error("Limit не может быть меньше единицы"));
        return ResponseEntity.ok(HttpResponse.of(operatorDispatcher.getPage(body)));
    }

    @PostMapping("operator")
    private ResponseEntity<HttpResponse> addOperator(@RequestBody Operator operator){
        try {
            operatorDispatcher.create(operator);
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        operatorWS.sendMessage(ListUpdateWrapper.of(UpdateType.ADD,operator));
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PatchMapping("operator")
    private ResponseEntity<HttpResponse> editOperator(@RequestBody Operator operator){
        try {
            operatorDispatcher.edit(operator);
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        operatorWS.sendMessage(ListUpdateWrapper.of(UpdateType.UPDATE,operator));
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @DeleteMapping("operator/{login}")
    private ResponseEntity<HttpResponse> deleteOperator(@PathVariable String login) {
        try {
            operatorWS.sendMessage(ListUpdateWrapper.of(UpdateType.REMOVE,operatorDispatcher.deleteOperator(login)));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }
}
