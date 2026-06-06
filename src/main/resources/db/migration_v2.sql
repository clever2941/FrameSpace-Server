-- FrameSpace 电影模块 v2 升级脚本（首次全新库使用）
-- ⚠️ 若报 Duplicate column / Duplicate key，说明已升级成功，无需重复执行！
-- 请改执行：verify_schema.sql 验证；或 migration_v2_helpers.sql + migration_v2_idempotent.sql

USE framespace;

-- 1. 扩展 movie 表（支持热映 / Top500、TMDB 海报、分类筛选）
ALTER TABLE movie
    ADD COLUMN tmdb_id INT NULL COMMENT 'TMDB 电影 ID' AFTER id,
    ADD COLUMN category VARCHAR(20) NULL COMMENT 'NOW_PLAYING / TOP500' AFTER release_year,
    ADD COLUMN region VARCHAR(10) NULL COMMENT 'CN / US / GLOBAL' AFTER category,
    ADD COLUMN genres VARCHAR(255) NULL COMMENT '类型中文，逗号分隔' AFTER region,
    ADD COLUMN genre_ids VARCHAR(64) NULL COMMENT 'TMDB 类型 ID，逗号分隔' AFTER genres,
    ADD COLUMN rank_num INT NULL COMMENT 'Top500 排名' AFTER genre_ids,
    ADD COLUMN backdrop_url VARCHAR(512) NULL COMMENT '横幅宣传图' AFTER poster_url,
    ADD COLUMN runtime INT NULL COMMENT '片长(分钟)' AFTER backdrop_url,
    ADD COLUMN director VARCHAR(128) NULL COMMENT '导演' AFTER runtime,
    ADD COLUMN cast_list VARCHAR(512) NULL COMMENT '主演，逗号分隔' AFTER director,
    ADD COLUMN synced_at DATETIME NULL COMMENT '最近同步时间' AFTER cast_list;

-- 2. 索引（提升筛选与去重效率）
CREATE UNIQUE INDEX uk_movie_tmdb_category ON movie (tmdb_id, category);
CREATE INDEX idx_movie_category_region ON movie (category, region);
CREATE INDEX idx_movie_category_rank ON movie (category, rank_num);
CREATE INDEX idx_movie_release_year ON movie (release_year);

-- 3. 可选：清空旧 Mock 数据（海报为占位图时建议执行）
-- DELETE FROM movie WHERE tmdb_id IS NULL;
