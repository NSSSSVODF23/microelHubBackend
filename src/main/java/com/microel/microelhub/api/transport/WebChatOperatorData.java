package com.microel.microelhub.api.transport;

import com.microel.microelhub.storage.entity.Operator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WebChatOperatorData {
    private String operatorName;
    private String operatorAvatar;

    public static WebChatOperatorData from(Operator operator) {
        if(operator == null) return null;
        return new WebChatOperatorData(operator.getName(), operator.getAvatar());
    }
}
