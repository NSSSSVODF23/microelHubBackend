package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.services.internal.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebChatWS extends AbstractWebSocketHandler<ListUpdateWrapper<Message>> {
    @Override
    public void onReceiveMessage(ListUpdateWrapper<Message> object) {

    }

    @Override
    public List<ListUpdateWrapper<Message>> onNewConnection(String connectionToken) {
        return null;
    }

    @Override
    public boolean isAuthorize(String token) {
        return true;
    }
}
