package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.*;
import com.microel.microelhub.common.UpdateType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.security.AuthenticationManager;
import com.microel.microelhub.services.internal.InternalService;
import com.microel.microelhub.services.internal.Message;
import com.microel.microelhub.services.telegram.TelegramService;
import com.microel.microelhub.storage.*;
import com.microel.microelhub.storage.entity.Call;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("api/public")
public class PublicResolvers {

    private final AuthenticationManager authenticationManager;
    private final InternalService internalService;
    private final ChatDispatcher chatDispatcher;
    private final CallDispatcher callDispatcher;
    private final MessageDispatcher messageDispatcher;
    private final CallWS callWS;
    private final ConfigurationDispatcher configurationDispatcher;
    private final TelegramService telegramService;

    public PublicResolvers(AuthenticationManager authenticationManager, InternalService internalService, ChatDispatcher chatDispatcher, CallDispatcher callDispatcher, MessageDispatcher messageDispatcher, CallWS callWS, ConfigurationDispatcher configurationDispatcher, TelegramService telegramService) {
        this.authenticationManager = authenticationManager;
        this.internalService = internalService;
        this.chatDispatcher = chatDispatcher;
        this.callDispatcher = callDispatcher;
        this.messageDispatcher = messageDispatcher;
        this.callWS = callWS;
        this.configurationDispatcher = configurationDispatcher;
        this.telegramService = telegramService;
    }

    @PostMapping("login")
    private ResponseEntity<HttpResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(HttpResponse.of(authenticationManager.doLogin(request)));
        } catch (Exception e) {
            return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
        }
    }

    @PostMapping("refresh-token")
    private ResponseEntity<HttpResponse> refreshToken(@RequestBody String token) {
        try {
            return ResponseEntity.ok(HttpResponse.of(authenticationManager.doRefresh(authenticationManager.validateRefreshToken(token))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("photo/{id}")
    private ResponseEntity<byte[]> getPhoto(@PathVariable String id) {
        try {
            byte[] image = Files.readAllBytes(Path.of("./attachments", "photos", id + ".jpg"));
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).contentLength(image.length).body(image);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("video/{id}")
    private ResponseEntity<byte[]> getVideo(@PathVariable String id) {
        try {
            byte[] video = Files.readAllBytes(Path.of("./attachments", "videos", id + ".mp4"));
            return ResponseEntity.ok().contentType(MediaType.valueOf("video/mp4")).contentLength(video.length).body(video);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("chat/message")
    private ResponseEntity<HttpResponse> getMessage(@RequestBody Message body) {
        if (body.getUserId() == null)
            return ResponseEntity.ok(HttpResponse.error("Идентификатор пользователя не может быть пустым"));
        if (body.getMessage() == null || body.getMessage().isBlank())
            return ResponseEntity.ok(HttpResponse.error("Сообщение не может быть пустым"));
        internalService.onMessageReceived(body);
        return ResponseEntity.ok(HttpResponse.of(null));
    }

    @GetMapping("chat/messages/{id}")
    private ResponseEntity<HttpResponse> getMessagesFromChat(@PathVariable String id, @RequestParam Long offset, @RequestParam Integer limit) {
        if (id == null) return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор"));
        if (offset == null || offset < 0L) offset = 0L;
        if (limit == null || limit < 1) limit = 1;
        Chat chat = chatDispatcher.getLastByUserId(id, Platform.INTERNAL);
        if (chat != null && chat.getActive()) {
            try {
                return ResponseEntity.ok(HttpResponse.of(WebMessagesPage.of(messageDispatcher.getMessagesFromChat(chat.getChatId().toString(), offset, limit))));
            } catch (Exception e) {
                return ResponseEntity.ok(HttpResponse.error(e.getMessage()));
            }
        } else {
            return ResponseEntity.ok(HttpResponse.of(new WebMessagesPage()));
        }
    }

    @GetMapping("chat/operator/{id}")
    private ResponseEntity<HttpResponse> getChatOperator(@PathVariable String id){
        if (id == null || id.isBlank()) return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор"));
        Chat chat = chatDispatcher.getLastByUserId(id, Platform.INTERNAL);
        if(chat == null || !chat.getActive()) return ResponseEntity.ok(HttpResponse.of(null));
        return ResponseEntity.ok(HttpResponse.of(WebChatOperatorData.from(chat.getOperator())));
    }

    @GetMapping("chat/active/{id}")
    private ResponseEntity<HttpResponse> getIsActiveChat(@PathVariable String id) {
        if (id == null) return ResponseEntity.ok(HttpResponse.error("Пустой идентификатор"));
        Chat chat = chatDispatcher.getLastByUserId(id, Platform.INTERNAL);
        if (chat != null && chat.getActive()) {
            return ResponseEntity.ok(HttpResponse.of(true));
        } else {
            return ResponseEntity.ok(HttpResponse.of(false));
        }
    }

    @GetMapping("chat/config")
    private ResponseEntity<HttpResponse> getIsWorking() {
        Configuration config = configurationDispatcher.getLastConfig();
        if (config.getStartWorkingDay() == null || config.getEndWorkingDay() == null)
            return ResponseEntity.ok(HttpResponse.of(new WebChatConfigResponse(true, config.getWarning(), null, null)));
        return ResponseEntity.ok(HttpResponse.of(
                new WebChatConfigResponse(
                        (config.getStartWorkingDay().before(Time.valueOf(LocalTime.now())) && config.getEndWorkingDay().after(Time.valueOf(LocalTime.now()))),
                        config.getWarning(),
                        "https://vk.com/im?sel=-" + config.getVkGroupId(),
                        "https://telegram.me/" + config.getTlgBotUsername()
                )
        ));
    }

    @PostMapping("call")
    private ResponseEntity<HttpResponse> createCall(@RequestBody String body) {
        if (body == null || body.isBlank()) return ResponseEntity.ok(HttpResponse.error("Пустой номер телефона"));
        Call call = callDispatcher.getLastByPhone(body);
        if (call != null && call.getCreated().toInstant().plus(15, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return ResponseEntity.ok(HttpResponse.error("Слишком много запросов, попробуйте позже."));
        }
        callWS.sendBroadcast(ListUpdateWrapper.of(UpdateType.ADD, callDispatcher.create(body)));
        telegramService.sendNotification("\uD83D\uDCDE Новая заявка на обратный звонок");
        return ResponseEntity.ok(HttpResponse.of(null));
    }
}
