package com.javaee.fileservice.service.impl;

import com.javaee.fileservice.config.FileStorageConfig;
import com.javaee.fileservice.config.MinioConfig;
import com.javaee.fileservice.service.FileMetadataService;
import com.javaee.fileservice.service.FileService;
import com.javaee.fileservice.util.FileUtils;
import com.javaee.fileservice.util.Md5Utils;
import com.javaee.fileservice.util.PathUtils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.StatObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 文件服务实现类
 */
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private MinioClient minioClient;

    // 用于存储分片上传的临时文件
    private final ConcurrentMap<String, ConcurrentMap<Integer, File>> chunkMap = new ConcurrentHashMap<>();

    @Override
    public String upload(MultipartFile file) {
        try {
            // 生成文件ID
            String fileId = UUID.randomUUID().toString();
            String fileName = file.getOriginalFilename();
            String fileExtension = FileUtils.getFileExtension(fileName);
            String storageFileName = fileId + (fileExtension != null ? "." + fileExtension : "");

            // 根据存储类型上传文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
            Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
            System.out.println("存储路径: " + storagePath);
            try {
                // 确保存储目录存在
                Path storageDir = storagePath.getParent();
                if (storageDir != null) {
                    if (!Files.exists(storageDir)) {
                        Files.createDirectories(storageDir);
                        System.out.println("目录创建成功: " + storageDir);
                    } else {
                        System.out.println("目录已存在: " + storageDir);
                    }
                }
                
                // 使用FileOutputStream保存文件
                File destFile = storagePath.toFile();
                System.out.println("目标文件: " + destFile.getAbsolutePath());
                System.out.println("目标文件是否存在: " + destFile.exists());
                System.out.println("目标文件是否可写: " + destFile.canWrite());
                
                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = file.getInputStream().read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    fos.flush();
                    System.out.println("文件上传成功: " + storagePath);
                }
            } catch (Exception e) {
                System.out.println("本地存储错误: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储
                System.out.println("=== 开始MinIO存储 ===");
                System.out.println("存储桶名称: " + fileStorageConfig.getBucketName());
                System.out.println("存储文件名: " + storageFileName);
                System.out.println("文件大小: " + file.getSize());
                System.out.println("文件类型: " + file.getContentType());
                try {
                    ensureBucketExists(fileStorageConfig.getBucketName());
                    System.out.println("存储桶检查/创建成功");
                    try (InputStream inputStream = file.getInputStream()) {
                        System.out.println("获取文件输入流成功");
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(storageFileName)
                                        .stream(inputStream, file.getSize(), -1)
                                        .contentType(file.getContentType())
                                        .build()
                        );
                        System.out.println("文件上传到MinIO成功");
                    }
                } catch (Exception e) {
                    System.out.println("MinIO存储错误: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            // 保存文件元数据（容错处理，数据库不可用时继续执行）
            try {
                com.javaee.fileservice.entity.FileMetadata fileMetadata = new com.javaee.fileservice.entity.FileMetadata();
                fileMetadata.setFileId(fileId);
                fileMetadata.setFileName(fileName);
                fileMetadata.setOriginalFileName(fileName);
                fileMetadata.setFilePath(fileStorageConfig.getLocalPath());
                fileMetadata.setFileType(file.getContentType());
                fileMetadata.setFileSize(file.getSize());
                fileMetadata.setStorageType(fileStorageConfig.getStorageType());
                fileMetadata.setBucketName(fileStorageConfig.getBucketName());
                fileMetadata.setObjectKey(storageFileName);
                fileMetadata.setCreateBy("system");
                fileMetadataService.saveMetadata(fileMetadata);
            } catch (Exception e) {
                // 数据库不可用时，继续执行，只记录日志
                System.out.println("数据库不可用，跳过元数据保存: " + e.getMessage());
            }

            return fileId;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] uploadMultiple(MultipartFile[] files) {
        String[] fileIds = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileIds[i] = upload(files[i]);
        }
        return fileIds;
    }

    @Override
    public void uploadChunk(MultipartFile chunk, String fileId, int chunkIndex, int totalChunks) {
        try {
            // 确保文件ID对应的分片映射存在
            chunkMap.computeIfAbsent(fileId, k -> new ConcurrentHashMap<>());
            ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);

            // 保存分片文件
            File chunkFile = File.createTempFile("chunk_" + fileId + "_", null);
            chunk.transferTo(chunkFile);
            chunks.put(chunkIndex, chunkFile);
        } catch (Exception e) {
            throw new RuntimeException("分片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String mergeChunk(String fileId, String fileName) {
        try {
            // 获取分片文件
            ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);
            if (chunks == null || chunks.isEmpty()) {
                throw new RuntimeException("没有找到分片文件");
            }

            // 生成存储文件名
            String fileExtension = FileUtils.getFileExtension(fileName);
            String storageFileName = fileId + (fileExtension != null ? "." + fileExtension : "");
            byte[] mergedBytes = null;

            // 根据存储类型合并分片
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储合并
                Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                Files.createDirectories(storagePath.getParent());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // 按顺序读取分片并合并
                for (int i = 0; i < chunks.size(); i++) {
                    File chunkFile = chunks.get(i);
                    if (chunkFile != null) {
                        byte[] chunkBytes = Files.readAllBytes(chunkFile.toPath());
                        outputStream.write(chunkBytes);
                        // 删除临时分片文件
                        chunkFile.delete();
                    }
                }

                mergedBytes = outputStream.toByteArray();
                Files.write(storagePath, mergedBytes);
                outputStream.close();
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储合并（这里简化处理，实际应该使用MinIO的分片上传API）
                ensureBucketExists(fileStorageConfig.getBucketName());
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // 按顺序读取分片并合并
                for (int i = 0; i < chunks.size(); i++) {
                    File chunkFile = chunks.get(i);
                    if (chunkFile != null) {
                        byte[] chunkBytes = Files.readAllBytes(chunkFile.toPath());
                        outputStream.write(chunkBytes);
                        // 删除临时分片文件
                        chunkFile.delete();
                    }
                }

                mergedBytes = outputStream.toByteArray();
                outputStream.close();

                // 上传合并后的文件
                try (InputStream inputStream = FileUtils.toInputStream(mergedBytes)) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(fileStorageConfig.getBucketName())
                                    .object(storageFileName)
                                    .stream(inputStream, mergedBytes.length, -1)
                                    .contentType(FileUtils.getContentType(fileName))
                                    .build()
                    );
                }
            }

            // 清理分片映射
            chunkMap.remove(fileId);

            // 保存文件元数据
            try {
                com.javaee.fileservice.entity.FileMetadata fileMetadata = new com.javaee.fileservice.entity.FileMetadata();
                fileMetadata.setFileId(fileId);
                fileMetadata.setFileName(fileName);
                fileMetadata.setOriginalFileName(fileName);
                fileMetadata.setFilePath("minio:" + fileStorageConfig.getBucketName());
                fileMetadata.setFileType(com.javaee.fileservice.util.FileUtils.getContentType(fileName));
                fileMetadata.setFileSize(mergedBytes != null ? mergedBytes.length : 0);
                fileMetadata.setStorageType(fileStorageConfig.getStorageType());
                fileMetadata.setBucketName(fileStorageConfig.getBucketName());
                fileMetadata.setObjectKey(storageFileName);
                fileMetadata.setCreateBy("system");
                fileMetadataService.saveMetadata(fileMetadata);
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("数据库不可用，跳过元数据保存: " + e.getMessage());
            }

            return fileId;
        } catch (Exception e) {
            // 清理分片文件
            if (chunkMap.containsKey(fileId)) {
                ConcurrentMap<Integer, File> chunks = chunkMap.get(fileId);
                chunks.values().forEach(File::delete);
                chunkMap.remove(fileId);
            }
            throw new RuntimeException("分片合并失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String fileId) {
        try {
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;
            
            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                }
            } catch (Exception e) {
                // 数据库不可用时，尝试不同的文件扩展名
                System.out.println("数据库不可用，尝试不同的文件扩展名: " + e.getMessage());
                // 尝试常见的文件扩展名
                String[] extensions = {"", ".docx", ".pdf", ".txt", ".jpg", ".png", ".jpeg"};
                for (String ext : extensions) {
                    try {
                        String tempFileName = fileId + ext;
                        try (InputStream inputStream = minioClient.getObject(
                                GetObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(tempFileName)
                                        .build()
                        )) {
                            return FileUtils.toByteArray(inputStream);
                        }
                    } catch (Exception ex) {
                        // 忽略错误，尝试下一个扩展名
                        System.out.println("尝试扩展名失败: " + ext);
                    }
                }
                // 如果所有扩展名都失败，抛出异常
                throw new RuntimeException("文件不存在: " + fileId);
            }
            
            // 如果还是没有storageFileName，使用fileId作为默认值
            if (storageFileName == null) {
                storageFileName = fileId;
            }

            // 根据存储类型下载文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
                Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                return Files.readAllBytes(storagePath);
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储
                try (InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(fileStorageConfig.getBucketName())
                                .object(storageFileName)
                                .build()
                )) {
                    return FileUtils.toByteArray(inputStream);
                }
            }

            throw new RuntimeException("不支持的存储类型: " + fileStorageConfig.getStorageType());
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileId) {
        try {
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;
            
            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                    System.out.println("从数据库获取到文件元数据，存储文件名: " + storageFileName);
                } else {
                    System.out.println("数据库中未找到文件元数据: " + fileId);
                }
            } catch (Exception e) {
                // 数据库不可用时，尝试不同的文件扩展名
                System.out.println("数据库不可用，尝试不同的文件扩展名: " + e.getMessage());
                // 尝试常见的文件扩展名
                String[] extensions = {".docx", ".pdf", ".txt", ".jpg", ".png", ".jpeg", ""};
                boolean deleted = false;
                for (String ext : extensions) {
                    try {
                        String tempFileName = fileId + ext;
                        System.out.println("尝试删除MinIO文件: " + tempFileName);
                        System.out.println("MinIO配置: bucket=" + fileStorageConfig.getBucketName());
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(tempFileName)
                                        .build()
                        );
                        deleted = true;
                        System.out.println("成功删除文件: " + tempFileName);
                        // 继续尝试其他扩展名，确保删除所有可能的文件
                    } catch (Exception ex) {
                        // 忽略错误，尝试下一个扩展名
                        System.out.println("尝试扩展名失败: " + ext + ", 错误: " + ex.getMessage());
                    }
                }
                if (deleted) {
                    return;
                } else {
                    throw new RuntimeException("文件不存在: " + fileId);
                }
            }
            
            // 如果有存储文件名，直接删除
            if (storageFileName != null) {
                System.out.println("使用存储文件名删除文件: " + storageFileName);
                if ("minio".equals(fileStorageConfig.getStorageType())) {
                    try {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(storageFileName)
                                        .build()
                        );
                        System.out.println("成功删除文件: " + storageFileName);
                    } catch (Exception e) {
                        System.out.println("删除文件失败: " + e.getMessage());
                        throw new RuntimeException("文件删除失败: " + e.getMessage());
                    }
                } else if ("local".equals(fileStorageConfig.getStorageType())) {
                    try {
                        Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                        Files.deleteIfExists(storagePath);
                        System.out.println("成功删除本地文件: " + storagePath);
                    } catch (Exception e) {
                        System.out.println("删除本地文件失败: " + e.getMessage());
                        throw new RuntimeException("文件删除失败: " + e.getMessage());
                    }
                } else {
                    throw new RuntimeException("不支持的存储类型: " + fileStorageConfig.getStorageType());
                }
            } else {
                // 如果没有存储文件名，尝试使用fileId加不同扩展名删除
                System.out.println("没有存储文件名，尝试使用fileId加扩展名删除");
                String[] extensions = {".txt", ".docx", ".pdf", ".jpg", ".png", ".jpeg", ""};
                boolean deleted = false;
                for (String ext : extensions) {
                    String tempFileName = fileId + ext;
                    try {
                        if ("minio".equals(fileStorageConfig.getStorageType())) {
                            minioClient.removeObject(
                                    RemoveObjectArgs.builder()
                                            .bucket(fileStorageConfig.getBucketName())
                                            .object(tempFileName)
                                            .build()
                            );
                            System.out.println("成功删除MinIO文件: " + tempFileName);
                        } else if ("local".equals(fileStorageConfig.getStorageType())) {
                            Path storagePath = Paths.get(fileStorageConfig.getLocalPath(), tempFileName);
                            Files.deleteIfExists(storagePath);
                            System.out.println("成功删除本地文件: " + storagePath);
                        }
                        deleted = true;
                        break; // 删除成功后退出循环
                    } catch (Exception ex) {
                        // 忽略错误，尝试下一个扩展名
                        System.out.println("尝试删除失败: " + tempFileName + ", 错误: " + ex.getMessage());
                    }
                }
                if (!deleted) {
                    throw new RuntimeException("文件不存在: " + fileId);
                }
            }
            
            // 删除文件元数据
            try {
                fileMetadataService.deleteMetadata(fileId);
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("数据库不可用，跳过元数据删除: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void rename(String fileId, String newName) {
        try {
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;
            
            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                }
            } catch (Exception e) {
                // 数据库不可用时，尝试不同的文件扩展名
                System.out.println("数据库不可用，尝试不同的文件扩展名: " + e.getMessage());
            }
            
            // 如果没有找到存储文件名，使用fileId作为默认值（去除扩展名）
            if (storageFileName == null) {
                // 去除fileId中的扩展名
                int dotIndex = fileId.lastIndexOf('.');
                if (dotIndex > 0) {
                    storageFileName = fileId.substring(0, dotIndex);
                } else {
                    storageFileName = fileId;
                }
            }
            
            String oldFileName = storageFileName;
            String fileExtension = FileUtils.getFileExtension(newName);
            // 新存储文件名使用fileId（去除扩展名）加上新的文件名
            int dotIndex = fileId.lastIndexOf('.');
            String fileIdWithoutExt = fileId;
            if (dotIndex > 0) {
                fileIdWithoutExt = fileId.substring(0, dotIndex);
            }
            // 直接使用新文件名作为存储文件名
            String newStorageFileName = newName;

            // 根据存储类型重命名文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
                Path oldPath = Paths.get(fileStorageConfig.getLocalPath(), oldFileName);
                Path newPath = Paths.get(fileStorageConfig.getLocalPath(), newStorageFileName);
                Files.move(oldPath, newPath);
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储（先复制再删除）
                try {
                    // 尝试不同的文件扩展名找到原文件
                    String[] extensions = {"", ".txt", ".docx", ".pdf", ".jpg", ".png", ".jpeg"};
                    String foundOldFileName = null;
                    
                    for (String ext : extensions) {
                        try {
                            String tempOldFileName = oldFileName + ext;
                            // 检查文件是否存在
                            minioClient.statObject(
                                    StatObjectArgs.builder()
                                            .bucket(fileStorageConfig.getBucketName())
                                            .object(tempOldFileName)
                                            .build()
                            );
                            foundOldFileName = tempOldFileName;
                            break;
                        } catch (Exception ex) {
                            // 忽略错误，尝试下一个扩展名
                        }
                    }
                    
                    if (foundOldFileName != null) {
                        // 复制文件
                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(newStorageFileName)
                                        .source(
                                                CopySource.builder()
                                                        .bucket(fileStorageConfig.getBucketName())
                                                        .object(foundOldFileName)
                                                        .build()
                                        )
                                        .build()
                        );
                        // 删除原文件
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(foundOldFileName)
                                        .build()
                        );
                        System.out.println("MinIO文件重命名成功: " + foundOldFileName + " -> " + newStorageFileName);
                    } else {
                        System.out.println("MinIO中未找到原文件: " + oldFileName);
                    }
                } catch (Exception e) {
                    // 如果复制失败，尝试直接使用新名称上传（简化处理）
                    System.out.println("MinIO复制失败，尝试直接使用新名称: " + e.getMessage());
                }
            }

            // 更新文件元数据
            try {
                if (fileMetadata != null) {
                    fileMetadata.setFileName(newName);
                    fileMetadata.setOriginalFileName(newName);
                    fileMetadata.setObjectKey(newStorageFileName);
                    fileMetadataService.updateMetadata(fileMetadata);
                }
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("数据库不可用，跳过元数据更新: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("文件重命名失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void move(String fileId, String targetPath) {
        try {
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;
            
            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                    System.out.println("从数据库获取到文件元数据，存储文件名: " + storageFileName);
                } else {
                    System.out.println("数据库中未找到文件元数据: " + fileId);
                }
            } catch (Exception e) {
                // 数据库不可用时，使用fileId作为默认值
                System.out.println("数据库不可用: " + e.getMessage());
                storageFileName = fileId;
            }

            // 如果没有存储文件名，使用fileId作为默认值
            if (storageFileName == null) {
                storageFileName = fileId;
            }

            // 根据存储类型移动文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
                Path oldPath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                Path newPath = Paths.get(targetPath, storageFileName);
                Files.createDirectories(newPath.getParent());
                Files.move(oldPath, newPath);
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储（先复制再删除）
                try {
                    // 尝试不同的文件扩展名找到原文件
                    String[] extensions = {"", ".txt", ".docx", ".pdf", ".jpg", ".png", ".jpeg"};
                    String foundStorageFileName = null;
                    
                    for (String ext : extensions) {
                        try {
                            String tempFileName = storageFileName + ext;
                            // 检查文件是否存在
                            minioClient.statObject(
                                    StatObjectArgs.builder()
                                            .bucket(fileStorageConfig.getBucketName())
                                            .object(tempFileName)
                                            .build()
                            );
                            foundStorageFileName = tempFileName;
                            break;
                        } catch (Exception ex) {
                            // 忽略错误，尝试下一个扩展名
                        }
                    }
                    
                    if (foundStorageFileName != null) {
                        // 构建新的存储路径
                        String newStoragePath = targetPath + "/" + foundStorageFileName;
                        // 移除开头的斜杠
                        if (newStoragePath.startsWith("/")) {
                            newStoragePath = newStoragePath.substring(1);
                        }
                        
                        // 复制文件
                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(newStoragePath)
                                        .source(
                                                CopySource.builder()
                                                        .bucket(fileStorageConfig.getBucketName())
                                                        .object(foundStorageFileName)
                                                        .build()
                                        )
                                        .build()
                        );
                        // 删除原文件
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(foundStorageFileName)
                                        .build()
                        );
                        System.out.println("MinIO文件移动成功: " + foundStorageFileName + " -> " + newStoragePath);
                    } else {
                        throw new RuntimeException("文件不存在: " + storageFileName);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("MinIO文件移动失败: " + e.getMessage(), e);
                }
            }

            // 更新文件元数据（如果数据库可用）
            try {
                if (fileMetadata != null) {
                    fileMetadata.setFilePath(targetPath);
                    fileMetadataService.updateMetadata(fileMetadata);
                }
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("更新文件元数据失败: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("文件移动失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String copy(String fileId, String targetPath) {
        try {
            // 尝试从数据库获取文件元数据
            com.javaee.fileservice.entity.FileMetadata fileMetadata = null;
            String storageFileName = null;
            String fileName = "copy_" + fileId + ".txt";
            
            try {
                fileMetadata = fileMetadataService.getMetadata(fileId);
                if (fileMetadata != null) {
                    storageFileName = fileMetadata.getObjectKey();
                    fileName = fileMetadata.getFileName();
                    System.out.println("从数据库获取到文件元数据，存储文件名: " + storageFileName);
                } else {
                    System.out.println("数据库中未找到文件元数据: " + fileId);
                }
            } catch (Exception e) {
                // 数据库不可用时，使用fileId作为默认值
                System.out.println("数据库不可用: " + e.getMessage());
                storageFileName = fileId;
            }

            // 如果没有存储文件名，使用fileId作为默认值
            if (storageFileName == null) {
                storageFileName = fileId;
            }

            // 生成新的文件ID
            String newFileId = UUID.randomUUID().toString();
            String newFileExtension = FileUtils.getFileExtension(fileName);
            String newStorageFileName = newFileId + (newFileExtension != null ? "." + newFileExtension : ".txt");

            // 根据存储类型复制文件
            if ("local".equals(fileStorageConfig.getStorageType())) {
                // 本地存储
                Path oldPath = Paths.get(fileStorageConfig.getLocalPath(), storageFileName);
                Path newPath = Paths.get(targetPath, newStorageFileName);
                Files.createDirectories(newPath.getParent());
                Files.copy(oldPath, newPath);
            } else if ("minio".equals(fileStorageConfig.getStorageType())) {
                // MinIO存储
                try {
                    // 尝试不同的文件扩展名找到原文件
                    String[] extensions = {"", ".txt", ".docx", ".pdf", ".jpg", ".png", ".jpeg"};
                    String foundStorageFileName = null;
                    
                    for (String ext : extensions) {
                        try {
                            String tempFileName = storageFileName + ext;
                            // 检查文件是否存在
                            minioClient.statObject(
                                    StatObjectArgs.builder()
                                            .bucket(fileStorageConfig.getBucketName())
                                            .object(tempFileName)
                                            .build()
                            );
                            foundStorageFileName = tempFileName;
                            break;
                        } catch (Exception ex) {
                            // 忽略错误，尝试下一个扩展名
                        }
                    }
                    
                    if (foundStorageFileName != null) {
                        // 构建新的存储路径
                        String newStoragePath = targetPath + "/" + newStorageFileName;
                        // 移除开头的斜杠
                        if (newStoragePath.startsWith("/")) {
                            newStoragePath = newStoragePath.substring(1);
                        }
                        
                        // 复制文件
                        minioClient.copyObject(
                                CopyObjectArgs.builder()
                                        .bucket(fileStorageConfig.getBucketName())
                                        .object(newStoragePath)
                                        .source(
                                                CopySource.builder()
                                                        .bucket(fileStorageConfig.getBucketName())
                                                        .object(foundStorageFileName)
                                                        .build()
                                        )
                                        .build()
                        );
                        System.out.println("MinIO文件复制成功: " + foundStorageFileName + " -> " + newStoragePath);
                    } else {
                        throw new RuntimeException("文件不存在: " + storageFileName);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("MinIO文件复制失败: " + e.getMessage(), e);
                }
            }

            // 保存新文件的元数据（如果数据库可用）
            try {
                if (fileMetadata != null) {
                    com.javaee.fileservice.entity.FileMetadata newFileMetadata = new com.javaee.fileservice.entity.FileMetadata();
                    newFileMetadata.setFileId(newFileId);
                    newFileMetadata.setFileName(fileName);
                    newFileMetadata.setOriginalFileName(fileName);
                    newFileMetadata.setFilePath(targetPath);
                    newFileMetadata.setFileType(fileMetadata.getFileType());
                    newFileMetadata.setFileSize(fileMetadata.getFileSize());
                    newFileMetadata.setStorageType(fileMetadata.getStorageType());
                    newFileMetadata.setBucketName(fileMetadata.getBucketName());
                    newFileMetadata.setObjectKey(newStorageFileName);
                    newFileMetadata.setCreateBy("system");
                    fileMetadataService.saveMetadata(newFileMetadata);
                }
            } catch (Exception e) {
                // 数据库不可用时，忽略错误
                System.out.println("保存新文件元数据失败: " + e.getMessage());
            }

            return newFileId;
        } catch (Exception e) {
            throw new RuntimeException("文件复制失败: " + e.getMessage(), e);
        }
    }

    /**
     * 确保MinIO存储桶存在
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

}
