package com.microel.microelhub.storage.repository;

import com.microel.microelhub.common.chat.Platform;
import com.microel.microelhub.storage.entity.Chat;
import com.microel.microelhub.storage.proxies.CGroupStatisticData;
import com.microel.microelhub.storage.proxies.RDStatisticPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID>, JpaSpecificationExecutor<Chat> {
    Chat findTopByChatIdAndUser_PlatformOrderByCreatedDesc(UUID chatId, Platform platform);

    Chat findTopByUser_UserIdAndUser_PlatformOrderByCreatedDesc(String userId, Platform platform);

    Page<Chat> findByActive(Boolean isActive, Pageable pageable);

    List<Chat> findAllByActiveOrderByLastMessageDesc(boolean active);

    @Query(value = "WITH w_messages AS (\n" +
            "    SELECT DISTINCT f_chat_id FROM messages m JOIN chats c on c.chat_id = m.f_chat_id JOIN users u on u.user_id = c.f_user_id\n" +
            "    WHERE (:query IS NULL OR to_tsvector('russian', m.text) @@ websearch_to_tsquery('russian', :query) OR u.billing_login LIKE concat('%',:query,'%'))\n" +
            ")\n" +
            "SELECT * FROM chats\n" +
            "    JOIN w_messages m on chats.chat_id = m.f_chat_id\n" +
            "    JOIN operators o on o.login = chats.f_operator_id\n" +
            "         WHERE\n" +
            "           chats.active = false AND" +
            "           (:who IS NULL OR o.login = :who)\n" +
            "           AND (:start IS NULL OR chats.created >= cast(:start as timestamp))\n" +
            "           AND (:end IS NULL OR chats.created <= cast(:end as timestamp))\n" +
            "         ORDER BY chats.created DESC\n" +
            "         OFFSET :offset LIMIT :limit", nativeQuery = true)
    Page<Chat> getFilteredPage(@Param("query") String query, @Param("who") String who, @Param("start") String start, @Param("end") String end, @Param("offset") Long offset, @Param("limit") Integer limit, Pageable pageable);

    @Query(value = "WITH a_chats AS (SELECT chat_id, initial_delay, duration, chats.created, date_trunc('day', chats.created) as day\n" +
            "                 FROM chats\n" +
            "                          JOIN users u on u.user_id = chats.f_user_id\n" +
            "                          JOIN operators o on o.login = chats.f_operator_id\n" +
            "                 WHERE (:platform IS NULL OR u.platform = :platform)\n" +
            "                   AND (:login IS NULL OR o.login = :login)\n" +
            "                   AND (:start IS NULL OR chats.created >= cast(:start as timestamp))\n" +
            "                   AND (:end IS NULL OR chats.created <= cast(:end as timestamp)))\n" +
            "SELECT day,\n" +
            "       count(chat_id) filter ( where initial_delay < 30000 )                              AS sId,\n" +
            "       count(chat_id) filter ( where initial_delay > 30000 AND initial_delay < 120000 )   AS mId,\n" +
            "       count(chat_id) filter ( where initial_delay > 120000 AND initial_delay < 600000 )  AS lId,\n" +
            "       count(chat_id) filter ( where initial_delay > 600000 ) AS xId,\n" +
            "       count(chat_id) filter ( where duration < 300000 )                                   AS sD,\n" +
            "       count(chat_id) filter ( where duration > 300000 AND duration < 900000 )             AS mD,\n" +
            "       count(chat_id) filter ( where duration > 900000 AND duration < 1800000 )            AS lD,\n" +
            "       count(chat_id) filter ( where duration > 1800000 )           AS xD,\n" +
            "       count(chat_id)                                            AS a,\n" +
            "       avg(initial_delay)                                        AS avgDelay,\n" +
            "       avg(duration)                                             AS avgDuration\n" +
            "FROM a_chats\n" +
            "GROUP BY day ORDER BY day;", nativeQuery = true)
    List<RDStatisticPoint> getStatisticGroupedByDay(@Param("platform") Integer platform,
                                                    @Param("login") String login,
                                                    @Param("start") String start,
                                                    @Param("end") String end);

    @Query(value = "WITH a_chats AS (SELECT chat_id, initial_delay, duration, chats.created\n" +
            "                 FROM chats\n" +
            "                          JOIN users u on u.user_id = chats.f_user_id\n" +
            "                          JOIN operators o on o.login = chats.f_operator_id\n" +
            "                 WHERE (:platform IS NULL OR u.platform = :platform)\n" +
            "                   AND (:login IS NULL OR o.login = :login)\n" +
            "                   AND (:start IS NULL OR chats.created >= cast(:start as timestamp))\n" +
            "                   AND (:end IS NULL OR chats.created <= cast(:end as timestamp)))\n" +
            "SELECT \n" +
            "       count(chat_id) filter ( where initial_delay < 30000 )                              AS sId,\n" +
            "       count(chat_id) filter ( where initial_delay > 30000 AND initial_delay < 120000 )   AS mId,\n" +
            "       count(chat_id) filter ( where initial_delay > 120000 AND initial_delay < 600000 )  AS lId,\n" +
            "       count(chat_id) filter ( where initial_delay > 600000 ) AS xId,\n" +
            "       count(chat_id) filter ( where duration < 300000 )                                   AS sD,\n" +
            "       count(chat_id) filter ( where duration > 300000 AND duration < 900000 )             AS mD,\n" +
            "       count(chat_id) filter ( where duration > 900000 AND duration < 1800000 )            AS lD,\n" +
            "       count(chat_id) filter ( where duration > 1800000 )           AS xD,\n" +
            "       count(chat_id)                                            AS a,\n" +
            "       avg(initial_delay)                                        AS avgDelay,\n" +
            "       avg(duration)                                             AS avgDuration\n" +
            "FROM a_chats\n", nativeQuery = true)
    RDStatisticPoint getStatisticUngrouped(@Param("platform") Integer platform,
                                                    @Param("login") String login,
                                                    @Param("start") String start,
                                                    @Param("end") String end);

    @Query(value = "WITH a_chats AS (SELECT chat_id, initial_delay, duration, chats.created, platform\n" +
            "                 FROM chats\n" +
            "                          JOIN users u on u.user_id = chats.f_user_id\n" +
            "                          JOIN operators o on o.login = chats.f_operator_id\n" +
            "                 WHERE (:start IS NULL OR chats.created >= cast(:start as timestamp))\n" +
            "                   AND (:end IS NULL OR chats.created <= cast(:end as timestamp))\n" +
            "                 )\n" +
            "SELECT platform AS grp ,\n" +
            "       count(chat_id) filter ( where initial_delay < 30000 )                              AS s,\n" +
            "       count(chat_id) filter ( where initial_delay > 30000 AND initial_delay < 120000 )   AS m,\n" +
            "       count(chat_id) filter ( where initial_delay > 120000 AND initial_delay < 600000 )  AS l,\n" +
            "       count(chat_id) filter ( where initial_delay > 600000 ) AS x,\n" +
            "       count(chat_id)                                            AS a,\n" +
            "       avg(initial_delay)                                        AS avg\n" +
            "FROM a_chats\n" +
            "GROUP BY grp ORDER BY grp", nativeQuery = true)
    List<CGroupStatisticData> getStatisticDelayGroupedBySource(String start, String end);

    @Query(value = "WITH a_chats AS (SELECT chat_id, initial_delay, duration, chats.created, platform\n" +
            "                 FROM chats\n" +
            "                          JOIN users u on u.user_id = chats.f_user_id\n" +
            "                          JOIN operators o on o.login = chats.f_operator_id\n" +
            "                 WHERE (:start IS NULL OR chats.created >= cast(:start as timestamp))\n" +
            "                   AND (:end IS NULL OR chats.created <= cast(:end as timestamp))\n" +
            ")\n" +
            "SELECT platform AS grp ,\n" +
            "       count(chat_id) filter ( where duration < 300000 )                                   AS s,\n" +
            "       count(chat_id) filter ( where duration > 300000 AND duration < 900000 )             AS m,\n" +
            "       count(chat_id) filter ( where duration > 900000 AND duration < 1800000 )            AS l,\n" +
            "       count(chat_id) filter ( where duration > 1800000 )           AS x,\n" +
            "       count(chat_id)                                            AS a,\n" +
            "       avg(duration)                                        AS avg\n" +
            "FROM a_chats\n" +
            "GROUP BY grp ORDER BY grp", nativeQuery = true)
    List<CGroupStatisticData> getStatisticDurationGroupedBySource(String start, String end);

    @Query(value = "WITH a_chats AS (SELECT chat_id, initial_delay, duration, chats.created, login, o.name\n" +
            "                 FROM chats\n" +
            "                          JOIN users u on u.user_id = chats.f_user_id\n" +
            "                          JOIN operators o on o.login = chats.f_operator_id\n" +
            "                 WHERE (:start IS NULL OR chats.created >= cast(:start as timestamp))\n" +
            "                   AND (:end IS NULL OR chats.created <= cast(:end as timestamp))\n" +
            ")\n" +
            "SELECT name AS grp ,\n" +
            "       count(chat_id) filter ( where initial_delay < 30000 )                              AS s,\n" +
            "       count(chat_id) filter ( where initial_delay > 30000 AND initial_delay < 120000 )   AS m,\n" +
            "       count(chat_id) filter ( where initial_delay > 120000 AND initial_delay < 600000 )  AS l,\n" +
            "       count(chat_id) filter ( where initial_delay > 600000 ) AS x,\n" +
            "       count(chat_id)                                            AS a,\n" +
            "       avg(initial_delay)                                        AS avg\n" +
            "FROM a_chats\n" +
            "GROUP BY login, name ORDER BY login", nativeQuery = true)
    List<CGroupStatisticData> getStatisticDelayGroupedByOperator(String start, String end);

    @Query(value = "WITH a_chats AS (SELECT chat_id, initial_delay, duration, chats.created, login, o.name\n" +
            "                 FROM chats\n" +
            "                          JOIN users u on u.user_id = chats.f_user_id\n" +
            "                          JOIN operators o on o.login = chats.f_operator_id\n" +
            "                 WHERE (:start IS NULL OR chats.created >= cast(:start as timestamp))\n" +
            "                   AND (:end IS NULL OR chats.created <= cast(:end as timestamp))\n" +
            ")\n" +
            "SELECT name AS grp ,\n" +
            "       count(chat_id) filter ( where duration < 300000 )                                   AS s,\n" +
            "       count(chat_id) filter ( where duration > 300000 AND duration < 900000 )             AS m,\n" +
            "       count(chat_id) filter ( where duration > 900000 AND duration < 1800000 )            AS l,\n" +
            "       count(chat_id) filter ( where duration > 1800000 )           AS x,\n" +
            "       count(chat_id)                                            AS a,\n" +
            "       avg(duration)                                        AS avg\n" +
            "FROM a_chats\n" +
            "GROUP BY login, name ORDER BY login", nativeQuery = true)
    List<CGroupStatisticData> getStatisticDurationGroupedByOperator(String start, String end);
}
