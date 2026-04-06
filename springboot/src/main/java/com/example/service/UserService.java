package com.example.service;

import com.example.entity.User;
import com.example.entity.Visitor;

import java.util.List;

public interface UserService {

    User login(String account, String password);

    void register(User user);

    void checkUserPermission(Long userId, int actionType);

    void punishUser(Long userId, String reason);

    User getUserProfile(Long targetUserId, Long currentUserId);

    void updateProfile(User form);

    List<Visitor> getRecentVisitors(Long userId);

    /**
     * 搜索用户
     * 
     * @param keyword 搜索关键词（用户名或昵称）
     * @return 用户列表（不包含密码等敏感信息）
     */
    List<User> searchUsers(String keyword);
}
