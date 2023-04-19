package com.microel.microelhub.services.vk;

import com.microel.microelhub.common.AttachmentsController;
import com.microel.microelhub.common.chat.AttachmentType;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.microel.microelhub.services.MessageSenderWrapper;
import com.microel.microelhub.services.StatedApiService;
import com.microel.microelhub.storage.ConfigurationDispatcher;
import com.microel.microelhub.storage.entity.Configuration;
import com.microel.microelhub.storage.entity.MessageAttachment;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.photos.responses.GetMessagesUploadServerResponse;
import com.vk.api.sdk.objects.photos.responses.MessageUploadResponse;
import com.vk.api.sdk.objects.photos.responses.SaveMessagesPhotoResponse;
import com.vk.api.sdk.objects.video.VideoFull;
import com.vk.api.sdk.objects.video.responses.GetResponse;
import com.vk.api.sdk.queries.messages.MessagesSendQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

@Slf4j
@Service
public class VkService implements MessageSenderWrapper {
    private final VkApiClient api;
    private final ConfigurationDispatcher configurationDispatcher;
    private final StatedApiService statedApiService;
    private final AttachmentsController attachmentsController;
    private GroupActor groupActor;
    private UserActor userActor;
    private VkUpdateHandler pollHandler;

    public VkService(@Lazy MessageAggregatorService messageAggregatorService, ConfigurationDispatcher configurationDispatcher, StatedApiService statedApiService, AttachmentsController attachmentsController) {
        this.configurationDispatcher = configurationDispatcher;
        this.statedApiService = statedApiService;
        this.attachmentsController = attachmentsController;
        statedApiService.logCreated(Platform.VK);
        TransportClient transportClient = new HttpTransportClient();
        api = new VkApiClient(transportClient);
        configurationDispatcher.addChangeConfigurationHandler("vk", () -> initialization(messageAggregatorService, attachmentsController));
        initialization(messageAggregatorService, attachmentsController);
    }

    private void initialization(MessageAggregatorService messageAggregatorService, AttachmentsController attachmentsController) {
        Configuration configuration = configurationDispatcher.getLastConfig();
        if (pollHandler != null && pollHandler.isRunning()) pollHandler.stop();
        if (configuration == null || configuration.getVkGroupId() == null || configuration.getVkGroupToken() == null) {
            statedApiService.logStatusChange(Platform.VK, "Реквизиты для инициализации API пусты");
            return;
        }

        try {
            userActor = new UserActor(Integer.parseInt(configuration.getVkUserId()), configuration.getVkUserToken());
        } catch (Exception e) {
            log.warn("Не удалось инициализировать VK UserActor");
        }
        try {
            groupActor = new GroupActor(Integer.parseInt(configuration.getVkGroupId()), configuration.getVkGroupToken());
        } catch (Exception e) {
            log.warn("Не удалось инициализировать VK GroupActor");
        }

        try {
            api.groups().getTokenPermissions(groupActor).execute();
        } catch (ApiException | ClientException e) {
            statedApiService.logStatusChange(Platform.VK, "API не удалось зарегистрировать, нет доступа к интернету или реквизиты не верны");
            Executors.newSingleThreadExecutor().execute(
                    () -> {
                        try {
                            sleep(60000);
                            statedApiService.logStatusChange(Platform.VK, "Повторная инициализация API");
                            initialization(messageAggregatorService, attachmentsController);
                        } catch (Exception ignored) {
                        }
                    }
            );
            return;
        }

        try {
            api.groups().setLongPollSettings(groupActor, Integer.parseInt(configuration.getVkGroupId())).enabled(true).apiVersion("5.102")
                    .messageNew(true).messageDeny(false).messageAllow(false).messageEdit(false)
                    .execute();
        } catch (Exception e) {
            statedApiService.logStatusChange(Platform.VK, "Не удалось сконфигурировать VK API");
            return;
        }

        try {

        } catch (NullPointerException e) {

        }
        pollHandler = new VkUpdateHandler(api, groupActor, userActor, 35, messageAggregatorService, attachmentsController);
        pollHandler.run();

        statedApiService.logStatusChange(Platform.VK, "API инициализирован успешно");
    }

    private Integer getRandomId() {
        return Math.round((float) Math.random() * Integer.MAX_VALUE);
    }

    @Override
    public String sendMessage(String userId, String text, List<MessageAttachment> imageAttachments) {
        try {
            List<String> attachmentsIds = new ArrayList<>();
            if (imageAttachments.size() > 0) {
                for (MessageAttachment imageAttach : imageAttachments) {
                    if (imageAttach.getAttachmentType() != AttachmentType.PHOTO) continue;
                    GetMessagesUploadServerResponse uploadServerResponse = api.photos().getMessagesUploadServer(groupActor).execute();
                    MessageUploadResponse messageUploadResponse = api.upload()
                            .photoMessage(uploadServerResponse.getUploadUrl().toString(), attachmentsController.getFile(imageAttach.getAttachmentId().toString(), AttachmentType.PHOTO))
                            .execute();
                    List<SaveMessagesPhotoResponse> messagesPhotoResponses = api.photos().saveMessagesPhoto(groupActor, messageUploadResponse.getPhoto())
                            .server(messageUploadResponse.getServer()).hash(messageUploadResponse.getHash()).execute();
                    SaveMessagesPhotoResponse savedToVkPhoto = messagesPhotoResponses.get(0);
                    if (savedToVkPhoto == null)
                        throw new Exception("Получен пустой ответ на запрос сохранения изображения.");
                    attachmentsIds.add("photo" + savedToVkPhoto.getOwnerId() + "_" + savedToVkPhoto.getId() + "_" + savedToVkPhoto.getAccessKey());
                }
            }
            MessagesSendQuery messagesSendQuery = api.messages().send(groupActor).randomId(getRandomId()).disableMentions(false).peerId(Integer.parseInt(userId)).message(text);
            if (attachmentsIds.size() > 0) messagesSendQuery.attachment(String.join(",", attachmentsIds));
            return messagesSendQuery.execute().toString();
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение. {}", e.getMessage());
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
    }//fixme Удаляет фото при редактировании

    @Override
    public void deleteMessage(String userId, String chatMsgId) throws Exception {
        try {
            api.messages().delete(groupActor).peerId(Integer.parseInt(userId)).messageIds(Integer.parseInt(chatMsgId)).deleteForAll(true).execute();
        } catch (ApiException | ClientException | NumberFormatException e) {
            throw new Exception("Не удалось удалить сообщение: " + e.getMessage());
        }
    }


    public String getVideoLink(String identifier) {
        try {
            GetResponse response = api.videos().get(userActor).videos(identifier).execute();
            VideoFull videoFull = response.getItems().get(0);
            if (videoFull == null) return null;
            return URLDecoder.decode(response.getItems().get(0).getPlayer().toString(), StandardCharsets.UTF_8);
        } catch (ApiException | ClientException e) {
            log.info("Ошибка получения видеозаписи {}", e.getMessage());
        }
        return null;
    }
}
