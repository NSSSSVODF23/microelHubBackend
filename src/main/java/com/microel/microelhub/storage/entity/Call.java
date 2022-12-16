package com.microel.microelhub.storage.entity;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "calls")
public class Call {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long callId;
    private String phone;
    private Timestamp created;
    @ManyToOne()
    @JoinColumn(name = "f_operator_login")
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Operator processed;
}
