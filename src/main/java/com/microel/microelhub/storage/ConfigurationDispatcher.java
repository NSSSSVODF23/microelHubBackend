package com.microel.microelhub.storage;

import com.microel.microelhub.api.transport.ChangeConfigBody;
import com.microel.microelhub.common.ChangeConfigurationHandle;
import com.microel.microelhub.storage.entity.Configuration;
import com.microel.microelhub.storage.entity.Operator;
import com.microel.microelhub.storage.repository.ConfigurationRepository;
import org.hibernate.loader.collection.OneToManyJoinWalker;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class ConfigurationDispatcher {
    private final ConfigurationRepository configurationRepository;
    private final Map<String, ChangeConfigurationHandle> configurationHandlersMap = new HashMap<>();

    public ConfigurationDispatcher(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public Configuration getLastConfig() {
        return configurationRepository.findTopByOrderByChangeDesc().orElse(null);
    }

    public void addChangeConfigurationHandler(String operatorName,ChangeConfigurationHandle handle){
        configurationHandlersMap.put(operatorName,handle);
    }

    public void update(ChangeConfigBody configBody){
        Configuration configuration = Configuration.builder()
                .edited(Operator.builder().login(configBody.getOperatorLogin()).build())
                .change(Timestamp.from(Instant.now()))
                .greeting(configBody.getGreeting())
                .warning(configBody.getWarning())
                .startWorkingDay(configBody.getStartWorkingDay())
                .endWorkingDay(configBody.getEndWorkingDay())
                .tlgBotToken(configBody.getTlgBotToken())
                .tlgBotUsername(configBody.getTlgBotUsername())
                .vkGroupToken(configBody.getVkGroupToken())
                .vkGroupId(configBody.getVkGroupId())
                .build();
        configurationRepository.save(configuration);
        configurationHandlersMap.values().forEach(ChangeConfigurationHandle::apply);
    }
}
