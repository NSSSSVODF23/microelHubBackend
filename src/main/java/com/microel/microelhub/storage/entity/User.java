package com.microel.microelhub.storage.entity;

import com.microel.microelhub.common.chat.Platform;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    private String userId;
    private String phone;
    private String name;
    private Platform platform;
}
