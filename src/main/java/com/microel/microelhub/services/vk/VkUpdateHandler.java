package com.microel.microelhub.services.vk;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.services.MessageAggregatorService;
import com.vk.api.sdk.callback.longpoll.CallbackApiLongPoll;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.objects.callback.MessageAllow;
import com.vk.api.sdk.objects.callback.MessageDeny;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.users.responses.GetResponse;

import java.util.List;

public class VkUpdateHandler extends CallbackApiLongPoll {

    private final VkApiClient api;
    private final GroupActor actor;

    private final MessageAggregatorService messageAggregatorService;

    public VkUpdateHandler(VkApiClient client, GroupActor actor, int waitTime, MessageAggregatorService messageAggregatorService) {
        super(client, actor, waitTime);
        this.api = client;
        this.actor = actor;
        this.messageAggregatorService = messageAggregatorService;
    }

    @Override
    public void messageNew(Integer groupId, String secret, Message message) {
        messageNew(groupId,message);
    }

    @Override
    public void messageNew(Integer groupId, Message message) {
        try {
            api.messages().markAsRead(actor).peerId(message.getPeerId()).execute();
            List<GetResponse> list = api.users().get(actor).userIds(message.getPeerId().toString()).execute();
            String name = "";
            String phone = null;
            if (!list.isEmpty()) {
                GetResponse response = list.get(0);
                if(response.getFirstName() == null && response.getLastName() == null){
                    name = response.getNickname();
                }
                if(response.getFirstName() != null){
                    name += response.getFirstName();
                }
                if(response.getLastName() != null){
                    name += " " + response.getLastName();
                }
                phone = response.getMobilePhone();
            }
            messageAggregatorService.nextMessageFromUser(message.getPeerId().toString(), message.getText(), message.getId().toString(), phone, name, Platform.VK);
        } catch (Exception ignored) {
        }
    }

    //
//    @Override
//    public void messageReply(Integer groupId, Message message) {
//        System.out.println("reply");
//    }
//

    @Override
    public void messageEdit(Integer groupId, String secret, Message message) {
        messageEdit(groupId,message);
    }

    @Override
    public void messageEdit(Integer groupId, Message message) {
        try {
            messageAggregatorService.editMessageFromUser(message.getPeerId().toString(), message.getText(), message.getId().toString(), Platform.VK);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void messageAllow(Integer groupId, MessageAllow message) {
        System.out.println("allow " + message.toString());
    }

    @Override
    public void messageDeny(Integer groupId, MessageDeny message) {
        System.out.println("deny " + message.toString());
    }

}
