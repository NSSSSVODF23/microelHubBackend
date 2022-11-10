package com.microel.microelhub.services.telegram;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.StatedApiService;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import static java.lang.Thread.sleep;

@Slf4j
@Service
public class TelegramBotProvider extends TelegramBotsApi {
    private final StatedApiService statedApiService;
    private BotSession botSession = null;

    public TelegramBotProvider(TelegramService bot, ConfigurationDispatcher configurationDispatcher, StatedApiService statedApiService) throws TelegramApiException {
            super(DefaultBotSession.class);
        this.statedApiService = statedApiService;
        statedApiService.logCreated(Platform.TELEGRAM);
        configurationDispatcher.addChangeConfigurationHandler("telegram", ()->initialization(bot));
        initialization(bot);
    }

    private void initialization(TelegramService bot){
        try{
            if(botSession != null && botSession.isRunning()) botSession.stop();
            bot.updateCredentials();
            botSession = registerBot(bot);
            statedApiService.logStatusChange(Platform.TELEGRAM, "API инициализирован успешно");
        }catch (TelegramApiException e){
            statedApiService.logStatusChange(Platform.TELEGRAM, "API не удалось зарегистрировать, нет доступа к интернету или реквизиты не верны");
//            Executors.newSingleThreadExecutor().execute(()->{
//                try {
//                    sleep(60000);
//                } catch (InterruptedException ignored) {}
//                initialization(bot);
//            });
        } catch (Exception e) {
            statedApiService.logStatusChange(Platform.TELEGRAM, e.getMessage());
//            Executors.newSingleThreadExecutor().execute(()->{
//                try {
//                    sleep(60000);
//                } catch (InterruptedException ignored) {}
//                initialization(bot);
//            });
        }
    }
}
