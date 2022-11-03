package com.microel.microelhub.storage.entity;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;
    private String chatMsgId;
    @Column(columnDefinition = "text default ''")
    private String text;
    @ManyToOne
    @JoinColumn(name = "f_operator_login")
    private Operator operator;
    private Timestamp timestamp;
    private Boolean operatorMsg;
    private Boolean edited;
    @ManyToOne
    @JoinColumn(name = "f_chat_id")
    private Chat chat;

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", chatMsgId='" + chatMsgId + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                ", operatorMsg=" + operatorMsg +
                ", edited=" + edited +
                ", chat=" + chat +
                '}';
    }
}
