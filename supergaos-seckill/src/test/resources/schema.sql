CREATE TABLE IF NOT EXISTS seckill_activity (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(200) NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    stock        INT NOT NULL DEFAULT 0,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NOT NULL,
    status       TINYINT DEFAULT 0,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS seckill_order (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id   BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    amount        DECIMAL(10,2) NOT NULL,
    status        TINYINT DEFAULT 0,
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_user (activity_id, user_id)
);
