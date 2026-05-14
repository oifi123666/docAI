# File Download Skill

## 功能说明
使用skill实现从minIO下载文件到本地的功能，支持两种方式：
1. 直接下载文件流
2. 获取预签名访问URL

## API接口

### 直接下载文件
- **接口地址**: GET /api/ai/download
- **功能**: 从minIO服务器下载文件到本地
- **请求参数**:
  - bucketName: 存储桶名称（可选，默认使用documents）
  - objectName: 对象名称（文件路径，必填）
- **返回结果**: 文件流（application/octet-stream）

### 获取文件访问URL
- **接口地址**: GET /api/ai/download/url
- **功能**: 获取minIO中文件的预签名访问URL
- **请求参数**:
  - bucketName: 存储桶名称（可选，默认使用documents）
  - objectName: 对象名称（文件路径，必填）
- **返回结果**:
  - fileUrl: 文件访问URL
  - bucketName: 存储桶名称
  - objectName: 对象名称
  - expirySeconds: 过期时间（秒）

## 依赖服务

### MinIOService
minIO服务接口，提供文件下载、获取URL等操作。

**待实现**: 需要实现minIO客户端的具体功能，包括：
1. 添加minIO依赖到pom.xml
2. 在application.yml中添加minIO配置
3. 实现minIO客户端的初始化
4. 实现所有接口方法的具体逻辑

## 配置说明

在application.yml中添加：
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: documents
  url:
    expiry: 3600  # URL过期时间（秒）
```

## 使用示例

### 直接下载文件
```bash
# 下载文件
curl -X GET "http://localhost:8080/api/ai/download?objectName=2026/04/15/abc123def456.txt" \
  --output downloaded.txt
```

### 获取文件URL
```bash
# 获取预签名URL
curl -X GET "http://localhost:8080/api/ai/download/url?objectName=2026/04/15/abc123def456.txt"
```

响应示例：
```json
{
  "code": 200,
  "data": {
    "fileUrl": "http://minio-url/documents/2026/04/15/abc123def456.txt?token=xxx",
    "bucketName": "documents",
    "objectName": "2026/04/15/abc123def456.txt",
    "expirySeconds": 3600
  },
  "message": "success"
}
```
