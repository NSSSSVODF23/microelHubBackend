package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.security.AuthenticationManager;
import com.microel.microelhub.storage.entity.Chat;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatWS extends DefaultWebSocketHandler<ListUpdateWrapper<Chat>>{
    private final AuthenticationManager authenticationManager;

    public ChatWS(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void onReceiveMessage(ListUpdateWrapper<Chat> object) {

    }

    @Override
    public List<ListUpdateWrapper<Chat>> onNewConnection() {
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
