-- H2 测试数据初始化
-- 插入基础测试数据

-- 测试用户数据
INSERT INTO
    sys_user (
        id,
        account,
        password,
        nickname,
        role,
        status,
        credit_score
    )
VALUES (
        1,
        '2021001001',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
        '管理员',
        1,
        1,
        100
    ),
    (
        2,
        '2021001002',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
        '测试用户A',
        0,
        1,
        50
    ),
    (
        3,
        '2021001003',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
        '测试用户B',
        0,
        1,
        50
    ),
    (
        4,
        '2021001004',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
        '封禁用户',
        0,
        0,
        0
    );

-- 测试分类数据
INSERT INTO
    sys_category (
        id,
        name,
        icon,
        color,
        bg_color,
        sort,
        status
    )
VALUES (
        1,
        '表白',
        'Heart',
        'text-pink-500',
        'bg-pink-100',
        1,
        1
    ),
    (
        2,
        '寻物',
        'Search',
        'text-blue-500',
        'bg-blue-100',
        2,
        1
    ),
    (
        3,
        '闲置',
        'ShoppingBag',
        'text-orange-500',
        'bg-orange-100',
        3,
        1
    ),
    (
        4,
        '吐槽',
        'MessageCircle',
        'text-purple-500',
        'bg-purple-100',
        4,
        1
    ),
    (
        5,
        '其他',
        'LayoutGrid',
        'text-gray-500',
        'bg-gray-100',
        5,
        1
    );

-- 测试帖子数据
INSERT INTO
    post (
        id,
        user_id,
        content,
        category,
        is_anonymous,
        visibility,
        status,
        like_count,
        comment_count
    )
VALUES (
        1,
        2,
        '这是一条测试帖子',
        1,
        0,
        0,
        1,
        5,
        2
    ),
    (
        2,
        2,
        '这是一条匿名帖子',
        1,
        1,
        0,
        1,
        3,
        1
    ),
    (
        3,
        3,
        '仅自己可见的帖子',
        1,
        0,
        2,
        1,
        0,
        0
    ),
    (
        4,
        2,
        '待审核的帖子',
        1,
        0,
        0,
        0,
        0,
        0
    );

-- 测试敏感词数据
INSERT INTO
    sys_sensitive (word)
VALUES ('敏感词1'),
    ('敏感词2'),
    ('测试敏感词');

-- 测试公告数据
INSERT INTO
    sys_announcement (title, content, is_top)
VALUES ('欢迎使用表白墙', '这是一个测试公告', 1);