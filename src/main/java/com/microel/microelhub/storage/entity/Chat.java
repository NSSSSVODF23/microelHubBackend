package com.microel.microelhub.storage.entity;

import com.microel.microelhub.common.chat.ChatState;
import com.microel.microelhub.common.chat.Platform;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID chatId;
    @ManyToOne
    @JoinColumn(name = "f_user_id")
    private User user;
    private Boolean active;
    private Timestamp created;
    private Timestamp lastMessage;
    @ManyToOne
    @JoinColumn(name = "f_operator_id")
    private Operator operator;
}
