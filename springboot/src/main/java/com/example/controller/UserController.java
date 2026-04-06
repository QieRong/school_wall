package com.example.controller;

import com.example.common.Result;
import com.example.dto.LoginDTO;
import com.example.dto.RegisterDTO;
import com.example.entity.User;
import com.example.entity.Visitor;
import com.example.service.UserService;
import com.example.utils.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public Result<User> login(@Valid @RequestBody LoginDTO dto) {
        User loginUser = userService.login(dto.getAccount(), dto.getPassword());
        String token = jwtUtils.createToken(loginUser.getId(), loginUser.getAccount());
        loginUser.setToken(token);
        return Result.success(loginUser);
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        User user = new User();
        user.setAccount(dto.getAccount());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        userService.register(user);
        return Result.success(null);
    }

    // 搜索用户（分层架构规范：通过Service层调用）
    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) {
        return Result.success(userService.searchUsers(keyword));
    }

    // 获取用户资料 (含统计 + 自动记录访客)
    @GetMapping("/profile")
    public Result<User> getProfile(@RequestParam Long targetId, @RequestParam(required = false) Long currentId) {
        User user = userService.getUserProfile(targetId, currentId);
        return Result.success(user);
    }

    // 更新资料
    @PostMapping("/update")
    public Result<?> update(@RequestBody User user, @RequestHeader("Authorization") String token) {
        // 从token中获取当前用户ID
        Long currentUserId = jwtUtils.verifyToken(token.replace("Bearer ", ""));

        if (currentUserId == null) {
            return Result.error("Token无效或已过期");
        }

        // 权限校验：只能修改自己的资料
        if (!currentUserId.equals(user.getId())) {
            return Result.error("无权修改他人资料");
        }

        userService.updateProfile(user);
        return Result.success(null);
    }

    // 获取访客列表
    @GetMapping("/visitors")
    public Result<List<Visitor>> getVisitors(@RequestParam Long userId) {
        return Result.success(userService.getRecentVisitors(userId));
    }
}
