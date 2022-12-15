package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.security.AuthenticationManager;
import com.microel.microelhub.storage.entity.Call;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class CallWS extends AbstractWebSocketHandler<ListUpdateWrapper<Call>> {

    private final AuthenticationManager authenticationManager;

    public CallWS(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void onReceiveMessage(ListUpdateWrapper<Call> object) {

    }

    @Override
    public List<ListUpdateWrapper<Call>> onNewConnection(String connectionToken) {
        return null;
    }

    @Override
    public void onCloseConnection(String connectionToken, Boolean isMultiple) {

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
