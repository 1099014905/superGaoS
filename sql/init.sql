-- 创建四个数据库
CREATE DATABASE IF NOT EXISTS supergaos_blog DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS supergaos_comment DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS supergaos_file DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS supergaos_user DEFAULT CHARACTER SET utf8mb4;

USE supergaos_blog;
CREATE TABLE article (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     LONGTEXT,
    summary     VARCHAR(500),
    status      TINYINT DEFAULT 1 COMMENT '1:草稿 2:已发布',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE category (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE article_category (
    article_id BIGINT,
    category_id BIGINT,
    PRIMARY KEY (article_id, category_id)
);
CREATE TABLE tag (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE article_tag (
    article_id BIGINT,
    tag_id     BIGINT,
    PRIMARY KEY (article_id, tag_id)
);

USE supergaos_comment;
CREATE TABLE comment (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id  BIGINT NOT NULL,
    parent_id   BIGINT DEFAULT NULL COMMENT '回复的评论ID',
    nickname    VARCHAR(100),
    email       VARCHAR(200),
    content     TEXT NOT NULL,
    status      TINYINT DEFAULT 1 COMMENT '1:显示 0:隐藏',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

USE supergaos_file;
CREATE TABLE file_record (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_name  VARCHAR(255),
    storage_path   VARCHAR(500),
    url            VARCHAR(500),
    file_size      BIGINT,
    mime_type      VARCHAR(100),
    article_id     BIGINT DEFAULT NULL,
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP
);

USE supergaos_user;
CREATE TABLE user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(100),
    avatar      VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- 默认管理员，密码 admin123（BCrypt）
INSERT INTO user (username, password, nickname) VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员');
