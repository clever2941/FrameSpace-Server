# FrameSpace MySQL 升级说明（v2）

## 一、执行顺序

1. 确保已创建数据库 `framespace`
2. 升级 `movie` 表（二选一）：
   - **首次空库**：执行 `migration_v2.sql`
   - **已报 Duplicate 错误 / 可重复执行**：先 `migration_v2_helpers.sql`，再 `migration_v2_idempotent.sql`
   - **仅检查是否已就绪**：执行 `verify_schema.sql`（`v2_column_count=11` 且 `v2_index_count=4` 即成功）
3. 在 `application.properties` 配置 **TMDB API Key**
4. 重启 Spring Boot，首次访问列表接口会自动从 TMDB 同步数据

### 关于 Duplicate 报错

若出现 `Duplicate column name 'tmdb_id'` 或 `Duplicate key name 'uk_movie_tmdb_category'`，
表示 **列和索引已经存在，迁移实际上已完成**，不必再跑 `migration_v2.sql`。
直接执行 `verify_schema.sql` 确认即可。

## 二、完整建表（全新环境）

若尚无 `movie` 表，可直接执行：

```sql
USE framespace;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `movie` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tmdb_id` INT NULL COMMENT 'TMDB 电影 ID',
  `title` VARCHAR(255) NULL,
  `original_title` VARCHAR(255) NULL,
  `poster_url` VARCHAR(512) NULL COMMENT '官方宣传海报',
  `backdrop_url` VARCHAR(512) NULL COMMENT '官方宣传横幅',
  `rating` DOUBLE NULL,
  `summary` TEXT NULL,
  `release_year` VARCHAR(8) NULL,
  `category` VARCHAR(20) NULL COMMENT 'NOW_PLAYING / TOP500',
  `region` VARCHAR(10) NULL COMMENT 'CN / US / GLOBAL',
  `genres` VARCHAR(255) NULL,
  `genre_ids` VARCHAR(64) NULL,
  `rank_num` INT NULL COMMENT 'Top500 排名',
  `runtime` INT NULL,
  `director` VARCHAR(128) NULL,
  `cast_list` VARCHAR(512) NULL,
  `synced_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_movie_tmdb_category` (`tmdb_id`, `category`),
  KEY `idx_movie_category_region` (`category`, `region`),
  KEY `idx_movie_category_rank` (`category`, `rank_num`),
  KEY `idx_movie_release_year` (`release_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 三、从 v1 升级

若已有旧版 `movie` 表（仅含 title/poster_url 等字段），执行：

`Server/demo/src/main/resources/db/migration_v2.sql`

建议同步前清理旧 Mock 数据：

```sql
DELETE FROM movie WHERE tmdb_id IS NULL;
```

## 四、TMDB API Key

1. 注册 https://www.themoviedb.org/
2. 进入 Settings → API → 申请 API Key（Developer）
3. 写入 `application.properties`：

```properties
tmdb.api.key=你的密钥
```

## 五、数据同步策略

| 分类 | 来源 | 缓存策略 |
|------|------|----------|
| 正在热映 | TMDB `now_playing`（CN + US） | 6 小时内不重复拉取 |
| Top500 | TMDB `top_rated` 前 25 页 | 24 小时内不重复拉取 |
| 详情 | TMDB `movie/{id}` + credits | 访问详情时补全并写回 DB |

海报地址示例：`https://image.tmdb.org/t/p/w500/xxx.jpg`（官方宣传图）

## 六、v3 社交功能表（migration_v3_social.sql）

| 表名 | 用途 |
|------|------|
| `user_comment` | 用户评论 |
| `comment_like` | 评论点赞 |
| `movie_like` | 电影点赞 |
| `movie_favorite` | 个人收藏 |
| `browse_history` | 浏览记录 |

执行：`db/migration_v3_social.sql`
