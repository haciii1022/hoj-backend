# 数据库初始化
# @author <a href="https://github.com/liyupi">程序员鱼皮</a>
# @from <a href="https://yupi.icu">编程导航知识星球</a>

-- 创建库
create database if not exists hoj;

-- 切换库
use hoj;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    unionId      varchar(256)                           null comment '微信开放平台id',
    mpOpenId     varchar(256)                           null comment '公众号openId',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_unionId (unionId)
) comment '用户' collate = utf8mb4_general_ci;

-- 题目表
create table if not exists question
(
    id          bigint auto_increment comment 'id' primary key,
    title       varchar(512)                       null comment '标题',
    content     text                               null comment '内容',
    tags        varchar(1024)                      null comment '标签列表（json 数组）',
    answer      text                               null comment '答案',
    submitNum   int      default 0                 not null comment '题目提交数',
    acceptedNum int      default 0                 not null comment '题目通过数',
    judgeCase   text                               null comment '判题用例（json数组）',
    judgeConfig text                               null comment '判题配置（json 对象）',
    thumbNum    int      default 0                 not null comment '点赞数',
    favourNum   int      default 0                 not null comment '收藏数',
    userId      bigint                             not null comment '创建用户 id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '题目' collate = utf8mb4_general_ci;

-- 题目提交表
create table if not exists question_submit
(
    id         bigint auto_increment comment 'id' primary key,
    language   varchar(128)                       not null comment '编程语言',
    code       text                               not null comment '用户代码',
    judgeInfo  text                               null comment '判题信息（json 对象）',
    status     int      default 0                 not null comment '判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）',
    questionId bigint                             not null comment '题目 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_questionId (questionId),
    index idx_userId (userId)
) comment '题目提交' collate = utf8mb4_general_ci;



-- 帖子表
create table if not exists post
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(512)                       null comment '标题',
    content    text                               null comment '内容',
    tags       varchar(1024)                      null comment '标签列表（json 数组）',
    thumbNum   int      default 0                 not null comment '点赞数',
    favourNum  int      default 0                 not null comment '收藏数',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '帖子' collate = utf8mb4_general_ci;

-- 帖子点赞表（硬删除）
create table if not exists post_thumb
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子点赞' collate = utf8mb4_general_ci;

-- 帖子收藏表（硬删除）
create table if not exists post_favour
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
) comment '帖子收藏' collate = utf8mb4_general_ci;


CREATE TABLE `sequence` (
  `name` VARCHAR(50) NOT NULL,
  `value` BIGINT NOT NULL,
  `increment_step` INT NOT NULL DEFAULT 1,
  PRIMARY KEY (`name`)
)comment '序列表' collate = utf8mb4_general_ci;

CREATE PROCEDURE `nextval`(IN seq_name varchar(50), OUT next_value bigint)
BEGIN

  -- 启动事务
  START TRANSACTION;

  -- 更新序列值
  UPDATE `sequence`
  SET `value` = `value` + `increment_step`
  WHERE `name` = seq_name;

  -- 获取更新后的值
  SELECT `value` INTO next_value
  FROM `sequence`
  WHERE `name` = seq_name;

  -- 提交事务
  COMMIT;
end;


CREATE PROCEDURE `curval`(IN seq_name varchar(50), OUT cur_value bigint)
BEGIN


  -- 获取更新后的值
  SELECT `value` + `increment_step` INTO cur_value
  FROM `sequence`
  WHERE `name` = seq_name;

end;



create table if not exists judge_case_group
(
    id           bigint auto_increment comment 'id' primary key,
    questionId   bigint                                 not null comment '题目 id',
    userId       bigint                                 not null comment '创建用户 id',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_questionId (questionId),
    index idx_userId (userId)
) comment '测试用例组' collate = utf8mb4_general_ci;


create table if not exists judge_case_file
(
    id           bigint auto_increment comment 'id' primary key,
    groupId      bigint                                 not null comment '测试用例组 id',
    url          varchar(256)                           null comment '文件url',
    type         tinyint                                not null comment '0:in 1:out',
    fileName     varchar(128)                           null comment '文件名',
    fileFolder   varchar(128)                           null comment '文件夹位置',
    userId       bigint                                 not null comment '创建用户 id',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '测试用例文件' collate = utf8mb4_general_ci;

INSERT INTO sequence (name, value, increment_step)VALUES ('question_id', 1, 1) ON DUPLICATE KEY UPDATE increment_step = VALUES(increment_step);
INSERT INTO sequence (name, value, increment_step)VALUES ('user_id', 1, 1) ON DUPLICATE KEY UPDATE increment_step = VALUES(increment_step);
INSERT INTO sequence (name, value, increment_step)VALUES ('question_submit_id', 1, 1) ON DUPLICATE KEY UPDATE increment_step = VALUES(increment_step);
INSERT INTO sequence (name, value, increment_step)VALUES ('judge_case_group_id', 1, 1) ON DUPLICATE KEY UPDATE increment_step = VALUES(increment_step);
INSERT INTO sequence (name, value, increment_step)VALUES ('judge_case_file_id', 1, 1) ON DUPLICATE KEY UPDATE increment_step = VALUES(increment_step);
