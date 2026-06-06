-- FrameSpace v7：好友、私信、推片
USE framespace;

CREATE TABLE IF NOT EXISTS friend_request (
    id BIGINT NOT NULL AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ACCEPTED / REJECTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_friend_req_pair (from_user_id, to_user_id),
    KEY idx_friend_req_to (to_user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_friend (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    KEY idx_user_friend (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS private_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT / MOVIE_PUSH',
    content VARCHAR(2000) NULL,
    movie_id BIGINT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pm_receiver (receiver_id, created_at DESC),
    KEY idx_pm_sender (sender_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
