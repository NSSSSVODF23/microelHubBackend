package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.ListUpdateWrapper;
import com.microel.microelhub.security.AuthenticationManager;
import com.microel.microelhub.storage.entity.Operator;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class OperatorWS extends AbstractWebSocketHandler<ListUpdateWrapper<Operator>> {

    private final AuthenticationManager authenticationManager;

    public OperatorWS(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void onReceiveMessage(ListUpdateWrapper<Operator> object) {

    }

    @Override
    public List<ListUpdateWrapper<Operator>> onNewConnection(String connectionToken) {
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
