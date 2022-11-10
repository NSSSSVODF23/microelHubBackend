package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.security.AuthenticationManager;
import com.microel.microelhub.storage.entity.Message;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatMessageWS extends AbstractWebSocketHandler<ListUpdateWrapper<Message>> {

    private final AuthenticationManager authenticationManager;

    public ChatMessageWS(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void onReceiveMessage(ListUpdateWrapper<Message> object) {
        System.out.println(object);
    }

    @Override
    public List<ListUpdateWrapper<Message>> onNewConnection(String connectionToken) {
        return null;
    }

    @Override
    public boolean isAuthorize(String token) {
        try {
            return authenticationManager.validateUserToken(token) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
