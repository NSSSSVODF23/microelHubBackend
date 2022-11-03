package com.microel.microelhub.storage.repository;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.storage.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID>, JpaSpecificationExecutor<Chat> {
    Chat findTopByChatIdAndUser_PlatformOrderByCreatedDesc(UUID chatId, Platform platform);
    Chat findTopByUser_UserIdAndUser_PlatformOrderByCreatedDesc(String userId, Platform platform);
    Page<Chat> findByActive(Boolean isActive, Pageable pageable);
    List<Chat> findAllByActiveOrderByLastMessageDesc(boolean active);
}
