# File Management Skill

## 功能说明
使用skill实现完整的文件管理功能，包括文件删除、回收站、文件版本管理等，所有功能都通过MCP进行权限验证。

## API接口

### 删除文件
- **接口地址**: DELETE /api/ai/files
- **功能**: 删除文件，支持确认删除和回收站
- **请求参数**:
  - bucketName: 存储桶名称（可选）
  - objectName: 对象名称（文件路径，必填）
  - requireConfirmation: 是否需要确认（true/false，默认true）
  - confirmationToken: 确认token（确认时使用）
- **返回结果**:
  - status: 状态（pending等待确认，recycle已移至回收站）
  - confirmationToken: 确认token（需要确认时返回）
  - confirmationExpiry: 确认过期时间
  - recycleId: 回收站记录ID（移至回收站时返回）
  - message: 消息

### 恢复文件
- **接口地址**: POST /api/ai/files/restore
- **功能**: 从回收站恢复文件
- **请求参数**:
  - recycleId: 回收站记录ID
  - bucketName: 存储桶名称（可选）
  - objectName: 对象名称（可选）
  - newObjectName: 恢复到的新对象名称（可选）
- **返回结果**:
  - status: 状态
  - fileUrl: 文件访问URL
  - bucketName: 存储桶名称
  - objectName: 对象名称
  - message: 消息

### 获取回收站文件列表
- **接口地址**: GET /api/ai/recycle-bin
- **功能**: 获取回收站中的文件列表
- **请求参数**:
  - bucketName: 存储桶名称（可选）
- **返回结果**:
  - totalCount: 文件总数
  - files: 文件列表
    - recycleId: 回收站记录ID
    - bucketName: 存储桶名称
    - originalObjectName: 原始对象名称
    - deleteTime: 删除时间
    - expiryTime: 过期时间
    - fileSize: 文件大小
    - deleter: 删除者

### 获取文件版本列表
- **接口地址**: GET /api/ai/files/versions
- **功能**: 获取文件的所有版本
- **请求参数**:
  - bucketName: 存储桶名称（可选）
  - objectName: 对象名称（必填）
  - versionId: 版本号（可选）
- **返回结果**:
  - bucketName: 存储桶名称
  - objectName: 对象名称
  - currentVersionId: 当前版本号
  - versions: 版本列表
    - versionId: 版本号
    - isCurrent: 是否为当前版本
    - createTime: 创建时间
    - fileSize: 文件大小
    - uploader: 上传者
    - remark: 版本备注

### 切换文件版本
- **接口地址**: POST /api/ai/files/versions/switch
- **功能**: 切换到指定版本
- **请求参数**:
  - bucketName: 存储桶名称（可选）
  - objectName: 对象名称（必填）
  - targetVersionId: 目标版本号（必填）
- **返回结果**:
  - versionId: 版本号
  - isCurrent: 是否为当前版本
  - createTime: 创建时间
  - fileSize: 文件大小
  - uploader: 上传者
  - remark: 版本备注

## 依赖服务

### MinIOService
minIO服务接口，提供文件上传、下载、删除等操作。

**待实现**: 需要实现minIO客户端的具体功能，包括：
1. 添加minIO依赖到pom.xml
2. 在application.yml中添加minIO配置
3. 实现minIO客户端的初始化
4. 实现所有接口方法的具体逻辑

### MCP服务
**待实现**: 需要集成MCP进行权限验证，包括：
1. 权限验证
2. 操作审批
3. 审计记录
4. 用户身份管理

## 配置说明

在application.yml中添加：
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: documents
  url:
    expiry: 3600
  recycle:
    expiry-days: 7
  delete:
    confirmation-timeout: 300
```

## 功能特性

1. **确认删除**: 重要文件删除前需要确认，防止误删
2. **回收站**: 删除的文件先移至回收站，在有效期内可以恢复
3. **文件版本**: 同一文件多次上传时记录每个版本，方便查看和切换
4. **权限控制**: 通过MCP进行权限验证，确保安全
5. **审计记录**: 所有删除操作都有详细的审计记录

## 使用示例

### 删除文件（需要确认）
```bash
# 请求删除
curl -X DELETE "http://localhost:8080/api/ai/files?objectName=2026/04/15/abc123def456.txt"
```

响应示例：
```json
{
  "code": 200,
  "data": {
    "status": "pending",
    "confirmationToken": "xxx-xxx-xxx",
    "confirmationExpiry": 1234567890,
    "message": "请确认删除操作"
  },
  "message": "success"
}
```

### 确认删除
```bash
# 确认删除
curl -X DELETE "http://localhost:8080/api/ai/files?objectName=2026/04/15/abc123def456.txt&confirmationToken=xxx-xxx-xxx"
```

### 从回收站恢复文件
```bash
# 恢复文件
curl -X POST "http://localhost:8080/api/ai/files/restore" \
  -H "Content-Type: application/json" \
  -d '{
    "recycleId": "recycle-id"
  }'
```

### 获取文件版本列表
```bash
# 获取版本列表
curl -X GET "http://localhost:8080/api/ai/files/versions?objectName=2026/04/15/abc123def456.txt"
```

### 切换文件版本
```bash
# 切换版本
curl -X POST "http://localhost:8080/api/ai/files/versions/switch" \
  -H "Content-Type: application/json" \
  -d '{
    "objectName": "2026/04/15/abc123def456.txt",
    "targetVersionId": "version-id"
  }'
```
