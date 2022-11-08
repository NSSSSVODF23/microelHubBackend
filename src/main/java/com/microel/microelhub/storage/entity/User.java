package com.microel.microelhub.storage.entity;

import com.microel.microelhub.common.chat.Platform;
import lombok.*;

import javax.persistence.*;
import java.util.List;

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
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="user_phones", joinColumns=@JoinColumn(name="user_id"))
    private List<String> phones;
    private String billingLogin;
    private String name;
    private Platform platform;
}
