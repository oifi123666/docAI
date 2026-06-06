package com.javaee.documentservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Keeps the document-service schema compatible with the current code when an
 * existing deployment was created from an older SQL file.
 */
@Component
public class DocumentSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentSchemaInitializer.class);
    private static final String TARGET_CHARACTER_SET = "utf8mb4";
    private static final String TARGET_COLLATION = "utf8mb4_unicode_ci";
    private static final List<String> MANAGED_TABLES = List.of(
            "document",
            "document_access",
            "document_version",
            "document_comment",
            "document_annotation"
    );

    private final DataSource dataSource;

    @Value("${document.schema.auto-migration.enabled:true}")
    private boolean enabled;

    public DocumentSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("文档服务数据库结构自动修复已关闭");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            migrate(connection);
        } catch (SQLException e) {
            log.error("文档服务数据库结构自动修复失败", e);
            throw new IllegalStateException("文档服务数据库结构自动修复失败", e);
        }
    }

    private void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            execute(statement, """
                    CREATE TABLE IF NOT EXISTS `document` (
                      `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '文档ID',
                      `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
                      `content` TEXT COMMENT '文档内容',
                      `summary` TEXT COMMENT '文档摘要',
                      `keywords` TEXT COMMENT '关键词（JSON格式）',
                      `file_id` VARCHAR(64) COMMENT '关联文件ID',
                      `user_id` BIGINT COMMENT '创建用户ID',
                      `bucket_name` VARCHAR(128) COMMENT '文档内容所在MinIO桶',
                      `object_name` VARCHAR(512) COMMENT '文档内容在MinIO中的对象名',
                      `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-活跃，deleted-已删除',
                      `version` INT DEFAULT 1 COMMENT '版本号',
                      `category` VARCHAR(50) COMMENT '分类',
                      `tags` TEXT COMMENT '标签（JSON格式）',
                      `created_by` VARCHAR(64) COMMENT '创建人',
                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      INDEX `idx_user_id` (`user_id`),
                      INDEX `idx_bucket_name` (`bucket_name`),
                      INDEX `idx_status` (`status`),
                      INDEX `idx_category` (`category`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表'
                    """);

            addColumnIfMissing(connection, statement, "document", "content",
                    "ALTER TABLE `document` ADD COLUMN `content` TEXT COMMENT '文档内容'");
            addColumnIfMissing(connection, statement, "document", "summary",
                    "ALTER TABLE `document` ADD COLUMN `summary` TEXT COMMENT '文档摘要'");
            addColumnIfMissing(connection, statement, "document", "keywords",
                    "ALTER TABLE `document` ADD COLUMN `keywords` TEXT COMMENT '关键词（JSON格式）'");
            addColumnIfMissing(connection, statement, "document", "file_id",
                    "ALTER TABLE `document` ADD COLUMN `file_id` VARCHAR(64) COMMENT '关联文件ID'");
            addColumnIfMissing(connection, statement, "document", "user_id",
                    "ALTER TABLE `document` ADD COLUMN `user_id` BIGINT COMMENT '创建用户ID'");
            addColumnIfMissing(connection, statement, "document", "bucket_name",
                    "ALTER TABLE `document` ADD COLUMN `bucket_name` VARCHAR(128) COMMENT '文档内容所在MinIO桶' AFTER `user_id`");
            addColumnIfMissing(connection, statement, "document", "object_name",
                    "ALTER TABLE `document` ADD COLUMN `object_name` VARCHAR(512) COMMENT '文档内容在MinIO中的对象名' AFTER `bucket_name`");
            addColumnIfMissing(connection, statement, "document", "status",
                    "ALTER TABLE `document` ADD COLUMN `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-活跃，deleted-已删除'");
            addColumnIfMissing(connection, statement, "document", "version",
                    "ALTER TABLE `document` ADD COLUMN `version` INT DEFAULT 1 COMMENT '版本号'");
            addColumnIfMissing(connection, statement, "document", "category",
                    "ALTER TABLE `document` ADD COLUMN `category` VARCHAR(50) COMMENT '分类'");
            addColumnIfMissing(connection, statement, "document", "tags",
                    "ALTER TABLE `document` ADD COLUMN `tags` TEXT COMMENT '标签（JSON格式）'");
            addColumnIfMissing(connection, statement, "document", "created_by",
                    "ALTER TABLE `document` ADD COLUMN `created_by` VARCHAR(64) COMMENT '创建人'");
            addColumnIfMissing(connection, statement, "document", "create_time",
                    "ALTER TABLE `document` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
            addColumnIfMissing(connection, statement, "document", "update_time",
                    "ALTER TABLE `document` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
            addIndexIfMissing(connection, statement, "document", "idx_user_id",
                    "ALTER TABLE `document` ADD INDEX `idx_user_id` (`user_id`)");
            addIndexIfMissing(connection, statement, "document", "idx_bucket_name",
                    "ALTER TABLE `document` ADD INDEX `idx_bucket_name` (`bucket_name`)");
            addIndexIfMissing(connection, statement, "document", "idx_status",
                    "ALTER TABLE `document` ADD INDEX `idx_status` (`status`)");
            addIndexIfMissing(connection, statement, "document", "idx_category",
                    "ALTER TABLE `document` ADD INDEX `idx_category` (`category`)");

            execute(statement, """
                    CREATE TABLE IF NOT EXISTS `document_access` (
                      `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '访问记录ID',
                      `document_id` VARCHAR(64) NOT NULL COMMENT '文档ID',
                      `bucket_name` VARCHAR(128) NOT NULL COMMENT '文档所在MinIO桶',
                      `user_id` BIGINT NOT NULL COMMENT '可访问用户ID',
                      `role` VARCHAR(20) NOT NULL DEFAULT 'editor' COMMENT '角色：owner/editor/viewer',
                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      UNIQUE KEY `uk_document_user` (`document_id`, `user_id`),
                      INDEX `idx_user_bucket` (`user_id`, `bucket_name`),
                      INDEX `idx_bucket_name` (`bucket_name`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档协作访问表'
                    """);
            addColumnIfMissing(connection, statement, "document_access", "bucket_name",
                    "ALTER TABLE `document_access` ADD COLUMN `bucket_name` VARCHAR(128) NOT NULL COMMENT '文档所在MinIO桶' AFTER `document_id`");
            addColumnIfMissing(connection, statement, "document_access", "role",
                    "ALTER TABLE `document_access` ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'editor' COMMENT '角色：owner/editor/viewer' AFTER `user_id`");
            addColumnIfMissing(connection, statement, "document_access", "create_time",
                    "ALTER TABLE `document_access` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
            addColumnIfMissing(connection, statement, "document_access", "update_time",
                    "ALTER TABLE `document_access` ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
            addIndexIfMissing(connection, statement, "document_access", "uk_document_user",
                    "ALTER TABLE `document_access` ADD UNIQUE KEY `uk_document_user` (`document_id`, `user_id`)");
            addIndexIfMissing(connection, statement, "document_access", "idx_user_bucket",
                    "ALTER TABLE `document_access` ADD INDEX `idx_user_bucket` (`user_id`, `bucket_name`)");

            execute(statement, """
                    CREATE TABLE IF NOT EXISTS `document_version` (
                      `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '版本ID',
                      `document_id` VARCHAR(64) NOT NULL COMMENT '所属文档ID',
                      `version_number` INT NOT NULL COMMENT '版本号',
                      `title` VARCHAR(255) NOT NULL COMMENT '文档标题',
                      `content` TEXT COMMENT '文档内容',
                      `summary` TEXT COMMENT '文档摘要',
                      `keywords` TEXT COMMENT '关键词',
                      `change_log` VARCHAR(500) COMMENT '变更日志',
                      `created_by` VARCHAR(64) COMMENT '创建人',
                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      INDEX `idx_document_id` (`document_id`),
                      INDEX `idx_version_number` (`version_number`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档版本表'
                    """);
            addColumnIfMissing(connection, statement, "document_version", "document_id",
                    "ALTER TABLE `document_version` ADD COLUMN `document_id` VARCHAR(64) NOT NULL COMMENT '所属文档ID'");
            addColumnIfMissing(connection, statement, "document_version", "version_number",
                    "ALTER TABLE `document_version` ADD COLUMN `version_number` INT NOT NULL COMMENT '版本号'");
            addColumnIfMissing(connection, statement, "document_version", "title",
                    "ALTER TABLE `document_version` ADD COLUMN `title` VARCHAR(255) NOT NULL COMMENT '文档标题'");
            addColumnIfMissing(connection, statement, "document_version", "content",
                    "ALTER TABLE `document_version` ADD COLUMN `content` TEXT COMMENT '文档内容'");
            addColumnIfMissing(connection, statement, "document_version", "summary",
                    "ALTER TABLE `document_version` ADD COLUMN `summary` TEXT COMMENT '文档摘要'");
            addColumnIfMissing(connection, statement, "document_version", "keywords",
                    "ALTER TABLE `document_version` ADD COLUMN `keywords` TEXT COMMENT '关键词'");
            addColumnIfMissing(connection, statement, "document_version", "change_log",
                    "ALTER TABLE `document_version` ADD COLUMN `change_log` VARCHAR(500) COMMENT '变更日志'");
            addColumnIfMissing(connection, statement, "document_version", "created_by",
                    "ALTER TABLE `document_version` ADD COLUMN `created_by` VARCHAR(64) COMMENT '创建人'");
            addColumnIfMissing(connection, statement, "document_version", "create_time",
                    "ALTER TABLE `document_version` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
            addIndexIfMissing(connection, statement, "document_version", "idx_document_id",
                    "ALTER TABLE `document_version` ADD INDEX `idx_document_id` (`document_id`)");
            addIndexIfMissing(connection, statement, "document_version", "idx_version_number",
                    "ALTER TABLE `document_version` ADD INDEX `idx_version_number` (`version_number`)");

            execute(statement, """
                    CREATE TABLE IF NOT EXISTS `document_comment` (
                      `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '评论ID',
                      `document_id` VARCHAR(64) NOT NULL COMMENT '所属文档ID',
                      `user_id` BIGINT COMMENT '评论用户ID',
                      `content` TEXT COMMENT '评论内容',
                      `parent_id` VARCHAR(64) COMMENT '父评论ID（回复）',
                      `created_by` VARCHAR(64) COMMENT '创建人',
                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-活跃，deleted-已删除',
                      INDEX `idx_document_id` (`document_id`),
                      INDEX `idx_user_id` (`user_id`),
                      INDEX `idx_parent_id` (`parent_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档评论表'
                    """);

            execute(statement, """
                    CREATE TABLE IF NOT EXISTS `document_annotation` (
                      `id` VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '批注ID',
                      `document_id` VARCHAR(64) NOT NULL COMMENT '所属文档ID',
                      `user_id` BIGINT COMMENT '批注用户ID',
                      `line_number` INT COMMENT '批注所在行号',
                      `start_offset` INT COMMENT '批注起始位置',
                      `end_offset` INT COMMENT '批注结束位置',
                      `annotation_type` VARCHAR(50) COMMENT '批注类型：highlight-高亮，comment-注释，note-笔记',
                      `content` TEXT COMMENT '批注内容',
                      `color` VARCHAR(20) COMMENT '批注颜色',
                      `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-活跃，deleted-已删除',
                      `created_by` VARCHAR(64) COMMENT '创建人',
                      `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      INDEX `idx_document_id` (`document_id`),
                      INDEX `idx_user_id` (`user_id`),
                      INDEX `idx_line_number` (`line_number`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档批注表'
                    """);

            normalizeManagedTableCollations(connection, statement);
        }
    }

    private void normalizeManagedTableCollations(Connection connection, Statement statement) throws SQLException {
        for (String table : MANAGED_TABLES) {
            if (tableExists(connection, table) && tableNeedsCollationRepair(connection, table)) {
                execute(statement, "ALTER TABLE `" + table + "` CONVERT TO CHARACTER SET "
                        + TARGET_CHARACTER_SET + " COLLATE " + TARGET_COLLATION);
            }
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private boolean tableNeedsCollationRepair(Connection connection, String table) throws SQLException {
        String catalog = connection.getCatalog();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT TABLE_COLLATION
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
                """)) {
            ps.setString(1, catalog);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && differs(rs.getString("TABLE_COLLATION"), TARGET_COLLATION)) {
                    return true;
                }
            }
        }

        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT CHARACTER_SET_NAME, COLLATION_NAME
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_NAME = ?
                  AND CHARACTER_SET_NAME IS NOT NULL
                """)) {
            ps.setString(1, catalog);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (differs(rs.getString("CHARACTER_SET_NAME"), TARGET_CHARACTER_SET)
                            || differs(rs.getString("COLLATION_NAME"), TARGET_COLLATION)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean differs(String actual, String expected) {
        return actual == null || !actual.equalsIgnoreCase(expected);
    }

    private void addColumnIfMissing(Connection connection, Statement statement, String table, String column, String sql)
            throws SQLException {
        if (!columnExists(connection, table, column)) {
            execute(statement, sql);
        }
    }

    private void addIndexIfMissing(Connection connection, Statement statement, String table, String index, String sql)
            throws SQLException {
        if (!indexExists(connection, table, index)) {
            execute(statement, sql);
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }

    private boolean indexExists(Connection connection, String table, String index) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (rs.next()) {
                if (index.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void execute(Statement statement, String sql) throws SQLException {
        log.debug("执行文档服务数据库结构语句: {}", sql);
        statement.execute(sql);
    }
}
