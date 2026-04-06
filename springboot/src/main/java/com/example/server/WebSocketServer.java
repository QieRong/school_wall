// File: springboot/src/main/java/com/example/server/WebSocketServer.java
package com.example.server;

import cn.hutool.json.JSONObject;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import com.example.utils.JwtUtils;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/{token}")
@Component
public class WebSocketServer {

    private static final ConcurrentHashMap<Long, Session> sessionMap = new ConcurrentHashMap<>();
    private static JwtUtils jwtUtils;
    private static UserMapper userMapper;

    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        WebSocketServer.jwtUtils = jwtUtils;
    }

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        WebSocketServer.userMapper = userMapper;
    }

    private Long userId;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        Long uid = jwtUtils.verifyToken(token);
        if (uid == null) {
            try {
                session.close();
            } catch (IOException e) {
            }
            return;
        }
        this.userId = uid;
        sessionMap.put(uid, session);
        // 用户上线，LastActiveTime 可设为 NULL 或当前时间，视业务逻辑定
        // 这里暂不操作数据库，仅在下线时更新
    }

    @OnClose
    public void onClose() {
        if (this.userId != null) {
            sessionMap.remove(this.userId);
            // 【核心】用户下线，记录时间
            updateLastActiveTime(this.userId);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        if (this.userId != null)
            sessionMap.remove(this.userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 【修复】响应客户端心跳
        if ("ping".equals(message)) {
            try {
                session.getBasicRemote().sendText("pong");
            } catch (IOException e) {
                // 发送失败，忽略
            }
        }
    }

    // 更新最后活跃时间
    private void updateLastActiveTime(Long uid) {
        try {
            User user = new User();
            user.setId(uid);
            user.setLastActiveTime(LocalDateTime.now());
            userMapper.updateById(user);
        } catch (Exception e) {
            // 服务关闭时数据库连接可能已断开，忽略此类错误以避免控制台报错
        }
    }

    // 推送通知
    public static void sendNotification(Long receiverId, String content, int type) {
        Session session = sessionMap.get(receiverId);
        if (session != null && session.isOpen()) {
            try {
                JSONObject msg = new JSONObject();
                msg.set("type", "NOTICE");
                msg.set("noticeType", type);
                msg.set("content", content);
                session.getBasicRemote().sendText(msg.toString());
            } catch (IOException e) {
            }
        }
    }

    // 推送私信
    public static void sendPrivateMessage(Long receiverId, Object chatMessage) {
        Session session = sessionMap.get(receiverId);
        if (session != null && session.isOpen()) {
            try {
                JSONObject msg = new JSONObject();
                msg.set("type", "CHAT");
                msg.set("data", chatMessage);
                session.getBasicRemote().sendText(msg.toString());
            } catch (IOException e) {
            }
        }
    }

    // 检查用户是否在线
    public static boolean isOnline(Long userId) {
        Session session = sessionMap.get(userId);
        // 【优化】检查Session是否真正有效
        if (session != null && session.isOpen()) {
            return true;
        } else if (session != null) {
            // Session存在但已关闭，清理掉
            sessionMap.remove(userId);
        }
        return false;
    }

    /**
     * 【优化】清理无效Session
     * 可由定时任务调用，清理已断开但未正常关闭的连接
     */
    public static void cleanupInvalidSessions() {
        sessionMap.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            return session == null || !session.isOpen();
        });
    }

    /**
     * 获取当前在线人数
     */
    public static int getOnlineCount() {
        return (int) sessionMap.values().stream().filter(s -> s != null && s.isOpen()).count();
    }

    /**
     * 广播消息给所有在线用户
     */
    public static void broadcast(String message) {
        sessionMap.values().forEach(session -> {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    // 发送失败，忽略
                }
            }
        });
    }
}