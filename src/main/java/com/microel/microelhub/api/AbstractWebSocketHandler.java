package com.microel.microelhub.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractWebSocketHandler<T> extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<String,WebSocketSession> connectionTokenSessionMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<T> messages = new ConcurrentLinkedDeque<T>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    private void pingSchedule() {
        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> {
            try {
                if(session.isOpen())
                    session.sendMessage(new PingMessage());
            } catch (IOException ignored) {
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String[] strings = Objects.requireNonNull(session.getUri()).getPath().split("/");
        String token = strings[strings.length - 1];
        if (!isAuthorize(token)) session.close(CloseStatus.SERVER_ERROR);
        sessions.add(session);
        connectionTokenSessionMap.put(token,session);
        List<T> initialsObject = onNewConnection(token);
        if (initialsObject != null) initialsObject.forEach(o -> sendUnicast(session, o));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            T object = objectMapper.readValue(message.getPayload(), new TypeReference<T>() {
            });
            onReceiveMessage(object);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        connectionTokenSessionMap.remove(session);
    }

    public void sendBroadcast(T object) {
        if(messages.size() == 0){
            messages.add(object);
            Executors.newSingleThreadExecutor().execute(()->{
                for (T message : messages){
                    sessions.forEach(session -> sendUnicast(session, message));
                }
                messages.clear();
            });
        }else{
            messages.add(object);
        }
    }

    private void sendUnicast(WebSocketSession session, T object) {
        try {
            if(session.isOpen()) session.sendMessage(new TextMessage(objectMapper.writeValueAsString(object)));
        } catch (IOException e) {
            log.warn("Не удалось отправить сообщение через WebSocket");
        }
    }

    public void sendUnicast(String connectionToken, T object) throws Exception {
        WebSocketSession session = connectionTokenSessionMap.get(connectionToken);
        if(session == null || !session.isOpen()) {
            log.info("Не удалось отправить сообщение в вэб версию чата");
            return;
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(object)));
    }

    abstract public void onReceiveMessage(T object);

    abstract public List<T> onNewConnection(String connectionToken);

    abstract public boolean isAuthorize(String token);
}
