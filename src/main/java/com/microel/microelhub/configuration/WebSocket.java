package com.microel.microelhub.configuration;

import com.microel.microelhub.api.ApisStatusWS;
import com.microel.microelhub.api.ChatMessageWS;
import com.microel.microelhub.api.ChatWS;
import com.microel.microelhub.api.OperatorWS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@Configuration
@EnableWebSocket
public class WebSocket implements WebSocketConfigurer {

    private final ChatMessageWS chatMessageWS;
    private final ChatWS chatWs;
    private final ApisStatusWS apisStatusWS;
    private final OperatorWS operatorWS;

    public WebSocket(ChatMessageWS chatMessageWS, ChatWS chatWs, ApisStatusWS apisStatusWS, OperatorWS operatorWS) {
        this.chatMessageWS = chatMessageWS;
        this.chatWs = chatWs;
        this.apisStatusWS = apisStatusWS;
        this.operatorWS = operatorWS;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatMessageWS,"/api/ws/messages/{token}");
        registry.addHandler(chatWs,"/api/ws/chats/{token}");
        registry.addHandler(apisStatusWS,"/api/ws/api-statuses/{token}");
        registry.addHandler(operatorWS,"/api/ws/operators/{token}");
    }
}
