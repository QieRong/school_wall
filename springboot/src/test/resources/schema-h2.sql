-- H2 测试数据库 Schema
-- 简化版本，用于单元测试

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255) DEFAULT '/default.png',
    role TINYINT DEFAULT 0 COMMENT '0:普通用户 1:管理员',
    status TINYINT DEFAULT 1 COMMENT '0:封禁 1:正常',
    credit_score INT DEFAULT 50,
    violation_count INT DEFAULT 0,
    ban_end_time TIMESTAMP,
    cover_image VARCHAR(255),
    last_nickname_update TIMESTAMP,
    last_active_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 帖子表
CREATE TABLE IF NOT EXISTS post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    images TEXT,
    category TINYINT DEFAULT 1,
    is_anonymous TINYINT DEFAULT 0,
    visibility TINYINT DEFAULT 0,
    view_count INT DEFAULT 0,
    status TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    poll_options TEXT,
    poll_end_time TIMESTAMP,
    tags VARCHAR(500),
    scheduled_time TIMESTAMP,
    location VARCHAR(255),
    video VARCHAR(500),
    end_time TIMESTAMP,
    reward VARCHAR(255),
    deadline TIMESTAMP
);

-- 评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    reply_user_id BIGINT,
    content VARCHAR(500) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    like_count INT DEFAULT 0,
    img_url VARCHAR(255)
);

-- 点赞表
CREATE TABLE IF NOT EXISTS sys_post_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (post_id, user_id)
);

-- 收藏表
CREATE TABLE IF NOT EXISTS sys_collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, post_id)
);

-- 关注表
CREATE TABLE IF NOT EXISTS sys_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (follower_id, target_id)
);

-- 黑名单表
CREATE TABLE IF NOT EXISTS sys_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, target_id)
);

-- 私信表
CREATE TABLE IF NOT EXISTS sys_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    type TINYINT DEFAULT 0,
    is_read TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_withdrawn TINYINT DEFAULT 0
);

-- 通知表
CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content VARCHAR(255) NOT NULL,
    type TINYINT NOT NULL,
    is_read TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sender_id BIGINT DEFAULT 0,
    related_id BIGINT
);

-- AI使用记录表
CREATE TABLE IF NOT EXISTS ai_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    prompt VARCHAR(500),
    result TEXT,
    usage_date DATE NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 投票记录表
CREATE TABLE IF NOT EXISTS post_vote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    option_index INT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (post_id, user_id)
);

-- 举报表
CREATE TABLE IF NOT EXISTS sys_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 敏感词表
CREATE TABLE IF NOT EXISTS sys_sensitive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 公告表
CREATE TABLE IF NOT EXISTS sys_announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_top TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分类表
CREATE TABLE IF NOT EXISTS sys_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50) DEFAULT 'Hash',
    color VARCHAR(50) DEFAULT 'text-gray-500',
    bg_color VARCHAR(50) DEFAULT 'bg-gray-100',
    sort INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 访客记录表
CREATE TABLE IF NOT EXISTS sys_visitor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    visitor_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, visitor_id)
);

-- 文件记录表
CREATE TABLE IF NOT EXISTS sys_file_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    is_used TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 位置统计表
CREATE TABLE IF NOT EXISTS location_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_name VARCHAR(100) NOT NULL,
    lng DECIMAL(10, 6),
    lat DECIMAL(10, 6),
    post_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    category INT DEFAULT 1,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (location_name, category)
);