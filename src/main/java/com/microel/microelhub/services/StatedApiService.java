package com.microel.microelhub.services;

import com.microel.microelhub.api.ApisStatusWS;
import com.microel.microelhub.api.transport.ApiStatus;
import com.microel.microelhub.common.chat.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StatedApiService {
    private final Map<Platform, ApiStatus> statusMap = new HashMap<>();
    private final ApisStatusWS apisStatusWS;

    public StatedApiService(ApisStatusWS apisStatusWS) {
        this.apisStatusWS = apisStatusWS;
    }

    public void logCreated(Platform platform) {
        ApiStatus status = new ApiStatus(platform, "API не инициализирован");
        statusMap.put(platform, status);
        apisStatusWS.sendMessage(status);
        log.info("{} API не инициализирован",platform);
    }

    public void logStatusChange(Platform platform, String text) {
        ApiStatus status = statusMap.get(platform);
        status.setStatus(text);
        apisStatusWS.sendMessage(status);
        log.info("{} {}",platform,text);
    }

    public List<ApiStatus> getStatuses(){
        return new ArrayList<>(statusMap.values());
    }
}
