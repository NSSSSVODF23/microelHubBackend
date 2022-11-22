package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.*;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.services.internal.InternalService;
import com.microel.microelhub.services.vk.VkService;
import com.microel.microelhub.storage.*;
import com.microel.microelhub.storage.entity.Call;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Message;
import com.microel.microelhub.storage.entity.Operator;
import org.apache.commons.codec.binary.Base64;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.UUID;

@Controller
@RequestMapping("api/private")
public class PrivateResolvers {

    private final MessageAggregatorService messageAggregatorService;
    private final MessageDispatcher messageDispatcher;
    private final ChatDispatcher chatDispatcher;
    private final ConfigurationDispatcher configurationDispatcher;
    private final OperatorDispatcher operatorDispatcher;
    private final UserDispatcher userDispatcher;
    private final CallDispatcher callDispatcher;
    private final OperatorWS operatorWS;
    private final CallWS callWS;
    private final ChatWS chatWS;
    private final InternalService internalService;

    public PrivateResolvers(MessageAggregatorService messageAggregatorService, MessageDispatcher messageDispatcher, ChatDispatcher chatDispatcher, ConfigurationDispatcher configurationDispatcher, OperatorDispatcher operatorDispatcher, UserDispatcher userDispatcher, CallDispatcher callDispatcher, OperatorWS operatorWS, CallWS callWS, ChatWS chatWS, InternalService internalService) {
        this.messageAggregatorService = messageAggregatorService;
        this.messageDispatcher = messageDispatcher;
        this.chatDispatcher = chatDispatcher;
        this.configurationDispatcher = configurationDispatcher;
        this.operatorDispatcher = operatorDispatcher;
        this.userDispatcher = userDispatcher;
        this.callDispatcher = callDispatcher;
        this.operatorWS = operatorWS;
        this.callWS = callWS;
        this.chatWS = chatWS;
        this.internalService = internalService;
    }

