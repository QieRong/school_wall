package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

        @Select("SELECT * FROM sys_user WHERE account = #{account}")
        User selectByAccount(String account);

        // 【核心修改】搜索用户时，增加 AND role != 1，彻底屏蔽管理员
        @Select("SELECT id, nickname, avatar, account FROM sys_user " +
                        "WHERE (nickname LIKE CONCAT('%',#{keyword},'%') OR account LIKE CONCAT('%',#{keyword},'%')) " +
                        "AND status = 1 AND role != 1 " + // <--- 关键修改
                        "LIMIT 10")
        List<User> searchUsers(String keyword);
}