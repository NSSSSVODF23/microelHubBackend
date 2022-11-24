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
@Table(name = "fast_messages")
public class FastMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fastMessageId;
    @ManyToOne
    @JoinColumn(name = "operator_login")
    private Operator operator;
    @Column(length = 4096)
    private String message;
    private Timestamp created;
}
