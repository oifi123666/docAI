-- File service database initialization script.
CREATE DATABASE IF NOT EXISTS `doc_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `doc_ai`;

CREATE TABLE IF NOT EXISTS `file_metadata` (
  `id` VARCHAR(64) NOT NULL COMMENT 'metadata primary key',
  `file_id` VARCHAR(64) NOT NULL COMMENT 'business file id',
  `file_name` VARCHAR(255) NOT NULL COMMENT 'stored file name',
  `original_file_name` VARCHAR(255) DEFAULT NULL COMMENT 'original upload file name',
  `file_path` VARCHAR(1024) DEFAULT NULL COMMENT 'file storage path',
  `file_type` VARCHAR(128) DEFAULT NULL COMMENT 'content type',
  `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT 'file size in bytes',
  `md5` VARCHAR(64) DEFAULT NULL COMMENT 'file md5 checksum',
  `storage_type` VARCHAR(32) DEFAULT NULL COMMENT 'storage type',
  `bucket_name` VARCHAR(128) DEFAULT NULL COMMENT 'storage bucket name',
  `object_key` VARCHAR(512) DEFAULT NULL COMMENT 'storage object key',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'metadata status',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT 'creator',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_metadata_file_id` (`file_id`),
  KEY `idx_file_metadata_bucket_status` (`bucket_name`, `status`),
  KEY `idx_file_metadata_file_name` (`file_name`),
  KEY `idx_file_metadata_create_time` (`create_time`),
  KEY `idx_file_metadata_md5` (`md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='file metadata table';
