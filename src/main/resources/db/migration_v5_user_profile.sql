-- FrameSpace v5：用户资料（昵称、头像、个性签名）
USE framespace;

ALTER TABLE user
    ADD COLUMN nickname VARCHAR(64) NULL COMMENT '昵称' AFTER username,
    ADD COLUMN avatar_url VARCHAR(512) NULL COMMENT '头像 URL' AFTER nickname,
    ADD COLUMN bio VARCHAR(200) NULL COMMENT '个性签名' AFTER avatar_url;
