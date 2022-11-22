package com.microel.microelhub.services.vk;

import com.microel.microelhub.common.AttachmentsSavingController;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.services.MessageSenderWrapper;
import com.microel.microelhub.services.StatedApiService;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import com.microel.microelhub.storage.entity.Configuration;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.video.Video;
import com.vk.api.sdk.objects.video.responses.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

@Slf4j
@Service
public class VkService implements MessageSenderWrapper {
    private final VkApiClient api;
    private GroupActor groupActor;
    private UserActor userActor;
    private VkUpdateHandler pollHandler;
    private final ConfigurationDispatcher configurationDispatcher;
    private final StatedApiService statedApiService;

    public VkService(@Lazy MessageAggregatorService messageAggregatorService, ConfigurationDispatcher configurationDispatcher, StatedApiService statedApiService, AttachmentsSavingController attachmentsSavingController) {
        this.configurationDispatcher = configurationDispatcher;
        this.statedApiService = statedApiService;
        statedApiService.logCreated(Platform.VK);
        TransportClient transportClient = new HttpTransportClient();
        api = new VkApiClient(transportClient);
        configurationDispatcher.addChangeConfigurationHandler("vk", () -> initialization(messageAggregatorService, attachmentsSavingController));
        initialization(messageAggregatorService, attachmentsSavingController);
    }

    private void initialization(MessageAggregatorService messageAggregatorService, AttachmentsSavingController attachmentsSavingController) {
        Configuration configuration = configurationDispatcher.getLastConfig();
        if (pollHandler != null && pollHandler.isRunning()) pollHandler.stop();
        if (configuration == null || configuration.getVkGroupId() == null || configuration.getVkGroupToken() == null) {
            statedApiService.logStatusChange(Platform.VK, "Реквизиты для инициализации API пусты");
            return;
        }

        groupActor = new GroupActor(Integer.parseInt(configuration.getVkGroupId()), configuration.getVkGroupToken());
        userActor = new UserActor(33474398,"vk1.a.EhsqH9D9K1hrD-WZFEEJ4Vj0xiiu9iH0ng1TWVIpPn73WB5lnep0fWqX6x7SIJkI2sSozl9urutsglN2lMwG4YvNxsHpAKGJmyafaBlTo5q9NHwFbwdQLgSokEgrMu5E7z1tCo9m_9G9jeqNNQP3IJVsK-LhsLaVMzkf0-WpcpgbxK5dhLclbE0AoOOgaU--HolTy2Y0Lnlr-wQZseIFug");

        try {
            api.groups().getTokenPermissions(groupActor).execute();
        } catch (ApiException | ClientException e) {
            statedApiService.logStatusChange(Platform.VK, "API не удалось зарегистрировать, нет доступа к интернету или реквизиты не верны");
            return;
        }

        try {
            api.groups().setLongPollSettings(groupActor, Integer.parseInt(configuration.getVkGroupId())).enabled(true).apiVersion("5.95")
                    .messageNew(true).messageDeny(false).messageAllow(false).messageEdit(false)
                    .execute();
        } catch (Exception e) {
            statedApiService.logStatusChange(Platform.VK, "Не удалось сконфигурировать VK API");
            return;
        }

        pollHandler = new VkUpdateHandler(api, groupActor, userActor, 35, messageAggregatorService, attachmentsSavingController);
        pollHandler.run();

        statedApiService.logStatusChange(Platform.VK, "API инициализирован успешно");
    }

    private Integer getRandomId() {
        return Math.round((float) Math.random() * Integer.MAX_VALUE);
    }

    @Override
    public String sendMessage(String userId, String text) {
        try {
            return api.messages().send(groupActor).randomId(getRandomId()).disableMentions(false).peerId(Integer.parseInt(userId)).message(text).execute().toString();
        } catch (ApiException | ClientException | NumberFormatException e) {
            log.warn("Не удалось отправить сообщение.");
        }
        return null;
    }

    @Override
    public void editMessage(String userId, String chatMsgId, String text) throws Exception {
        try {
            api.messages().edit(groupActor, Integer.parseInt(userId)).message(text).messageId(Integer.parseInt(chatMsgId)).execute();
        } catch (ApiException | ClientException | NumberFormatException e) {
            throw new Exception("Не удалось отредактировать сообщение: " + e.getMessage());
        }
    }

    @Override
    public void deleteMessage(String userId, String chatMsgId) throws Exception {
        try {
            api.messages().delete(groupActor).peerId(Integer.parseInt(userId)).messageIds(Integer.parseInt(chatMsgId)).deleteForAll(true).execute();
        } catch (ApiException | ClientException | NumberFormatException e) {
            throw new Exception("Не удалось удалить сообщение: " + e.getMessage());
        }
    }


}
