package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

import java.sql.Time;

@Getter
@Setter
public class ChangeConfigBody {
    private String operatorLogin;
    private String greeting;
    private String warning;
    private Time startWorkingDay;
    private Time endWorkingDay;
    private Integer chatTimeout;
    private String vkGroupId;
    private String vkGroupToken;
    private String tlgBotUsername;
    private String tlgBotToken;
}
