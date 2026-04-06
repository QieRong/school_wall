// File: springboot/src/main/java/com/example/controller/ChatController.java
package com.example.controller;

import com.example.common.Result;
import com.example.entity.Chat;
import com.example.mapper.ChatMapper;
import com.example.server.WebSocketServer;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatMapper chatMapper;

    /**
     * 发送私信
     */
    @PostMapping("/send")
    public Result<?> send(@RequestBody Chat chat) {
        if (chat.getContent() == null || chat.getContent().trim().isEmpty()) {
            return Result.error("内容不能为空");
        }

        // 【防骚扰】检查对方是否回复过
        // 查询对方给我发送的消息数量
        int receivedCount = chatMapper.countBySenderAndReceiver(chat.getReceiverId(), chat.getSenderId());

        if (receivedCount == 0) {
            // 对方从未回复过，检查我是否已经发送过消息
            int sentCount = chatMapper.countBySenderAndReceiver(chat.getSenderId(), chat.getReceiverId());
            if (sentCount >= 1) {
                return Result.error("对方尚未回复，请耐心等待~");
            }
        }

        chatMapper.insert(chat);

        // WebSocket 实时推送
        WebSocketServer.sendPrivateMessage(chat.getReceiverId(), chat);

        return Result.success(null);
    }

    /**
     * 获取联系人列表
     */
    @GetMapping("/contacts")
    public Result<List<Map<String, Object>>> contacts(@RequestParam Long userId) {
        List<Map<String, Object>> list = chatMapper.selectContactList(userId);

        // 填充在线状态
        for (Map<String, Object> map : list) {
            Long uid = Long.valueOf(map.get("id").toString());
            boolean isOnline = WebSocketServer.isOnline(uid);
            map.put("isOnline", isOnline);
        }
        return Result.success(list);
    }

    /**
     * 获取详细聊天记录
     */
    @GetMapping("/history")
    public Result<List<Chat>> history(@RequestParam Long userId, @RequestParam Long targetId) {
        // 标记所有未读为已读
        chatMapper.readAll(userId, targetId);
        return Result.success(chatMapper.selectHistory(userId, targetId));
    }

    /**
     * 撤回消息
     */
    @PostMapping("/withdraw")
    public Result<?> withdraw(@RequestParam Long id, @RequestParam Long userId) {
        Chat chat = chatMapper.selectById(id);
        if (chat == null || !chat.getSenderId().equals(userId)) {
            return Result.error("无权撤回");
        }
        // 撤回时限检查
        if (Duration.between(chat.getCreateTime(), LocalDateTime.now())
                .toMinutes() > com.example.common.Constants.MESSAGE_WITHDRAW_MINUTES) {
            return Result.error("超过" + com.example.common.Constants.MESSAGE_WITHDRAW_MINUTES + "分钟，无法撤回");
        }

        chatMapper.withdraw(id);
        return Result.success(null);
    }
}