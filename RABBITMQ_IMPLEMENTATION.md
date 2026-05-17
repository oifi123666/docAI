# RabbitMQ 消息队列功能实现文档

## 概述

本文档记录了在 doc-ai 项目中 RabbitMQ 消息队列功能的完整实现。

## 实现内容

### 1. 统一配置

- 所有服务的 RabbitMQ 账号统一为 `guest/guest`
- 主机地址为 `infra_rabbitmq:5672`
- 配置文件位置：各个服务的 `application.yml`

### 2. 服务配置

#### ai-service
- **配置类**：`RabbitMQConfig.java`
- **交换机**：`ai.exchange` (Direct Exchange)
- **队列**：
  - `ai.task.queue` - AI 任务队列
  - `ai.alert.queue` - AI 告警队列
- **路由键**：
  - `task`
  - `alert`
- **注意**：本服务不需要监听器

#### file-service
- **配置类**：`RabbitMQConfig.java`
- **交换机**：`file.exchange` (Topic Exchange)
- **队列**：
  - `file.upload.queue` - 文件上传队列
  - `file.download.queue` - 文件下载队列
  - `file.process.queue` - 文件处理队列
  - `file.delete.queue` - 文件删除队列
- **路由键**：
  - `file.upload`
  - `file.download`
  - `file.process`
  - `file.delete`
- **工具类**：`RabbitMQUtil.java`
- **监听器**：`FileMessageListener.java`
- **集成位置**：`FileController.java` - 在上传、下载、删除时发送消息

#### gateway-service
- **配置类**：`RabbitMQConfig.java`
- **交换机**：`gateway.exchange` (Topic Exchange)
- **队列**：
  - `gateway.log.queue` - 网关日志队列
  - `gateway.alert.queue` - 网关告警队列
- **路由键**：
  - `gateway.log`
  - `gateway.alert`
- **工具类**：`RabbitMQUtil.java`
- **监听器**：`GatewayMessageListener.java`
- **集成位置**：`AuthGlobalFilter.java` - 在请求时发送日志和告警消息

#### user-service
- **配置类**：`RabbitMQConfig.java`
- **交换机**：`user.exchange` (Topic Exchange)
- **队列**：
  - `user.register.queue` - 用户注册队列
  - `user.password.reset.queue` - 密码重置队列
  - `user.operate.log.queue` - 用户操作日志队列
- **路由键**：
  - `user.register`
  - `user.password.reset`
  - `user.operate.log`
- **工具类**：`RabbitMQUtil.java`
- **监听器**：`UserMessageListener.java`
- **集成位置**：`UserController.java` - 在注册、登录时发送消息

## 验证方法

### 前置条件

1. 确保 Docker 基础设施已启动：
   ```bash
   cd d:\doc_ai\doc-ai
   docker-compose -f docker-compose-infra.yml up -d
   ```

2. 访问 RabbitMQ 管理界面验证服务状态：
   - URL: http://localhost:15672
   - 用户名: guest
   - 密码: guest
   - 确认 Queues 和 Exchanges 标签页能正常访问

### 测试流程

#### 1. 启动服务

首先确保所有服务正常启动（顺序很重要）：
1. 基础设施服务（MySQL、Redis、RabbitMQ、MinIO、Nacos）
2. 应用服务（user-service、file-service、ai-service、gateway-service）

#### 2. 验证 user-service

1. 访问用户注册接口：
   ```
   POST http://localhost:8080/api/users/register
   Content-Type: application/json
   
   {
     "username": "testuser",
     "password": "Test123456",
     "email": "test@example.com",
     "phone": "${DOC_AI_ADMIN_PHONE}"
   }
   ```

2. 查看 user-service 控制台日志，确认：
   - 发送用户注册消息到 RabbitMQ
   - 发送用户操作日志消息到 RabbitMQ
   - 用户注册消息监听器接收到消息
   - 用户操作日志消息监听器接收到消息

#### 3. 验证 file-service

1. 访问文件上传接口：
   ```
   POST http://localhost:8080/api/files/upload
   Content-Type: multipart/form-data
   file: [选择一个测试文件]
   ```

2. 查看 file-service 控制台日志，确认：
   - 发送文件上传消息到 RabbitMQ
   - 文件上传消息监听器接收到消息

#### 4. 验证 gateway-service

1. 访问任意 API 接口，例如：
   ```
   GET http://localhost:8080/api/users/register
   ```

2. 查看 gateway-service 控制台日志，确认：
   - 网关收到请求
   - 发送网关日志消息到 RabbitMQ
   - 网关日志消息监听器接收到消息

#### 5. 验证 RabbitMQ 管理界面

1. 访问 http://localhost:15672
2. 登录后查看 **Queues** 标签页，确认所有队列都已创建
3. 查看每个队列的 **Messages** 统计，确认有消息被处理
4. 查看 **Exchanges** 标签页，确认所有交换机都已创建

## 消息格式

### 用户注册消息
```json
{
  "userId": 1,
  "username": "testuser",
  "email": "test@example.com",
  "timestamp": "2026-04-27T10:00:00"
}
```

### 用户操作日志消息
```json
{
  "userId": 1,
  "operation": "REGISTER",
  "description": "用户 testuser 注册成功",
  "timestamp": "2026-04-27T10:00:00"
}
```

### 文件上传消息
```json
{
  "fileId": "uuid",
  "fileName": "test.txt",
  "fileSize": 1024,
  "userId": "1",
  "timestamp": "2026-04-27T10:00:00"
}
```

### 网关日志消息
```json
{
  "path": "/api/users/register",
  "method": "POST",
  "userId": "1",
  "timestamp": "2026-04-27T10:00:00"
}
```

### 网关告警消息
```json
{
  "alertType": "UNAUTHORIZED",
  "description": "缺少有效的认证令牌: /api/files/upload",
  "timestamp": "2026-04-27T10:00:00"
}
```

## 下一步扩展建议

1. **持久化**：将监听器接收到的消息持久化到数据库
2. **邮件通知**：在用户注册和密码重置时发送真实邮件
3. **重试机制**：添加消息重试和死信队列处理
4. **监控告警**：添加消息队列监控和告警
5. **消息确认**：完善消息确认机制（ACK/NACK）

## 技术要点

- 使用 Spring AMQP 提供的 `@RabbitListener` 注解实现消息监听
- 通过 `RabbitTemplate` 发送消息
- 使用 Exchange 和 Queue 的绑定关系实现路由
- 消息使用 Map<String, Object> 格式，支持 JSON 序列化

## 注意事项

1. RabbitMQ 默认账号为 guest/guest，仅适用于开发环境
2. 生产环境应使用更安全的账号密码
3. 所有队列都配置为持久化（durable）
4. 监听器使用 `@Slf4j` 记录日志，方便调试
