package com.microel.microelhub.storage.entity;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Time;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "configuration")
public class Configuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long config_id;
    private Timestamp change;
    @ManyToOne()
    @JoinColumn(name = "f_operator")
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Operator edited;
    private String greeting;
    private String warning;
    private Time startWorkingDay;
    private Time endWorkingDay;
    private Integer chatTimeout;
    private String vkUserId;
    private String vkUserToken;
    private String vkGroupId;
    private String vkGroupToken;
    private String tlgBotUsername;
    private String tlgBotToken;
    private String tlgNotificationChatId;
}
