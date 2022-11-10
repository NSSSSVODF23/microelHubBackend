package com.microel.microelhub.storage.repository;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.storage.OffsetRequest;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long>, JpaSpecificationExecutor<Message> {
    Message findTopByChat_User_UserIdAndChatMsgIdAndChat_User_PlatformOrderByTimestampDesc(String userId, String chatMsgId, Platform platform);

    Page<Message> findByChat_ChatId(UUID chatId, Pageable pageable);

    Optional<Message> findTopByChatAndChatMsgId(Chat chat, String chatMsgId);

    Page<Message> findByChat_User_UserIdAndChat_User_Platform(String userId, Platform platform, Pageable pageable);

    Optional<Message> findTopByChat_User_UserIdAndChat_User_Platform(String userId, Platform platform, Sort sort);
}
