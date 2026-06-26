# 秒杀功能设计

## 概述

博客系统新增秒杀模块，支持管理员创建秒杀活动，用户高并发抢购虚拟商品。

## 架构

### 新增模块

`supergaos-seckill` — 秒杀微服务，端口 9095，Nacos 服务名 `supergaos-seckill`

### 新增基础设施

| 中间件 | 用途 |
|--------|------|
| Redis | 库存原子扣减（Lua 脚本）、用户去重、令牌桶限流 |
| RabbitMQ | 秒杀成功消息异步落库，削峰填谷 |

### 调用链路

```
用户抢购 → nginx → 网关 /api/seckill/** → lb://supergaos-seckill
         → SeckillController → SeckillService
            1. Redis Lua 脚本原子扣库存 + 去重
            2. 成功 → 发 MQ 消息 → 消费者落库创建订单
            3. 失败 → 返回具体原因
```

### 项目结构

```
supergaos-seckill/src/main/java/com/supergaos/seckill/
├── SeckillApplication.java
├── config/
│   ├── RedisConfig.java
│   └── RabbitMQConfig.java
├── controller/
│   └── SeckillController.java
├── service/
│   └── SeckillService.java
├── consumer/
│   └── OrderConsumer.java         # MQ 消费者，异步落单
├── entity/
│   ├── SeckillActivity.java
│   └── SeckillOrder.java
├── dto/
│   ├── SeckillActivityVO.java    # 前端展示用
│   └── SeckillOrderVO.java
├── mapper/
│   ├── SeckillActivityMapper.java
│   ├── SeckillOrderMapper.java
│   └── mapper/SeckillMapper.xml
└── common/
    └── GlobalExceptionHandler.java
```

## 数据库

```sql
CREATE DATABASE IF NOT EXISTS supergaos_seckill DEFAULT CHARACTER SET utf8mb4;

USE supergaos_seckill;

CREATE TABLE seckill_activity (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(200) NOT NULL COMMENT '活动标题',
    price        DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock        INT NOT NULL DEFAULT 0 COMMENT '总库存',
    start_time   DATETIME NOT NULL COMMENT '开始时间',
    end_time     DATETIME NOT NULL COMMENT '结束时间',
    status       TINYINT DEFAULT 0 COMMENT '0:未开始 1:进行中 2:已结束',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seckill_order (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id   BIGINT NOT NULL COMMENT '活动ID',
    user_id       BIGINT NOT NULL COMMENT '用户ID',
    amount        DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    status        TINYINT DEFAULT 0 COMMENT '0:待支付 1:已支付 2:已取消',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_user (activity_id, user_id)
);
```

## API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/seckill/list` | 活动列表 | 无 |
| GET | `/api/seckill/{id}` | 活动详情 | 无 |
| POST | `/api/seckill/{id}/grab` | 发起抢购 | JWT |
| GET | `/api/seckill/orders` | 我的订单 | JWT |

### 抢购核心流程

```
POST /api/seckill/{id}/grab
Authorization: Bearer <token>

1. SeckillService.grab(activityId, userId)
2. Redis Lua 脚本原子操作：
   - SISMEMBER 检查用户是否已抢
   - GET + DECR 库存
   - SADD 记录用户
3. 成功 → 发 MQ → 返回 "抢购成功，订单处理中"
4. 失败 → 返回具体原因（重复/售罄/未开始）

MQ 消费者 OrderConsumer：
   1. 扣减 MySQL 库存
   2. INSERT seckill_order
   3. 记录结果（用户通过订单列表查询）
```

## Redis Lua 脚本

```lua
-- KEYS[1] = seckill:stock:{activityId}
-- KEYS[2] = seckill:users:{activityId}
-- ARGV[1] = userId

local has = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if has == 1 then return -1 end

local stock = redis.call('GET', KEYS[1])
if not stock or tonumber(stock) <= 0 then return -2 end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 1
```

## 前端

### 新增页面

| 路由 | 页面 | 说明 |
|------|------|------|
| `/seckill` | 秒杀列表页 | 展示活动列表 + 倒计时 + 抢购按钮 |
| `/seckill/orders` | 我的秒杀订单 | 查看已抢订单 |

### 现有改动

- `App.vue`：导航栏新增「秒杀」入口
- `api/index.js`：新增秒杀相关 API 函数

## 网关路由

```yaml
- id: seckill-service
  uri: lb://supergaos-seckill
  predicates:
    - Path=/api/seckill/**
```

## Docker

### 新增服务

```yaml
redis:
  image: redis:7-alpine
  container_name: blog-redis
  ports:
    - "6379:6379"

rabbitmq:
  image: rabbitmq:3-management-alpine
  container_name: blog-rabbitmq
  ports:
    - "5672:5672"
    - "15672:15672"

seckill-service:
  build:
    context: .
    dockerfile: Dockerfile.seckill
  container_name: blog-seckill
  ports:
    - "9095:9095"
  environment:
    NACOS_SERVER_ADDR: nacos:8848
    SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/supergaos_seckill
    SPRING_DATASOURCE_USERNAME: root
    SPRING_DATASOURCE_PASSWORD: root123
    REDIS_HOST: redis
    RABBITMQ_HOST: rabbitmq
  depends_on:
    mysql: { condition: service_healthy }
    nacos: { condition: service_healthy }
    redis: { condition: service_started }
    rabbitmq: { condition: service_started }
```

## 限流与安全

- 用户级别限流：Redisson 令牌桶，每用户每秒 1 次抢购请求
- 重复请求：前端按钮禁用 + Redis Set 去重
- JWT 认证：抢购/订单接口必须带有效 token
- 活动 ID：使用数字 ID，结合时间校验防止提前抢购

## 边界情况

| 场景 | 处理 |
|------|------|
| 未登录抢购 | 网关返回 401，前端跳转登录页 |
| 活动未开始 | Redis 校验返回「活动未开始」 |
| 重复抢购 | Lua 脚本返回 -1，提示「您已参与过」 |
| 已售罄 | Lua 脚本返回 -2，提示「已售罄」 |
| MQ 消息积压 | 订单异步落库，用户通过订单列表轮询 |
| 订单落库失败 | MQ 重试机制，重试 3 次后进入死信队列 |
| Redis 宕机 | 降级为 MySQL 乐观锁（降级方案） |

## 实现顺序

1. 基础设施：添加 Redis + RabbitMQ 到 docker-compose
2. 新建模块：`supergaos-seckill` 骨架（pom.xml、配置文件、启动类）
3. 实体 + MyBatis：`SeckillActivity`、`SeckillOrder` + Mapper
4. Redis Lua 脚本 + 库存预加载
5. 抢购核心业务（`SeckillService.grab()`）
6. MQ 生产者 + 消费者（`OrderConsumer`）
7. 后端 API：`SeckillController`
8. 网关路由配置
9. 前端：秒杀列表页 + 订单页
10. 端到端联调
