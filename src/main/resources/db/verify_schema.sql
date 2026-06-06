-- 检查 movie 表 v2 结构是否齐全（不修改任何数据）

USE framespace;

SELECT '=== movie 表列结构 ===' AS info;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'framespace' AND TABLE_NAME = 'movie'
ORDER BY ORDINAL_POSITION;

SELECT '=== movie 表索引 ===' AS info;
SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'framespace' AND TABLE_NAME = 'movie'
GROUP BY INDEX_NAME, NON_UNIQUE;

SELECT '=== v2 必需列检查（应为 11） ===' AS info;
SELECT COUNT(*) AS v2_column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'framespace'
  AND TABLE_NAME = 'movie'
  AND COLUMN_NAME IN (
    'tmdb_id', 'category', 'region', 'genres', 'genre_ids',
    'rank_num', 'backdrop_url', 'runtime', 'director', 'cast_list', 'synced_at'
  );

SELECT '=== v2 必需索引检查（应为 4） ===' AS info;
SELECT COUNT(DISTINCT INDEX_NAME) AS v2_index_count
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'framespace'
  AND TABLE_NAME = 'movie'
  AND INDEX_NAME IN (
    'uk_movie_tmdb_category',
    'idx_movie_category_region',
    'idx_movie_category_rank',
    'idx_movie_release_year'
  );

SELECT '=== 当前电影数据统计 ===' AS info;
SELECT
    COUNT(*) AS total,
    SUM(category = 'NOW_PLAYING') AS now_playing,
    SUM(category = 'TOP500') AS top500,
    SUM(tmdb_id IS NOT NULL) AS with_tmdb
FROM movie;
