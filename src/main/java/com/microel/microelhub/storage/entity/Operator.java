package com.microel.microelhub.storage.entity;

import com.microel.microelhub.common.OperatorGroup;
import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "operators")
public class Operator {
    @Id
    private String login;
    private String name;
    private String password;
    private OperatorGroup role;
    private Timestamp created;
    private Timestamp lastLogin;
    private Integer rating;
    private Boolean isOnline;
}
