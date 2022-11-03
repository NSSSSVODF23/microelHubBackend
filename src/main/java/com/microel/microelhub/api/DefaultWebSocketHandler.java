package com.microel.microelhub.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class DefaultWebSocketHandler <T> extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String[] strings = Objects.requireNonNull(session.getUri()).getPath().split("/");
        String token = strings[strings.length-1];
        if(!isAuthorize(token)) session.close(CloseStatus.SERVER_ERROR);
        sessions.add(session);
        List<T> initialsObject = onNewConnection();
        if(initialsObject != null) initialsObject.forEach(o->sendUnicast(session,o));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            T object = objectMapper.readValue(message.getPayload(), new TypeReference<T>(){});
            onReceiveMessage(object);
        }catch (Exception ignored){}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    public void sendMessage(T object){
        sessions.forEach(session -> sendUnicast(session,object));
    }

    private void sendUnicast(WebSocketSession session, T object){
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(object)));
        } catch (IOException e) {
            log.warn("Не удалось отправить сообщение через WebSocket");
        }
    }

    abstract public void onReceiveMessage(T object);

    abstract public List<T> onNewConnection();

    abstract public boolean isAuthorize(String token);
}
