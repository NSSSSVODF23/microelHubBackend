package com.microel.microelhub.storage.entity;

import com.microel.microelhub.common.chat.ChatState;
import com.microel.microelhub.common.chat.Platform;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.stereotype.Indexed;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chats", indexes = @Index(name = "idx_timestamp", columnList = "created"))
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID chatId;
    @ManyToOne()
    @JoinColumn(name = "f_user_id")
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private User user;
    private Boolean active;
    private Timestamp created;
    private Timestamp firstMessage;
    private Timestamp lastMessage;
    private Long initialDelay;
    private Long duration;
    private Integer messageCount;
    private Integer unreadCount;
    @ManyToOne()
    @JoinColumn(name = "f_operator_id")
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Operator operator;

    public void increaseMessagesCount(){
        if(messageCount == null) messageCount = 0;
        messageCount++;
    }
}
