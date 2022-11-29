package com.microel.microelhub.api.transport;

import com.microel.microelhub.services.internal.Message;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class WebMessagesPage {
    private List<Message> content = new ArrayList<>();
    private Long totalElements = 0L;

    public static WebMessagesPage of(Page<com.microel.microelhub.storage.entity.Message> page){
        final WebMessagesPage webMessagesPage = new WebMessagesPage();
        webMessagesPage.totalElements = page.getTotalElements();
        webMessagesPage.content = page.getContent()
                .stream()
                .map(m->new Message(m.getChatMsgId(),
                        UUID.fromString(m.getChat().getUser().getUserId()),
                        m.getText(),
                        m.getOperatorMsg(),
                        false,
                        m.getAttachments()))
                .collect(Collectors.toList());
        return webMessagesPage;
    }
}
