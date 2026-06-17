# 分布式博客系统设计文档

> 日期: 2026-06-17
> 状态: 设计稿

---

## 1. 项目概述

基于 Spring Cloud Alibaba 技术栈的分布式个人博客系统，支持文章管理、图片上传、评论功能、前后端分离。

## 2. 架构设计

### 2.1 整体架构图

```
                       ┌─────────────┐
                       │  前端 (Vue)   │
                       └──────┬──────┘
                              │ HTTP
                      ┌───────┴───────┐
                      │  API Gateway   │  ← Spring Cloud Gateway
                      │  (路由/鉴权)    │
                      └───┬───┬───┬───┘
                          │   │   │
              ┌───────────┘   │   └───────────┐
              ▼               ▼               ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │  Blog    │   │ Comment  │   │  File    │
        │ Service  │   │ Service  │   │ Service  │
        └────┬─────┘   └────┬─────┘   └────┬─────┘
             │              │              │
             ▼              ▼              ▼
        ┌──────────┐   ┌──────────┐   ┌──────────┐
        │   MySQL  │   │   MySQL  │   │  MinIO   │
        │  blog    │   │ comment  │   │  (OSS)   │
        └──────────┘   └──────────┘   └──────────┘

              ┌──────────────┐
              │ User Service │
              │  (login)     │
              └──────┬───────┘
                     ▼
              ┌──────────┐
              │   MySQL  │
              │   user   │
              └──────────┘
```

### 2.2 服务列表

| 服务模块 | 端口 | 说明 |
|---------|------|------|
| supergaos-common | - | 公共依赖包（DTO、工具类、统一响应体） |
| supergaos-gateway | 9090 | Spring Cloud Gateway 网关 |
| supergaos-blog | 9091 | 文章管理（CRUD、分类、标签） |
| supergaos-comment | 9092 | 评论管理 |
| supergaos-file | 9093 | 文件上传（对接 MinIO） |
| supergaos-user | 9094 | 用户/登录（JWT 鉴权） |

## 3. 技术栈

| 组件 | 选型 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.2.x |
| 微服务 | Spring Cloud Alibaba | 2023.0.x |
| 注册/配置中心 | Nacos | 2.3.x |
| 网关 | Spring Cloud Gateway | - |
| 服务调用 | OpenFeign | - |
| 负载均衡 | Spring Cloud LoadBalancer | - |
| 限流/熔断 | Sentinel | - |
| 数据库 | MySQL | 8.0+ |
| ORM | MyBatis | - |
| 云存储 | MinIO | - |
| 鉴权 | JWT + Spring Security | - |
| 前端 | Vue 3 | 3.4+ |
| JDK | JDK 17 | 17 LTS |

## 4. 数据库设计

### 4.1 supergaos_blog（博客库）

```sql
CREATE TABLE article (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     LONGTEXT,
    summary     VARCHAR(500),
    status      TINYINT DEFAULT 1 COMMENT '1:草稿 2:已发布',
    create_time DATETIME,
    update_time DATETIME
);

CREATE TABLE category (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE article_category (
    article_id  BIGINT,
    category_id BIGINT
);

CREATE TABLE tag (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE article_tag (
    article_id BIGINT,
    tag_id     BIGINT
);
```

### 4.2 supergaos_comment（评论库）

```sql
CREATE TABLE comment (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id   BIGINT NOT NULL,
    parent_id    BIGINT DEFAULT NULL COMMENT '回复某条评论',
    nickname     VARCHAR(100),
    email        VARCHAR(200),
    content      TEXT NOT NULL,
    status       TINYINT DEFAULT 1 COMMENT '1:显示 0:隐藏',
    create_time  DATETIME
);
```

### 4.3 supergaos_file（文件库）

```sql
CREATE TABLE file_record (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_name  VARCHAR(255),
    storage_path   VARCHAR(500),
    url            VARCHAR(500),
    file_size      BIGINT,
    mime_type      VARCHAR(100),
    article_id     BIGINT DEFAULT NULL,
    create_time    DATETIME
);
```

### 4.4 supergaos_user（用户库）

```sql
CREATE TABLE user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(100),
    avatar      VARCHAR(500),
    create_time DATETIME
);
```

## 5. API 接口

所有请求统一走 Gateway（`:9090`）。

### 博客服务

```
GET    /api/blog/articles             → 文章列表（分页）
GET    /api/blog/articles/{id}        → 文章详情
POST   /api/blog/articles             → 创建文章
PUT    /api/blog/articles/{id}        → 更新文章
DELETE /api/blog/articles/{id}        → 删除文章
GET    /api/blog/categories           → 分类列表
POST   /api/blog/categories           → 创建分类
GET    /api/blog/tags                 → 标签列表
```

### 评论服务

```
GET    /api/comment/articles/{articleId}        → 获取评论列表
GET    /api/comment/articles/{articleId}/count  → 获取评论数(Feign 调用)
POST   /api/comment/articles/{articleId}        → 发表评论
DELETE /api/comment/{id}                        → 删除评论
```

### 文件服务

```
POST   /api/file/upload     → 上传图片
DELETE /api/file/{id}       → 删除文件
```

### 用户服务

```
POST   /api/user/login      → 登录（返回 JWT）
POST   /api/user/register   → 注册
```

## 6. 统一响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": { ... }
}
```

### 错误码约定

| 范围 | 服务 | 示例 |
|------|------|------|
| 1xxx | Gateway | 1001 = Token 无效 |
| 2xxx | Blog | 2001 = 文章不存在 |
| 3xxx | Comment | 3001 = 评论不存在 |
| 4xxx | File | 4001 = 文件上传失败 |
| 5xxx | User | 5001 = 用户名密码错误 |

## 7. 项目目录结构

```
superGaoS/
├── pom.xml
├── supergaos-common/
│   └── src/main/java/com/supergaos/common/
│       ├── result/Result.java
│       ├── exception/BusinessException.java
│       └── constant/ServiceConstant.java
├── supergaos-gateway/
│   └── src/main/java/com/supergaos/gateway/
│       └── GatewayApplication.java
├── supergaos-blog/
│   └── src/main/java/com/supergaos/blog/
│       ├── BlogApplication.java
│       ├── controller/
│       ├── service/
│       ├── mapper/
│       ├── entity/
│       ├── dto/
│       ├── config/
│       ├── feign/CommentClient.java
│       └── common/GlobalExceptionHandler.java
├── supergaos-comment/      (同上结构)
├── supergaos-file/         (同上结构)
└── supergaos-user/         (同上结构)
```

## 8. Nacos 配置中心

命名空间: `supergaos`

| Data ID | 服务 |
|---------|------|
| gateway.yml | 路由配置 |
| blog.yml | 博客服务 |
| comment.yml | 评论服务 |
| file.yml | 文件服务 |
| user.yml | 用户服务 |

## 9. 本地开发环境

### Docker Compose

```yaml
services:
  mysql:    # 8.0, port 3306
  nacos:    # 2.3.x, port 8848, MODE=standalone
  minio:    # port 9000/9001, bucket: supergaos-images
```

### 启动顺序

1. `docker-compose up -d`
2. Nacos 配置中心新建 Data ID
3. 启动 Gateway → Blog → Comment → File → User

## 10. 鉴权策略

- 文章发布/编辑/删除 → 需 JWT Token
- 文章浏览 → 无需登录
- 评论发表 → 游客可评论（留昵称、邮箱）
- 评论删除 → 仅管理员
