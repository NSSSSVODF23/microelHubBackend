package com.microel.microelhub.api;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.services.telegram.TelegramService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.UUID;

@Controller
@RequestMapping("api/public/telegram")
public class TelegramUpdatesReceiver {
    private final TelegramService telegramService;
    private final MessageAggregatorService messageAggregatorService;

    public TelegramUpdatesReceiver(TelegramService telegramService, MessageAggregatorService messageAggregatorService) {
        this.telegramService = telegramService;
        this.messageAggregatorService  = messageAggregatorService;
    }

    @PostMapping("updates")
    public ResponseEntity<Void> receiveUpdates(@RequestBody Update update){
        telegramService.onUpdateReceived(update);
        return ResponseEntity.ok().build();
    }

    @GetMapping("new-chat/{chatId}")
    public ResponseEntity<UUID> receiveNewChat(@PathVariable String chatId, @RequestParam @Nullable String chatName){
        return ResponseEntity.ok(messageAggregatorService.initializeChat(chatId, Platform.TELEGRAM, chatName));
    }
}
