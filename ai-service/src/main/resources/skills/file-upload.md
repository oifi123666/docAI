# File Upload Skill

## 功能说明
使用skill实现文件上传至minIO服务器的功能。

## API接口

### 文件上传
- **接口地址**: POST /api/ai/upload
- **功能**: 将本地文件上传至minIO服务器
- **请求参数**:
  - file: 上传的文件（MultipartFile，必填）
  - bucketName: 存储桶名称（可选，默认使用documents）
  - objectName: 对象名称（文件路径，可选，自动生成）
- **返回结果**:
  - fileUrl: 文件访问URL
  - bucketName: 存储桶名称
  - objectName: 对象名称
  - originalFilename: 原始文件名
  - fileSize: 文件大小
  - contentType: 文件类型
  - uploadTime: 上传时间戳

## 依赖服务

### MinIOService
minIO服务接口，提供文件上传、下载、删除等操作。

**待实现**: 需要实现minIO客户端的具体功能，包括：
1. 添加minIO依赖到pom.xml
2. 在application.yml中添加minIO配置
3. 实现minIO客户端的初始化
4. 实现所有接口方法的具体逻辑

## 文件命名规则
自动生成的文件路径格式：yyyy/MM/dd/UUID_原文件名

例如：2026/04/15/abc123def456.txt
