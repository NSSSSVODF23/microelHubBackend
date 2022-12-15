package com.microel.microelhub.api;

import com.microel.microelhub.api.transport.ApiStatus;
import com.microel.microelhub.security.AuthenticationManager;
import com.microel.microelhub.services.StatedApiService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ApisStatusWS extends AbstractWebSocketHandler<ApiStatus> {

    private final AuthenticationManager authenticationManager;
    private final StatedApiService statedApiService;

    public ApisStatusWS(AuthenticationManager authenticationManager,@Lazy StatedApiService statedApiService) {
        this.authenticationManager = authenticationManager;
        this.statedApiService = statedApiService;
    }

    @Override
    public void onReceiveMessage(ApiStatus object) {

    }

    @Override
    public List<ApiStatus> onNewConnection(String connectionToken) {
        return statedApiService.getStatuses();
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