    @PostMapping("send-message")
    private ResponseEntity<HttpResponse> sendMessage(@RequestBody SendMessageBody body) {
        try {
            messageAggregatorService.sendMessage(body.getChatId(), body.getText(), body.getPlatform(), false);
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

    @PostMapping("user-messages")
    private ResponseEntity<HttpResponse> getUserMessages(@RequestBody UserMessagesRequest request) {
        if (request.getOffset() == null || request.getOffset() < 0) {
            return ResponseEntity.ok(HttpResponse.error("Не верный offset"));
        }
        if (request.getLimit() == null || request.getLimit() < 1) {
            return ResponseEntity.ok(HttpResponse.error("Не верный limit"));
        }
        Page<Message> messages = null;
        try {
            messages = messageDispatcher.getMessagesFromUser(request.getUserId(), request.getPlatform(), request.getOffset(), request.getLimit());
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(messages));
    }

    @PostMapping("chat-messages")
    private ResponseEntity<HttpResponse> getChatMessages(@RequestBody ChatMessagesRequest request) {
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

    @GetMapping("chat")
    private ResponseEntity<HttpResponse> getChat(@RequestParam @Nullable String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор чата"));
        }
        try {
            return ResponseEntity.ok(HttpResponse.of(chatDispatcher.get(chatId)));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
    }

    @GetMapping("chat-inactive")
    private ResponseEntity<HttpResponse> messages(
            @RequestParam @Nullable String query,
            @RequestParam @Nullable String who,
            @RequestParam @Nullable Timestamp start,
            @RequestParam @Nullable Timestamp end,
            @RequestParam Long offset,
            @RequestParam Integer limit) {
        return ResponseEntity.ok(HttpResponse.of(chatDispatcher.getFiltered(query, who, start, end, offset, limit)));
    }

    @PatchMapping("chat-operator")
    private ResponseEntity<HttpResponse> changeChatOperator(@RequestBody ChangeChatOperatorRequest request) {
        if (request.getChatId() == null || request.getChatId().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор чата"));
        if (request.getLogin() == null || request.getLogin().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Пустой логин оператора"));
        try {
            Chat chat = chatDispatcher.changeOperator(request.getChatId(), request.getLogin());
            chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, chat, "operator"));
            internalService.setOperatorToWebChat(chat.getUser().getUserId(), chat.getOperator());
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
            chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.REMOVE, chatDispatcher.changeActive(chatId, false), "inactive"));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PatchMapping("chat-read")
    private ResponseEntity<HttpResponse> doReadChat(@RequestBody String chatId) {
        if (chatId == null || chatId.isBlank())
            return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор чата"));
        try {
            chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, chatDispatcher.clearUnread(chatId), "read"));
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
    private ResponseEntity<HttpResponse> getOperators(@RequestBody PageRequest body) {
        if (body.getOffset() == null || body.getOffset() < 0)
            return ResponseEntity.ok(HttpResponse.error("Offset не может быть отрицательным"));
        if (body.getLimit() == null || body.getLimit() < 1)
            return ResponseEntity.ok(HttpResponse.error("Limit не может быть меньше единицы"));
        return ResponseEntity.ok(HttpResponse.of(operatorDispatcher.getPage(body)));
    }

    @GetMapping("operator/{login}")
    private ResponseEntity<HttpResponse> getOperator(@PathVariable String login) {
        if (login == null || login.isBlank())
            return ResponseEntity.ok(HttpResponse.error("Логин не может быть пустым"));
        return ResponseEntity.ok(HttpResponse.of(operatorDispatcher.getByLogin(login)));
    }

    @PostMapping("operator/avatar")
    private ResponseEntity<HttpResponse> setOperatorAvatar(@RequestBody UploadOperatorAvatarRequest body) throws IOException {
        if (body.getLogin() == null || body.getLogin().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Логин оператора не может быть пустым"));
        if (body.getImage() == null || body.getImage().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Требуется изображение для сохранения"));

        byte[] photo = Base64.decodeBase64(body.getImage().split(",")[1]);
        if (photo == null || photo.length == 0)
            return ResponseEntity.ok(HttpResponse.error("Пустое декодированное изображение"));

        UUID fileName = UUID.randomUUID();
        Files.createDirectories(Path.of("./attachments", "photos"));
        Files.write(Path.of("./attachments", "photos", fileName + ".jpg"), photo);

        try {
            operatorWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, operatorDispatcher.setAvatar(body.getLogin(), fileName.toString())));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }

        return ResponseEntity.ok(HttpResponse.of(null));
    }



    @PostMapping("operator")
    private ResponseEntity<HttpResponse> addOperator(@RequestBody Operator operator) {
        try {
            operatorDispatcher.create(operator);
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        operatorWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.ADD, operator));
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PatchMapping("operator")
    private ResponseEntity<HttpResponse> editOperator(@RequestBody Operator operator) {
        try {
            operatorDispatcher.edit(operator);
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        operatorWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, operator));
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @DeleteMapping("operator/{login}")
    private ResponseEntity<HttpResponse> deleteOperator(@PathVariable String login) {
        try {
            operatorWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.REMOVE, operatorDispatcher.deleteOperator(login)));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @PostMapping("user-phone")
    private ResponseEntity<HttpResponse> appendPhoneToUser(@RequestBody UserAppendPhoneRequest body) {
        try {
            userDispatcher.appendPhone(body.getUserId(), body.getPlatform(), body.getPhone());

            Chat chat = chatDispatcher.getLastByUserId(body.getUserId(), body.getPlatform());
            if (chat == null || !chat.getActive()) return ResponseEntity.ok(HttpResponse.of(null));

            chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, chat));
            return ResponseEntity.ok(HttpResponse.of(null));

        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
    }

    @PostMapping("user-login")
    private ResponseEntity<HttpResponse> setLoginToUser(@RequestBody UserSetPhoneRequest body) {
        try {
            userDispatcher.setLogin(body.getUserId(), body.getPlatform(), body.getLogin());

            Chat chat = chatDispatcher.getLastByUserId(body.getUserId(), body.getPlatform());
            if (chat == null || !chat.getActive()) return ResponseEntity.ok(HttpResponse.of(null));

            chatWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, chat));
            return ResponseEntity.ok(HttpResponse.of(null));

        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
    }

    @PostMapping("calls")
    private ResponseEntity<HttpResponse> getCalls(@RequestBody PageRequest body) {
        if (body.getOffset() == null || body.getOffset() < 0)
            return ResponseEntity.ok(HttpResponse.error("Offset не может быть отрицательным"));
        if (body.getLimit() == null || body.getLimit() < 1)
            return ResponseEntity.ok(HttpResponse.error("Limit не может быть меньше единицы"));
        return ResponseEntity.ok(HttpResponse.of(callDispatcher.getPage(body)));
    }

    @PostMapping("unprocessed-calls")
    private ResponseEntity<HttpResponse> getUnprocessedCalls() {
        return ResponseEntity.ok(HttpResponse.of(callDispatcher.getUnprocessed()));
    }

    @PostMapping("processing-calls")
    private ResponseEntity<HttpResponse> setProcessingCalls(@RequestBody ProcessingCallsRequest body) {
        if (body.getCallIds() == null)
            return ResponseEntity.ok(HttpResponse.error("Пустой массив идентификаторов обратных вызовов"));
        if (body.getOperatorLogin() == null || body.getOperatorLogin().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Пустой логин оператора"));
        final Operator foundOperator = operatorDispatcher.getByLogin(body.getOperatorLogin());
        if (foundOperator == null) return ResponseEntity.ok(HttpResponse.error("Нет оператора с таким логином"));
        body.getCallIds().forEach(id -> {
            final Call foundCall = callDispatcher.getById(id);
            if (foundCall == null) return;
            foundCall.setProcessed(foundOperator);
            callWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.UPDATE, callDispatcher.save(foundCall), "processing"));
        });
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @GetMapping("chats/statistic/grouped/time/day")
    private ResponseEntity<HttpResponse> getChatsStatisticGroupedByDay(@RequestParam @Nullable Platform platform, @RequestParam @Nullable String login, @RequestParam @Nullable String start, @RequestParam @Nullable String end) {
        return ResponseEntity.ok(HttpResponse.of(chatDispatcher.getStatisticGroupedByDay(platform, login, start, end)));
    }

    @GetMapping("chats/statistic/grouped/{category}/{group}")
    private ResponseEntity<HttpResponse> getChatsStatisticGroupedByCategory(@RequestParam @Nullable String start, @RequestParam @Nullable String end, @PathVariable String category, @PathVariable String group) {
        switch (group) {
            case "source":
                return ResponseEntity.ok(HttpResponse.of(chatDispatcher.getStatisticGroupedBySource(category, start, end)));
            case "operator":
                return ResponseEntity.ok(HttpResponse.of(chatDispatcher.getStatisticGroupedByOperator(category, start, end)));
            default:
                return ResponseEntity.ok(HttpResponse.error("Не найден метод группировки статистики"));
        }
    }

    @GetMapping("chats/statistic/ungrouped")
    private ResponseEntity<HttpResponse> getChatStatisticUngrouped(@RequestParam @Nullable Platform platform, @RequestParam @Nullable String login, @RequestParam @Nullable String start, @RequestParam @Nullable String end) {
        return ResponseEntity.ok(HttpResponse.of(chatDispatcher.getStatisticUngrouped(platform, login, start, end)));
    }
}
