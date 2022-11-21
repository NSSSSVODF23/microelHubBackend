package com.microel.microelhub.api.transport;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadOperatorAvatarRequest {
    private String login;
    private String image;
}
