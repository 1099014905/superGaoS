# 分布式博客系统 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建基于 Spring Cloud Alibaba 的分布式个人博客系统，支持文章管理、图片上传（MinIO）、评论功能、JWT 鉴权。

**Architecture:** 6 个 Maven 模块（common → gateway → blog → comment → file → user），通过 Nacos 注册发现，Gateway 统一路由，OpenFeign 服务间调用，每个服务独立数据库。前后端分离，后端只提供 RESTful API。

**Tech Stack:** Spring Boot 3.2.x + Spring Cloud Alibaba 2023.0.x + Nacos 2.3.x + MyBatis + MySQL 8.0 + MinIO + JJWT + Spring Security

## Global Constraints

- JDK 17 minimum
- Spring Boot 3.2.x + Spring Cloud Alibaba 2023.0.x BOM
- MyBatis（非 MyBatis-Plus）
- MySQL 8.0
- Nacos 2.3.x（单机模式开发）
- MinIO（Docker 部署）
- 所有 API 统一响应格式 `Result<T>`（code/message/data）
- 错误码：Gateway=1xxx, Blog=2xxx, Comment=3xxx, File=4xxx, User=5xxx
- 包路径：`com.supergaos.{module}`
- 端口：gateway=9090, blog=9091, comment=9092, file=9093, user=9094

---

### Task 1: 项目脚手架 — 父 POM + Docker + SQL 初始化

**Files:**
- Create: `pom.xml`（父 POM）
- Create: `docker-compose.yml`
- Create: `sql/init.sql`

**Interfaces:**
- Produces: 父 POM 管理所有依赖版本，Docker Compose 一键启动 MySQL + Nacos + MinIO，SQL 初始化四个数据库的所有表

