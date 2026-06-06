-- FrameSpace v6：评论回复、消息通知、评论关联 tmdb_id
USE framespace;

ALTER TABLE user_comment
    ADD COLUMN parent_comment_id BIGINT NULL COMMENT '回复的父评论 ID' AFTER movie_id,
    ADD COLUMN reply_to_user_id BIGINT NULL COMMENT '被回复用户 ID' AFTER parent_comment_id,
    ADD COLUMN tmdb_id INT NULL COMMENT 'TMDB 电影 ID，防同步丢关联' AFTER reply_to_user_id,
    ADD KEY idx_comment_parent (parent_comment_id);

UPDATE user_comment uc
    INNER JOIN movie m ON uc.movie_id = m.id
SET uc.tmdb_id = m.tmdb_id
WHERE uc.tmdb_id IS NULL AND m.tmdb_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS user_notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收者',
    actor_id BIGINT NOT NULL COMMENT '触发者',
    type VARCHAR(20) NOT NULL COMMENT 'COMMENT_LIKE / COMMENT_REPLY',
    comment_id BIGINT NOT NULL,
    movie_id BIGINT NULL,
    preview VARCHAR(500) NULL COMMENT '回复内容摘要',
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notif_user_time (user_id, created_at DESC),
    KEY idx_notif_user_unread (user_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
