package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.WebChatOperatorData;
import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.storage.ChatDispatcher;
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
    public List<WebChatOperatorData> onNewConnection(String connectionToken) {
        WebChatOperatorData result = WebChatOperatorData.from(chatDispatcher.getLastByUserId(connectionToken, Platform.INTERNAL).getOperator());
        if(result == null) return null;
        return List.of(result);
    }

    @Override
    public boolean isAuthorize(String token) {
        return true;
    }
}
