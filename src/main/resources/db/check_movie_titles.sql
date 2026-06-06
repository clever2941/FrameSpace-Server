-- 检查 movie 表片名是否为中文（不修改数据）
USE framespace;

SELECT '=== 片名概况 ===' AS info;
SELECT
    COUNT(*) AS total,
    SUM(tmdb_id IS NOT NULL) AS with_tmdb,
    SUM(title REGEXP '[一-龥]') AS title_has_chinese,
    SUM(title NOT REGEXP '[一-龥]' OR title IS NULL) AS title_no_chinese
FROM movie;

SELECT '=== 按分类统计 ===' AS info;
SELECT
    category,
    COUNT(*) AS cnt,
    SUM(title REGEXP '[一-龥]') AS has_chinese,
    SUM(title NOT REGEXP '[一-龥]' OR title IS NULL) AS no_chinese
FROM movie
GROUP BY category;

SELECT '=== 无中文片名样例（前 20 条）===' AS info;
SELECT id, tmdb_id, category, region, title, original_title
FROM movie
WHERE title NOT REGEXP '[一-龥]' OR title IS NULL
ORDER BY id
LIMIT 20;

SELECT '=== 有中文片名样例（前 10 条）===' AS info;
SELECT id, tmdb_id, category, title, original_title
FROM movie
WHERE title REGEXP '[一-龥]'
ORDER BY id
LIMIT 10;
