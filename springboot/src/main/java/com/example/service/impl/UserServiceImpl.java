package com.example.service.impl;

import cn.hutool.core.util.ReUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.example.entity.User;
import com.example.entity.Visitor;
import com.example.mapper.PostMapper;
import com.example.mapper.UserMapper;
import com.example.mapper.VisitorMapper;
import com.example.mapper.FollowMapper;
import com.example.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 新增导入

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private VisitorMapper visitorMapper;
    @Resource
    private FollowMapper followMapper;

    @Override
    public User login(String account, String password) {
        User dbUser = userMapper.selectByAccount(account);
        if (dbUser == null) {
            throw new RuntimeException("账号不存在！");
        }

        // 使用BCrypt验证密码
        boolean pwMatch = BCrypt.checkpw(password.trim(), dbUser.getPassword());

        if (!pwMatch) {
            throw new RuntimeException("账号或密码错误！");
        }

        // 检查封号状态：只有 status=1 才允许登录
        Integer userStatus = dbUser.getStatus();
        if (userStatus == null || userStatus != 1) {
            // status 不为 1（包含 NULL、0 等情况）都视为封禁
            // 仅当 ban_end_time 非空且已过期时自动解封
            if (dbUser.getBanEndTime() != null && dbUser.getBanEndTime().isBefore(LocalDateTime.now())) {
                // 封禁到期，自动解封
                dbUser.setStatus(1);
                dbUser.setBanEndTime(null);
                userMapper.updateById(dbUser);
            } else {
                // 仍在封禁中（包括永久封禁 ban_end_time=NULL 的情况）
                String msg = dbUser.getBanEndTime() != null
                        ? "该账号已被封禁，解封时间: " + dbUser.getBanEndTime()
                        : "该账号已被封禁";
                throw new RuntimeException(msg);
            }
        }
        // 填充统计数据
        dbUser.setTotalLikes(postMapper.sumLikesByUserId(dbUser.getId()));
        dbUser.setTotalVisits(visitorMapper.countByUserId(dbUser.getId()));
        dbUser.setFollowingCount(followMapper.countFollowings(dbUser.getId()));
        dbUser.setFollowerCount(followMapper.countFollowers(dbUser.getId()));

        return dbUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 【修复】添加事务管理
    public void register(User user) {
        // 1. 账号校验：10位学号 或 11位手机 或 3-15位字母（管理员）
        if (!ReUtil.isMatch("^(\\d{10}|\\d{11}|[a-zA-Z]{3,15})$", user.getAccount())) {
            throw new RuntimeException("账号格式错误，请输入10位学号、11位手机号");
        }

        // 2. 密码校验：仅校验长度 >= 6
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }

        User exist = userMapper.selectByAccount(user.getAccount());
        if (exist != null) {
            throw new RuntimeException("该账号已存在");
        }

        // 3. 初始化默认信息
        if (user.getNickname() == null) {
            String suffix = user.getAccount().length() > 4 ? user.getAccount().substring(user.getAccount().length() - 4)
                    : user.getAccount();
            user.setNickname("张院" + suffix);
        }

        // 使用BCrypt加密密码
        String hashedPassword = BCrypt.hashpw(user.getPassword());
        user.setPassword(hashedPassword);

        user.setRole(0);
        user.setAvatar("/files/default.png");
        user.setCreditScore(100);
        user.setViolationCount(0);
        user.setStatus(1);

        userMapper.insert(user);
    }

    @Override
    public void checkUserPermission(Long userId, int actionType) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 与登录逻辑一致：status 不为 1 就视为封禁
        Integer status = user.getStatus();
        if (status == null || status != 1) {
            throw new RuntimeException("您已被封禁，无法进行此操作");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 【修复】添加事务管理
    public void punishUser(Long userId, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null)
            return;

        int oldScore = user.getCreditScore() != null ? user.getCreditScore() : 100;
        int newScore = Math.max(0, oldScore - 10);

        user.setCreditScore(newScore);
        user.setViolationCount((user.getViolationCount() == null ? 0 : user.getViolationCount()) + 1);

        if (newScore < 60) {
            user.setStatus(0);
            user.setBanEndTime(LocalDateTime.now().plusDays(3));
        }

        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 【修复】添加事务管理
    public User getUserProfile(Long targetUserId, Long currentUserId) {
        User user = userMapper.selectById(targetUserId);
        if (user == null)
            throw new RuntimeException("用户不存在");

        user.setTotalLikes(postMapper.sumLikesByUserId(targetUserId));
        user.setTotalVisits(visitorMapper.countByUserId(targetUserId));
        // 补充关注数和粉丝数
        user.setFollowingCount(followMapper.countFollowings(targetUserId));
        user.setFollowerCount(followMapper.countFollowers(targetUserId));

        if (currentUserId != null && !currentUserId.equals(targetUserId)) {
            int visited = visitorMapper.checkTodayVisit(targetUserId, currentUserId);
            if (visited == 0) {
                Visitor visitor = new Visitor();
                visitor.setUserId(targetUserId);
                visitor.setVisitorId(currentUserId);
                visitor.setVisitTime(LocalDateTime.now());
                visitorMapper.insert(visitor);
            }
        }

        // 清空密码（双重保护）
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 【修复】添加事务管理
    public void updateProfile(User form) {
        User dbUser = userMapper.selectById(form.getId());
        if (dbUser == null)
            throw new RuntimeException("用户不存在");

        if (form.getNickname() != null && !form.getNickname().equals(dbUser.getNickname())) {
            if (dbUser.getLastNicknameUpdate() != null) {
                long days = ChronoUnit.DAYS.between(dbUser.getLastNicknameUpdate(), LocalDateTime.now());
                if (days < 30) {
                    throw new RuntimeException("昵称修改太频繁，请 " + (30 - days) + " 天后再试");
                }
            }
            dbUser.setNickname(form.getNickname());
            dbUser.setLastNicknameUpdate(LocalDateTime.now());
        }

        if (form.getAvatar() != null)
            dbUser.setAvatar(form.getAvatar());
        if (form.getCoverImage() != null)
            dbUser.setCoverImage(form.getCoverImage());

        userMapper.updateById(dbUser);
    }

    @Override
    public List<Visitor> getRecentVisitors(Long userId) {
        return visitorMapper.selectRecentVisitors(userId);
    }

    /**
     * 搜索用户
     * 实现分层架构：Controller → Service → Mapper
     * 安全防护：清空密码字段，防止敏感信息泄露
     */
    @Override
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        List<User> users = userMapper.searchUsers(keyword);

        // 清空敏感信息（双重保护）
        users.forEach(user -> user.setPassword(null));

        return users;
    }
}
