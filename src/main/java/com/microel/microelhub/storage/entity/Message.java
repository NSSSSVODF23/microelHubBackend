package com.microel.microelhub.storage.entity;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

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
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "f_operator_login")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Operator operator;
    private Timestamp timestamp;
    private Boolean operatorMsg;
    private Boolean edited;
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "f_chat_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Chat chat;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable()
    private Set<MessageAttachment> attachments;

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
