package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.WebChatOperatorData;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.storage.ChatDispatcher;
import com.microel.microelhub.storage.entity.Chat;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class WebOperatorWS extends AbstractWebSocketHandler<WebChatOperatorData> {
    private final ChatDispatcher chatDispatcher;

    public WebOperatorWS(ChatDispatcher chatDispatcher) {
        this.chatDispatcher = chatDispatcher;
    }

    @Override
    public void onReceiveMessage(WebChatOperatorData object) {

    }

    @Override
    public List<WebChatOperatorData> onNewConnection(String connectionToken) {return null;}

    @Override
    public boolean isAuthorize(String token) {
        return true;
    }
}
