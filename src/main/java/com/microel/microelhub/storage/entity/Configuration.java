package com.microel.microelhub.storage.entity;

import lombok.*;

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
    @ManyToOne
    @JoinColumn(name = "f_operator")
    private Operator edited;
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