- [ ] **Step 1: 创建父 POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>superGaoS</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>supergaos-common</module>
        <module>supergaos-gateway</module>
        <module>supergaos-blog</module>
        <module>supergaos-comment</module>
        <module>supergaos-file</module>
        <module>supergaos-user</module>
    </modules>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2023.0.1</spring-cloud.version>
        <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud.alibaba.version>
        <mybatis.version>3.0.3</mybatis.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.mybatis.spring.boot</groupId>
                <artifactId>mybatis-spring-boot-starter</artifactId>
                <version>${mybatis.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: 创建 docker-compose.yml**

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: blog-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
    volumes:
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
      - mysql-data:/var/lib/mysql

  nacos:
    image: nacos/nacos-server:v2.3.2
    container_name: blog-nacos
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone

  minio:
    image: minio/minio
    container_name: blog-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    volumes:
      - minio-data:/data

volumes:
  mysql-data:
  minio-data:
```

- [ ] **Step 3: 创建 sql/init.sql**

```sql
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
```

- [ ] **Step 4: 验证**

Run: `docker-compose up -d`
Expected: MySQL(3306)、Nacos(8848)、MinIO(9000/9001) 三容器正常运行
Run: `docker ps`
Expected: 三个容器状态均为 Up

- [ ] **Step 5: 在 Nacos 配置中心创建 5 个 Data ID**

Nacos 控制台: http://localhost:8848/nacos （默认用户名密码 nacos/nacos）
新建命名空间：`supergaos`（命名空间ID可自动生成）
在每个 Data ID 下创建对应 YAML 配置（后续任务会填充具体内容）：
- `gateway.yml`
- `blog.yml`
- `comment.yml`
- `file.yml`
- `user.yml`

- [ ] **Step 6: 在 MinIO 创建 bucket**

打开 http://localhost:9001 ，用 minioadmin/minioadmin 登录，创建 bucket：`supergaos-images`，将访问策略改为 Public。

- [ ] **Step 7: Commit**

```bash
git add pom.xml docker-compose.yml sql/init.sql
git commit -m "chore: scaffold parent pom, docker-compose, and sql init"
```

---

### Task 2: supergaos-common — 公共模块

**Files:**
- Create: `supergaos-common/pom.xml`
- Create: `supergaos-common/src/main/java/com/supergaos/common/result/Result.java`
- Create: `supergaos-common/src/main/java/com/supergaos/common/exception/BusinessException.java`
- Create: `supergaos-common/src/main/java/com/supergaos/common/constant/ServiceConstant.java`

**Interfaces:**
- Produces: `Result<T>`（code/message/data 三字段统一响应），`BusinessException`（业务异常，含 errorCode），`ServiceConstant`（端口常量）

- [ ] **Step 1: 创建 common/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>superGaoS</artifactId>
        <groupId>org.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>supergaos-common</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 Result.java**

```java
package com.supergaos.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

- [ ] **Step 3: 创建 BusinessException.java**

```java
package com.supergaos.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int errorCode;

    public BusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static BusinessException notFound(int moduleCode, String resource) {
        return new BusinessException(moduleCode * 1000 + 1, resource + "不存在");
    }
}
```

- [ ] **Step 4: 创建 ServiceConstant.java**

```java
package com.supergaos.common.constant;

public interface ServiceConstant {
    int GATEWAY_PORT = 9090;
    int BLOG_PORT = 9091;
    int COMMENT_PORT = 9092;
    int FILE_PORT = 9093;
    int USER_PORT = 9094;

    String SERVICE_NAME_BLOG = "supergaos-blog";
    String SERVICE_NAME_COMMENT = "supergaos-comment";
    String SERVICE_NAME_FILE = "supergaos-file";
    String SERVICE_NAME_USER = "supergaos-user";
}
```

- [ ] **Step 5: Commit**

```bash
git add supergaos-common/
git commit -m "feat: add common module with Result, BusinessException, constants"
```

---

### Task 3: supergaos-gateway — API 网关

**Files:**
- Create: `supergaos-gateway/pom.xml`
- Create: `supergaos-gateway/src/main/java/com/supergaos/gateway/GatewayApplication.java`
- Create: `supergaos-gateway/src/main/java/com/supergaos/gateway/filter/JwtAuthGlobalFilter.java`
- Create: `supergaos-gateway/src/main/java/com/supergaos/gateway/config/GatewayConfig.java`
- Create: `supergaos-gateway/src/main/resources/application.yml`
- Create: `supergaos-gateway/src/main/resources/bootstrap.yml`

**Interfaces:**
- Consumes: `com.supergaos.common.result.Result`
- Produces: JWT 鉴权全局过滤器（白名单路由放行），路由配置（指向各服务）

- [ ] **Step 1: 创建 gateway/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>superGaoS</artifactId>
        <groupId>org.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>supergaos-gateway</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>supergaos-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 9090

spring:
  application:
    name: supergaos-gateway
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        namespace: supergaos
        file-extension: yml
        shared-configs:
          - data-id: gateway.yml
            refresh: true
    gateway:
      routes:
        - id: blog-service
          uri: lb://supergaos-blog
          predicates:
            - Path=/api/blog/**
        - id: comment-service
          uri: lb://supergaos-comment
          predicates:
            - Path=/api/comment/**
        - id: file-service
          uri: lb://supergaos-file
          predicates:
            - Path=/api/file/**
        - id: user-service
          uri: lb://supergaos-user
          predicates:
            - Path=/api/user/**
      default-filters:
        - name: JwtAuth
```

- [ ] **Step 3: 创建 GatewayApplication.java**

```java
package com.supergaos.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 JwtAuthGlobalFilter.java**

```java
package com.supergaos.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supergaos.common.result.Result;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITELIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/blog/articles/**",
            "/api/comment/articles/**",
            "/api/file/**"
    );

    private static final String SECRET = "YourSuperSecretKeyForJWTTokenGeneration2026BlogSystem";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        boolean isWhitelisted = WHITELIST.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (isWhitelisted) {
            return chain.filter(exchange);
        }

        // 非白名单路径需要 JWT
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少 Token");
        }

        String token = authHeader.substring(7);
        try {
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();

            // 将用户信息传递到下游服务
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            return unauthorized(exchange, "Token 无效或已过期");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(Result.error(1001, msg));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
```

- [ ] **Step 5: 验证 gateway 启动**

Run: 先确保 docker-compose 在运行，然后启动 `supergaos-gateway` 模块
Expected: 控制台输出 "Started GatewayApplication"，无报错

- [ ] **Step 6: Commit**

```bash
git add supergaos-gateway/
git commit -m "feat: add api gateway with jwt auth filter"
```

---

### Task 4: supergaos-user — 用户与鉴权服务

**Files:**
- Create: `supergaos-user/pom.xml`
- Create: `supergaos-user/src/main/java/com/supergaos/user/UserApplication.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/entity/User.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/mapper/UserMapper.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/service/UserService.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/controller/UserController.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/dto/LoginDTO.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/dto/RegisterDTO.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/dto/UserVO.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/util/JwtUtil.java`
- Create: `supergaos-user/src/main/java/com/supergaos/user/config/SecurityConfig.java`
- Create: `supergaos-user/src/main/resources/application.yml`
- Create: `supergaos-user/src/main/resources/mapper/UserMapper.xml`

**Interfaces:**
- Consumes: `com.supergaos.common.result.Result`, `com.supergaos.common.exception.BusinessException`
- Produces: `POST /api/user/login` → Token，`POST /api/user/register` → 创建用户

- [ ] **Step 1: 创建 user/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>superGaoS</artifactId>
        <groupId>org.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>supergaos-user</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>supergaos-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 9094

spring:
  application:
    name: supergaos-user
  datasource:
    url: jdbc:mysql://localhost:3306/supergaos_user?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.supergaos.user.entity
```

- [ ] **Step 3: 创建实体类**

```java
package com.supergaos.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4: 创建 UserMapper.java**

```java
package com.supergaos.user.mapper;

import com.supergaos.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    void insert(User user);
}
```

```xml
<!-- src/main/resources/mapper/UserMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.supergaos.user.mapper.UserMapper">
    <resultMap id="baseMap" type="User">
        <id column="id" property="id"/>
        <result column="username" property="username"/>
        <result column="password" property="password"/>
        <result column="nickname" property="nickname"/>
        <result column="avatar" property="avatar"/>
        <result column="create_time" property="createTime"/>
    </resultMap>

    <select id="findByUsername" resultMap="baseMap">
        SELECT * FROM user WHERE username = #{username}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (username, password, nickname) VALUES (#{username}, #{password}, #{nickname})
    </insert>
</mapper>
```

- [ ] **Step 5: 创建 JwtUtil.java**

```java
package com.supergaos.user.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET = "YourSuperSecretKeyForJWTTokenGeneration2026BlogSystem";
    private static final long EXPIRATION = 86400000L; // 24小时
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String generateToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }
}
```

- [ ] **Step 6: 创建 SecurityConfig.java（只保留密码加密，关闭拦截）**

```java
package com.supergaos.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

- [ ] **Step 7: 创建 UserService.java**

```java
package com.supergaos.user.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.user.dto.LoginDTO;
import com.supergaos.user.dto.RegisterDTO;
import com.supergaos.user.dto.UserVO;
import com.supergaos.user.entity.User;
import com.supergaos.user.mapper.UserMapper;
import com.supergaos.user.util.JwtUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserMapper userMapper, JwtUtil jwtUtil, BCryptPasswordEncoder encoder) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.encoder = encoder;
    }

    public String login(LoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw BusinessException.notFound(5, "用户名或密码错误");
        }
        return jwtUtil.generateToken(user.getId());
    }

    public void register(RegisterDTO dto) {
        User existing = userMapper.findByUsername(dto.getUsername());
        if (existing != null) {
            throw new BusinessException(5002, "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        userMapper.insert(user);
    }
}
```

- [ ] **Step 8: 创建 UserController.java**

```java
package com.supergaos.user.controller;

import com.supergaos.common.result.Result;
import com.supergaos.user.dto.LoginDTO;
import com.supergaos.user.dto.RegisterDTO;
import com.supergaos.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto) {
        String token = userService.login(dto);
        return Result.success(token);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }
}
```

- [ ] **Step 9: 验证用户登录**

Run: 启动 `supergaos-user` 模块
Run: `curl -X POST http://localhost:9094/api/user/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'`
Expected: 返回 `{"code":200,"message":"success","data":"eyJ..."}`（JWT Token 字符串）

- [ ] **Step 10: Commit**

```bash
git add supergaos-user/
git commit -m "feat: add user service with login/register and jwt"
```

---

### Task 5: supergaos-comment — 评论服务

**Files:**
- Create: `supergaos-comment/pom.xml`
- Create: `supergaos-comment/src/main/java/com/supergaos/comment/CommentApplication.java`
- Create: `supergaos-comment/src/main/java/com/supergaos/comment/entity/Comment.java`
- Create: `supergaos-comment/src/main/java/com/supergaos/comment/mapper/CommentMapper.java`
- Create: `supergaos-comment/src/main/java/com/supergaos/comment/service/CommentService.java`
- Create: `supergaos-comment/src/main/java/com/supergaos/comment/controller/CommentController.java`
- Create: `supergaos-comment/src/main/java/com/supergaos/comment/dto/CommentVO.java`
- Create: `supergaos-comment/src/main/java/com/supergaos/comment/dto/CommentPageVO.java`
- Create: `supergaos-comment/src/main/resources/application.yml`
- Create: `supergaos-comment/src/main/resources/mapper/CommentMapper.xml`

**Interfaces:**
- Consumes: `com.supergaos.common.result.Result`, `BusinessException`
- Produces: `GET/POST /api/comment/articles/{articleId}` — 评论列表&发表，`GET /api/comment/articles/{articleId}/count` — 评论计数（Feign 用）

- [ ] **Step 1: 创建 comment/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>superGaoS</artifactId>
        <groupId>org.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>supergaos-comment</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>supergaos-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 9092

spring:
  application:
    name: supergaos-comment
  datasource:
    url: jdbc:mysql://localhost:3306/supergaos_comment?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.supergaos.comment.entity
```

- [ ] **Step 3: 创建实体类**

```java
package com.supergaos.comment.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Comment {
    private Long id;
    private Long articleId;
    private Long parentId;
    private String nickname;
    private String email;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4: 创建 CommentVO.java**

```java
package com.supergaos.comment.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long articleId;
    private Long parentId;
    private String nickname;
    private String email;
    private String content;
    private LocalDateTime createTime;
}
```

- [ ] **Step 5: 创建 Mapper**

```java
package com.supergaos.comment.mapper;

import com.supergaos.comment.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> findByArticleId(@Param("articleId") Long articleId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);
    int countByArticleId(@Param("articleId") Long articleId);
    void insert(Comment comment);
    void deleteById(@Param("id") Long id);
}
```

```xml
<!-- src/main/resources/mapper/CommentMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.supergaos.comment.mapper.CommentMapper">
    <resultMap id="baseMap" type="Comment">
        <id column="id" property="id"/>
        <result column="article_id" property="articleId"/>
        <result column="parent_id" property="parentId"/>
        <result column="nickname" property="nickname"/>
        <result column="email" property="email"/>
        <result column="content" property="content"/>
        <result column="status" property="status"/>
        <result column="create_time" property="createTime"/>
    </resultMap>

    <select id="findByArticleId" resultMap="baseMap">
        SELECT * FROM comment WHERE article_id = #{articleId} AND status = 1
        ORDER BY create_time DESC LIMIT #{offset}, #{limit}
    </select>

    <select id="countByArticleId" resultType="int">
        SELECT COUNT(*) FROM comment WHERE article_id = #{articleId} AND status = 1
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO comment (article_id, parent_id, nickname, email, content)
        VALUES (#{articleId}, #{parentId}, #{nickname}, #{email}, #{content})
    </insert>

    <delete id="deleteById">
        DELETE FROM comment WHERE id = #{id}
    </delete>
</mapper>
```

- [ ] **Step 6: 创建 CommentService.java**

```java
package com.supergaos.comment.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.comment.dto.CommentVO;
import com.supergaos.comment.entity.Comment;
import com.supergaos.comment.mapper.CommentMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentMapper commentMapper;

    public CommentService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    public List<CommentVO> getComments(Long articleId, int page, int size) {
        List<Comment> list = commentMapper.findByArticleId(articleId, (page - 1) * size, size);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    public int getCommentCount(Long articleId) {
        return commentMapper.countByArticleId(articleId);
    }

    public CommentVO addComment(Comment comment) {
        commentMapper.insert(comment);
        return toVO(comment);
    }

    public void deleteComment(Long id) {
        commentMapper.deleteById(id);
    }

    private CommentVO toVO(Comment c) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }
}
```

- [ ] **Step 7: 创建 CommentController.java**

```java
package com.supergaos.comment.controller;

import com.supergaos.common.result.Result;
import com.supergaos.comment.dto.CommentVO;
import com.supergaos.comment.entity.Comment;
import com.supergaos.comment.service.CommentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/articles/{articleId}")
    public Result<List<CommentVO>> list(@PathVariable Long articleId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return Result.success(commentService.getComments(articleId, page, size));
    }

    @GetMapping("/articles/{articleId}/count")
    public Result<Integer> count(@PathVariable Long articleId) {
        return Result.success(commentService.getCommentCount(articleId));
    }

    @PostMapping("/articles/{articleId}")
    public Result<CommentVO> add(@PathVariable Long articleId, @RequestBody Comment comment) {
        comment.setArticleId(articleId);
        return Result.success(commentService.addComment(comment));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }
}
```

- [ ] **Step 8: 创建 CommentApplication.java**

```java
package com.supergaos.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CommentApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommentApplication.class, args);
    }
}
```

- [ ] **Step 9: 验证评论服务**

Run: 启动 `supergaos-comment`
Run: `curl http://localhost:9092/api/comment/articles/1/count`
Expected: `{"code":200,"message":"success","data":0}`

- [ ] **Step 10: Commit**

```bash
git add supergaos-comment/
git commit -m "feat: add comment service with crud and count api"
```

---

### Task 6: supergaos-blog — 文章服务

**Files:**
- Create: `supergaos-blog/pom.xml`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/BlogApplication.java`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/entity/{Article,Category,Tag,ArticleCategory,ArticleTag}.java`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/mapper/{ArticleMapper,CategoryMapper,TagMapper}.java`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/service/{ArticleService,CategoryService,TagService}.java`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/controller/{ArticleController,CategoryController,TagController}.java`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/dto/{ArticleVO,ArticlePageVO,ArticleCreateDTO}.java`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/config/MyBatisConfig.java`
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/common/GlobalExceptionHandler.java`
- Create: `supergaos-blog/src/main/resources/application.yml`
- Create: `supergaos-blog/src/main/resources/mapper/{ArticleMapper,CategoryMapper,TagMapper}.xml`

**Interfaces:**
- Consumes: `Result`, `BusinessException`（从 common），`CommentClient`（Feign，Task 8 中定义）
- Produces: 文章 CRUD 接口（含分类和标签）

**Note:** 创建大量文件，每个文件专注于单一职责。

- [ ] **Step 1: 创建 blog/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>superGaoS</artifactId>
        <groupId>org.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>supergaos-blog</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>supergaos-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 9091

spring:
  application:
    name: supergaos-blog
  datasource:
    url: jdbc:mysql://localhost:3306/supergaos_blog?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.supergaos.blog.entity
```

- [ ] **Step 3: 创建实体类**

```java
// Article.java
package com.supergaos.blog.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class Article {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

// Category.java
package com.supergaos.blog.entity;
import lombok.Data;
@Data
public class Category {
    private Long id;
    private String name;
}

// Tag.java
package com.supergaos.blog.entity;
import lombok.Data;
@Data
public class Tag {
    private Long id;
    private String name;
}

// ArticleCategory.java
package com.supergaos.blog.entity;
import lombok.Data;
@Data
public class ArticleCategory {
    private Long articleId;
    private Long categoryId;
}

// ArticleTag.java
package com.supergaos.blog.entity;
import lombok.Data;
@Data
public class ArticleTag {
    private Long articleId;
    private Long tagId;
}
```

- [ ] **Step 4: 创建 Mapper 接口和 XML**

```java
// ArticleMapper.java
package com.supergaos.blog.mapper;
import com.supergaos.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface ArticleMapper {
    List<Article> findAll(@Param("status") Integer status,
                          @Param("offset") int offset,
                          @Param("limit") int limit);
    int countAll(@Param("status") Integer status);
    Article findById(@Param("id") Long id);
    void insert(Article article);
    void update(Article article);
    void deleteById(@Param("id") Long id);
    void insertArticleCategory(@Param("articleId") Long articleId, @Param("categoryId") Long categoryId);
    void insertArticleTag(@Param("articleId") Long articleId, @Param("tagId") Long tagId);
    void deleteArticleCategories(@Param("articleId") Long articleId);
    void deleteArticleTags(@Param("articleId") Long articleId);
}

// CategoryMapper.java
package com.supergaos.blog.mapper;
import com.supergaos.blog.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface CategoryMapper {
    List<Category> findAll();
    Category findById(@Param("id") Long id);
    void insert(Category category);
    List<String> findByArticleId(@Param("articleId") Long articleId);
}

// TagMapper.java
package com.supergaos.blog.mapper;
import com.supergaos.blog.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface TagMapper {
    List<Tag> findAll();
    Tag findById(@Param("id") Long id);
    void insert(Tag tag);
    List<String> findByArticleId(@Param("articleId") Long articleId);
}
```

```xml
<!-- ArticleMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.supergaos.blog.mapper.ArticleMapper">
    <resultMap id="baseMap" type="Article">
        <id column="id" property="id"/>
        <result column="title" property="title"/>
        <result column="content" property="content"/>
        <result column="summary" property="summary"/>
        <result column="status" property="status"/>
        <result column="create_time" property="createTime"/>
        <result column="update_time" property="updateTime"/>
    </resultMap>

    <select id="findAll" resultMap="baseMap">
        SELECT * FROM article
        <where>
            <if test="status != null">status = #{status}</if>
        </where>
        ORDER BY create_time DESC LIMIT #{offset}, #{limit}
    </select>

    <select id="countAll" resultType="int">
        SELECT COUNT(*) FROM article
        <where>
            <if test="status != null">status = #{status}</if>
        </where>
    </select>

    <select id="findById" resultMap="baseMap">
        SELECT * FROM article WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO article (title, content, summary, status)
        VALUES (#{title}, #{content}, #{summary}, #{status})
    </insert>

    <update id="update">
        UPDATE article SET title = #{title}, content = #{content},
        summary = #{summary}, status = #{status}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM article WHERE id = #{id}
    </delete>

    <insert id="insertArticleCategory">
        INSERT INTO article_category (article_id, category_id) VALUES (#{articleId}, #{categoryId})
    </insert>

    <insert id="insertArticleTag">
        INSERT INTO article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})
    </insert>

    <delete id="deleteArticleCategories">
        DELETE FROM article_category WHERE article_id = #{articleId}
    </delete>

    <delete id="deleteArticleTags">
        DELETE FROM article_tag WHERE article_id = #{articleId}
    </delete>
</mapper>
```

```xml
<!-- CategoryMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.supergaos.blog.mapper.CategoryMapper">
    <resultMap id="baseMap" type="Category">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
    </resultMap>

    <select id="findAll" resultMap="baseMap">
        SELECT * FROM category
    </select>

    <select id="findById" resultMap="baseMap">
        SELECT * FROM category WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO category (name) VALUES (#{name})
    </insert>

    <select id="findByArticleId" resultType="string">
        SELECT c.name FROM category c JOIN article_category ac ON c.id = ac.category_id
        WHERE ac.article_id = #{articleId}
    </select>
</mapper>
```

```xml
<!-- TagMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.supergaos.blog.mapper.TagMapper">
    <resultMap id="baseMap" type="Tag">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
    </resultMap>

    <select id="findAll" resultMap="baseMap">
        SELECT * FROM tag
    </select>

    <select id="findById" resultMap="baseMap">
        SELECT * FROM tag WHERE id = #{id}
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO tag (name) VALUES (#{name})
    </insert>

    <select id="findByArticleId" resultType="string">
        SELECT t.name FROM tag t JOIN article_tag at ON t.id = at.tag_id
        WHERE at.article_id = #{articleId}
    </select>
</mapper>
```

- [ ] **Step 5: 创建 DTO 类**

```java
// ArticleVO.java
package com.supergaos.blog.dto;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Integer status;
    private List<String> categories;
    private List<String> tags;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

// ArticlePageVO.java
package com.supergaos.blog.dto;
import lombok.Data;
import java.util.List;
@Data
public class ArticlePageVO {
    private List<ArticleVO> articles;
    private int total;
    private int page;
    private int size;
}

// ArticleCreateDTO.java
package com.supergaos.blog.dto;
import lombok.Data;
import java.util.List;
@Data
public class ArticleCreateDTO {
    private String title;
    private String content;
    private String summary;
    private Integer status;
    private List<Long> categoryIds;
    private List<Long> tagIds;
}
```

- [ ] **Step 6: 创建 service 层**

```java
// ArticleService.java
package com.supergaos.blog.service;

import com.supergaos.blog.dto.ArticleCreateDTO;
import com.supergaos.blog.dto.ArticlePageVO;
import com.supergaos.blog.dto.ArticleVO;
import com.supergaos.blog.entity.*;
import com.supergaos.blog.mapper.*;
import com.supergaos.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public ArticleService(ArticleMapper articleMapper, CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    public ArticlePageVO list(Integer status, int page, int size) {
        ArticlePageVO result = new ArticlePageVO();
        result.setTotal(articleMapper.countAll(status));
        result.setPage(page);
        result.setSize(size);

        List<Article> articles = articleMapper.findAll(status, (page - 1) * size, size);
        result.setArticles(articles.stream().map(a -> {
            ArticleVO vo = toVO(a);
            vo.setCategories(categoryMapper.findByArticleId(a.getId()));
            vo.setTags(tagMapper.findByArticleId(a.getId()));
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    public ArticleVO getById(Long id) {
        Article article = articleMapper.findById(id);
        if (article == null) throw BusinessException.notFound(2, "文章");
        ArticleVO vo = toVO(article);
        vo.setCategories(categoryMapper.findByArticleId(id));
        vo.setTags(tagMapper.findByArticleId(id));
        return vo;
    }

    @Transactional
    public ArticleVO create(ArticleCreateDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        articleMapper.insert(article);

        if (dto.getCategoryIds() != null) {
            dto.getCategoryIds().forEach(cid -> articleMapper.insertArticleCategory(article.getId(), cid));
        }
        if (dto.getTagIds() != null) {
            dto.getTagIds().forEach(tid -> articleMapper.insertArticleTag(article.getId(), tid));
        }
        return getById(article.getId());
    }

    @Transactional
    public ArticleVO update(Long id, ArticleCreateDTO dto) {
        Article article = articleMapper.findById(id);
        if (article == null) throw BusinessException.notFound(2, "文章");
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setSummary(dto.getSummary());
        article.setStatus(dto.getStatus() != null ? dto.getStatus() : article.getStatus());
        articleMapper.update(article);

        // 简单处理：先删后插关联
        articleMapper.deleteArticleCategories(id);
        articleMapper.deleteArticleTags(id);
        if (dto.getCategoryIds() != null) {
            dto.getCategoryIds().forEach(cid -> articleMapper.insertArticleCategory(id, cid));
        }
        if (dto.getTagIds() != null) {
            dto.getTagIds().forEach(tid -> articleMapper.insertArticleTag(id, tid));
        }
        return getById(id);
    }

    @Transactional
    public void delete(Long id) {
        Article article = articleMapper.findById(id);
        if (article == null) throw BusinessException.notFound(2, "文章");
        articleMapper.deleteArticleCategories(id);
        articleMapper.deleteArticleTags(id);
        articleMapper.deleteById(id);
    }

    private ArticleVO toVO(Article a) {
        ArticleVO vo = new ArticleVO();
        org.springframework.beans.BeanUtils.copyProperties(a, vo);
        return vo;
    }
}
```

```java
// CategoryService.java
package com.supergaos.blog.service;
import com.supergaos.blog.entity.Category;
import com.supergaos.blog.mapper.CategoryMapper;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;
    public CategoryService(CategoryMapper categoryMapper) { this.categoryMapper = categoryMapper; }
    public List<Category> findAll() { return categoryMapper.findAll(); }
    public Category create(Category category) { categoryMapper.insert(category); return category; }
}
```

```java
// TagService.java
package com.supergaos.blog.service;
import com.supergaos.blog.entity.Tag;
import com.supergaos.blog.mapper.TagMapper;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class TagService {
    private final TagMapper tagMapper;
    public TagService(TagMapper tagMapper) { this.tagMapper = tagMapper; }
    public List<Tag> findAll() { return tagMapper.findAll(); }
    public Tag create(Tag tag) { tagMapper.insert(tag); return tag; }
}
```

- [ ] **Step 7: 创建 Controller**

```java
// ArticleController.java
package com.supergaos.blog.controller;
import com.supergaos.blog.dto.ArticleCreateDTO;
import com.supergaos.blog.dto.ArticlePageVO;
import com.supergaos.blog.dto.ArticleVO;
import com.supergaos.blog.service.ArticleService;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/blog/articles")
public class ArticleController {
    private final ArticleService articleService;
    public ArticleController(ArticleService articleService) { this.articleService = articleService; }

    @GetMapping
    public Result<ArticlePageVO> list(@RequestParam(required = false) Integer status,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(articleService.list(status, page, size));
    }

    @GetMapping("/{id}")
    public Result<ArticleVO> get(@PathVariable Long id) {
        return Result.success(articleService.getById(id));
    }

    @PostMapping
    public Result<ArticleVO> create(@RequestBody ArticleCreateDTO dto) {
        return Result.success(articleService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<ArticleVO> update(@PathVariable Long id, @RequestBody ArticleCreateDTO dto) {
        return Result.success(articleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }
}
```

```java
// CategoryController.java
package com.supergaos.blog.controller;
import com.supergaos.blog.entity.Category;
import com.supergaos.blog.service.CategoryService;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/blog/categories")
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) { this.categoryService = categoryService; }
    @GetMapping
    public Result<List<Category>> list() { return Result.success(categoryService.findAll()); }
    @PostMapping
    public Result<Category> create(@RequestBody Category category) { return Result.success(categoryService.create(category)); }
}
```

```java
// TagController.java
package com.supergaos.blog.controller;
import com.supergaos.blog.entity.Tag;
import com.supergaos.blog.service.TagService;
import com.supergaos.common.result.Result;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/blog/tags")
public class TagController {
    private final TagService tagService;
    public TagController(TagService tagService) { this.tagService = tagService; }
    @GetMapping
    public Result<List<Tag>> list() { return Result.success(tagService.findAll()); }
    @PostMapping
    public Result<Tag> create(@RequestBody Tag tag) { return Result.success(tagService.create(tag)); }
}
```

- [ ] **Step 8: 创建 BlogApplication.java**

```java
package com.supergaos.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
```

- [ ] **Step 9: 验证博客服务**

Run: 启动 `supergaos-blog`
Run: `curl http://localhost:9091/api/blog/articles`
Expected: `{"code":200,"message":"success","data":{"articles":[],"total":0,"page":1,"size":10}}`

- [ ] **Step 10: Commit**

```bash
git add supergaos-blog/
git commit -m "feat: add blog service with article, category, tag crud"
```

---

### Task 7: supergaos-file — 文件服务（MinIO）

**Files:**
- Create: `supergaos-file/pom.xml`
- Create: `supergaos-file/src/main/java/com/supergaos/file/FileApplication.java`
- Create: `supergaos-file/src/main/java/com/supergaos/file/entity/FileRecord.java`
- Create: `supergaos-file/src/main/java/com/supergaos/file/mapper/FileRecordMapper.java`
- Create: `supergaos-file/src/main/java/com/supergaos/file/service/FileService.java`
- Create: `supergaos-file/src/main/java/com/supergaos/file/controller/FileController.java`
- Create: `supergaos-file/src/main/java/com/supergaos/file/config/MinioConfig.java`
- Create: `supergaos-file/src/main/resources/application.yml`
- Create: `supergaos-file/src/main/resources/mapper/FileRecordMapper.xml`

**Interfaces:**
- Consumes: `Result`, `BusinessException`
- Produces: `POST /api/file/upload`（上传返回 URL），`DELETE /api/file/{id}`（删除文件）

- [ ] **Step 1: 创建 file/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>superGaoS</artifactId>
        <groupId>org.example</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>supergaos-file</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.5.10</version>
        </dependency>
        <dependency>
            <groupId>org.example</groupId>
            <artifactId>supergaos-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
server:
  port: 9093

spring:
  application:
    name: supergaos-file
  datasource:
    url: jdbc:mysql://localhost:3306/supergaos_file?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
  servlet:
    multipart:
      max-file-size: 10MB

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.supergaos.file.entity

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: supergaos-images
```

- [ ] **Step 3: 创建 MinioConfig.java**

```java
package com.supergaos.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

- [ ] **Step 4: 创建实体和 Mapper**

```java
// FileRecord.java
package com.supergaos.file.entity;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class FileRecord {
    private Long id;
    private String originalName;
    private String storagePath;
    private String url;
    private Long fileSize;
    private String mimeType;
    private Long articleId;
    private LocalDateTime createTime;
}
```

```java
// FileRecordMapper.java
package com.supergaos.file.mapper;
import com.supergaos.file.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface FileRecordMapper {
    void insert(FileRecord record);
    FileRecord findById(@Param("id") Long id);
    void deleteById(@Param("id") Long id);
}
```

```xml
<!-- FileRecordMapper.xml -->
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.supergaos.file.mapper.FileRecordMapper">
    <resultMap id="baseMap" type="FileRecord">
        <id column="id" property="id"/>
        <result column="original_name" property="originalName"/>
        <result column="storage_path" property="storagePath"/>
        <result column="url" property="url"/>
        <result column="file_size" property="fileSize"/>
        <result column="mime_type" property="mimeType"/>
        <result column="article_id" property="articleId"/>
        <result column="create_time" property="createTime"/>
    </resultMap>
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO file_record (original_name, storage_path, url, file_size, mime_type, article_id)
        VALUES (#{originalName}, #{storagePath}, #{url}, #{fileSize}, #{mimeType}, #{articleId})
    </insert>
    <select id="findById" resultMap="baseMap">
        SELECT * FROM file_record WHERE id = #{id}
    </select>
    <delete id="deleteById">
        DELETE FROM file_record WHERE id = #{id}
    </delete>
</mapper>
```

- [ ] **Step 5: 创建 FileService.java**

```java
package com.supergaos.file.service;

import com.supergaos.common.exception.BusinessException;
import com.supergaos.file.entity.FileRecord;
import com.supergaos.file.mapper.FileRecordMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Service
public class FileService {
    private final MinioClient minioClient;
    private final FileRecordMapper fileRecordMapper;

    @Value("${minio.bucket}")
    private String bucket;

    public FileService(MinioClient minioClient, FileRecordMapper fileRecordMapper) {
        this.minioClient = minioClient;
        this.fileRecordMapper = fileRecordMapper;
    }

    public FileRecord upload(MultipartFile file, Long articleId) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf(".")) : "";
            String storagePath = UUID.randomUUID() + ext;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String url = "http://localhost:9000/" + bucket + "/" + storagePath;

            FileRecord record = new FileRecord();
            record.setOriginalName(originalName);
            record.setStoragePath(storagePath);
            record.setUrl(url);
            record.setFileSize(file.getSize());
            record.setMimeType(file.getContentType());
            record.setArticleId(articleId);
            fileRecordMapper.insert(record);
            return record;
        } catch (Exception e) {
            throw new BusinessException(4001, "文件上传失败: " + e.getMessage());
        }
    }

    public void delete(Long id) {
        FileRecord record = fileRecordMapper.findById(id);
        if (record == null) throw BusinessException.notFound(4, "文件");
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(record.getStoragePath())
                    .build());
        } catch (Exception ignored) {}
        fileRecordMapper.deleteById(id);
    }
}
```

- [ ] **Step 6: 创建 FileController.java**

```java
package com.supergaos.file.controller;

import com.supergaos.common.result.Result;
import com.supergaos.file.entity.FileRecord;
import com.supergaos.file.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private final FileService fileService;
    public FileController(FileService fileService) { this.fileService = fileService; }

    @PostMapping("/upload")
    public Result<FileRecord> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam(required = false) Long articleId) {
        return Result.success(fileService.upload(file, articleId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return Result.success();
    }
}
```

- [ ] **Step 7: 验证文件上传**

Run: 确保 MinIO 正常运行且有 `supergaos-images` bucket
Run: 启动 `supergaos-file`
Run: `curl -X POST http://localhost:9093/api/file/upload -F "file=@test.png"`
Expected: 返回包含 `url` 字段的 JSON，url 可访问

- [ ] **Step 8: Commit**

```bash
git add supergaos-file/
git commit -m "feat: add file service with minio upload"
```

---

### Task 8: Blog ← Comment Feign 集成 + Gateway 全链路验证

**Files:**
- Modify: `BlogApplication.java`（添加 `@EnableFeignClients`）
- Create: `supergaos-blog/src/main/java/com/supergaos/blog/feign/CommentClient.java`
- Modify: `ArticleService.java`（文章详情报文时填充 commentCount）

**Interfaces:**
- Consumes: Comment Service 的 `GET /api/comment/articles/{articleId}/count`
- Produces: `ArticleVO` 中携带 `commentCount` 字段

- [ ] **Step 1: 在 BlogApplication 上添加 Feign 注解**

```java
package com.supergaos.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
```

- [ ] **Step 2: 创建 CommentClient.java**

```java
package com.supergaos.blog.feign;

import com.supergaos.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "supergaos-comment")
public interface CommentClient {
    @GetMapping("/api/comment/articles/{articleId}/count")
    Result<Integer> getCommentCount(@PathVariable("articleId") Long articleId);
}
```

- [ ] **Step 3: 修改 ArticleService 注入 Feign 并填充 commentCount**

```java
// 在 ArticleService.java 中添加 Feign 调用
// 注入：
private final CommentClient commentClient;
public ArticleService(ArticleMapper articleMapper, CategoryMapper categoryMapper,
                      TagMapper tagMapper, CommentClient commentClient) {
    this.articleMapper = articleMapper;
    this.categoryMapper = categoryMapper;
    this.tagMapper = tagMapper;
    this.commentClient = commentClient;
}

// 在 toVO 或 getById 中填充 commentCount：
public ArticleVO getById(Long id) {
    Article article = articleMapper.findById(id);
    if (article == null) throw BusinessException.notFound(2, "文章");
    ArticleVO vo = toVO(article);
    vo.setCategories(categoryMapper.findByArticleId(id));
    vo.setTags(tagMapper.findByArticleId(id));
    // Feign 调用 Comment Service 获取评论数
    try {
        Result<Integer> countResult = commentClient.getCommentCount(id);
        if (countResult != null && countResult.getData() != null) {
            vo.setCommentCount(countResult.getData());
        }
    } catch (Exception e) {
        vo.setCommentCount(0); // 评论服务挂了不影响文章展示
    }
    return vo;
}
```

- [ ] **Step 4: 全链路验证**

Run: 确保 docker-compose 正常运行（MySQL、Nacos、MinIO）
Run: 按顺序启动所有服务：Gateway(9090) → User(9094) → Comment(9092) → Blog(9091) → File(9093)

**验证步骤 1 — 登录：**
Run: `curl -X POST http://localhost:9090/api/user/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'`
Expected: 返回 Token

**验证步骤 2 — 创建文章：**
Run: `curl -X POST http://localhost:9090/api/blog/articles -H "Content-Type: application/json" -H "Authorization: Bearer {TOKEN}" -d '{"title":"Hello","content":"# Hello World","status":2}'`
Expected: 返回文章对象

**验证步骤 3 — 查看文章（含评论数）：**
Run: `curl http://localhost:9090/api/blog/articles/1`
Expected: `{"code":200,"data":{"title":"Hello","commentCount":0,...}}`

**验证步骤 4 — 发表评论：**
Run: `curl -X POST http://localhost:9090/api/comment/articles/1 -H "Content-Type: application/json" -d '{"nickname":"匿名","content":"好文章！"}'`
Expected: 返回评论对象

**验证步骤 5 — 无 Token 访问受限接口：**
Run: `curl -X POST http://localhost:9090/api/blog/articles -H "Content-Type: application/json" -d '{}'`
Expected: `{"code":1001,"message":"缺少 Token"}`

**验证步骤 6 — 上传图片：**
Run: `curl -X POST http://localhost:9090/api/file/upload -F "file=@test.png"`
Expected: 返回文件 URL

- [ ] **Step 5: Commit**

```bash
git add supergaos-blog/src/main/java/com/supergaos/blog/
git commit -m "feat: integrate blog-comment via feign and full chain verification"
```
