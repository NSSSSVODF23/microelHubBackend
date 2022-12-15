package com.microel.microelhub.api;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.security.AuthenticationManager;
import com.microel.microelhub.storage.OperatorDispatcher;
import com.microel.microelhub.storage.entity.Chat;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ChatWS extends AbstractWebSocketHandler<ListUpdateWrapper<Chat>> {
    private final AuthenticationManager authenticationManager;
    private final OperatorDispatcher operatorDispatcher;

    public ChatWS(AuthenticationManager authenticationManager, OperatorDispatcher operatorDispatcher) {
        this.authenticationManager = authenticationManager;
        this.operatorDispatcher = operatorDispatcher;
    }

    @Override
    public void onReceiveMessage(ListUpdateWrapper<Chat> object) {

    }

    @Override
    public List<ListUpdateWrapper<Chat>> onNewConnection(String connectionToken) {
        try {
            DecodedJWT decodedJWT = authenticationManager.validateUserToken(connectionToken);
            if(decodedJWT == null) return null;
            operatorDispatcher.setStatus(decodedJWT.getSubject(),true);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void onCloseConnection(String connectionToken, Boolean isMultiple) {
        if(isMultiple) return;
        try {
            DecodedJWT decodedJWT = authenticationManager.validateUserToken(connectionToken);
            if(decodedJWT == null) return;
            operatorDispatcher.setStatus(decodedJWT.getSubject(),false);
        } catch (Exception ignored) {
        }
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
