-- FrameSpace v2 幂等升级脚本（可重复执行，已存在的列/索引会自动跳过）
-- 在 DataGrip / MySQL Workbench 中整段执行即可

USE framespace;

-- ========== 1. 按需添加列 ==========
CALL add_column_if_missing('movie', 'tmdb_id',       'INT NULL COMMENT ''TMDB 电影 ID'' AFTER id');
CALL add_column_if_missing('movie', 'category',      'VARCHAR(20) NULL COMMENT ''NOW_PLAYING / TOP500'' AFTER release_year');
CALL add_column_if_missing('movie', 'region',        'VARCHAR(10) NULL COMMENT ''CN / US / GLOBAL'' AFTER category');
CALL add_column_if_missing('movie', 'genres',        'VARCHAR(255) NULL COMMENT ''类型中文'' AFTER region');
CALL add_column_if_missing('movie', 'genre_ids',     'VARCHAR(64) NULL COMMENT ''TMDB 类型 ID'' AFTER genres');
CALL add_column_if_missing('movie', 'rank_num',      'INT NULL COMMENT ''Top500 排名'' AFTER genre_ids');
CALL add_column_if_missing('movie', 'backdrop_url',  'VARCHAR(512) NULL COMMENT ''横幅宣传图'' AFTER poster_url');
CALL add_column_if_missing('movie', 'runtime',       'INT NULL COMMENT ''片长(分钟)'' AFTER backdrop_url');
CALL add_column_if_missing('movie', 'director',      'VARCHAR(128) NULL COMMENT ''导演'' AFTER runtime');
CALL add_column_if_missing('movie', 'cast_list',     'VARCHAR(512) NULL COMMENT ''主演'' AFTER director');
CALL add_column_if_missing('movie', 'synced_at',     'DATETIME NULL COMMENT ''最近同步时间'' AFTER cast_list');

-- ========== 2. 按需创建索引 ==========
CALL add_index_if_missing('movie', 'uk_movie_tmdb_category', 'CREATE UNIQUE INDEX uk_movie_tmdb_category ON movie (tmdb_id, category)');
CALL add_index_if_missing('movie', 'idx_movie_category_region', 'CREATE INDEX idx_movie_category_region ON movie (category, region)');
CALL add_index_if_missing('movie', 'idx_movie_category_rank', 'CREATE INDEX idx_movie_category_rank ON movie (category, rank_num)');
CALL add_index_if_missing('movie', 'idx_movie_release_year', 'CREATE INDEX idx_movie_release_year ON movie (release_year)');

-- ========== 3. 清理辅助存储过程（可选） ==========
DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;

-- ========== 4. 验证（应返回 11 行列名） ==========
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'framespace' AND TABLE_NAME = 'movie'
ORDER BY ORDINAL_POSITION;
