# DocAI 项目文档

## 项目概述

DocAI 是一个基于 Spring Cloud 的文档智能处理系统，包含用户管理、文件处理、AI 分析和文档管理等功能模块。

## 项目结构

```
docAI/
├── common/                  # 公共组件模块
├── user-service/            # 用户服务模块
├── file-service/            # 文件服务模块
├── ai-service/              # AI 服务模块
├── document-service/        # 文档服务模块
├── gateway-service/         # API 网关模块
├── vue-test-app/            # Vue 前端测试应用
├── pom.xml                  # 父项目 POM 文件
└── readme.md                # 项目文档
```

## 技术栈

### 后端
- Spring Boot 3.2.0
- Spring Cloud 2023.0.3
- Spring Cloud Alibaba 2023.0.1.0
- MyBatis-Plus 3.5.5
- MySQL 8.0+
- Redis 7.0+ (集群)
- RabbitMQ
- JWT 0.11.5
- Swagger 2.3.0
- Spring AI

### 前端
- Vue 3
- Vite 4.5.3
- Axios

### 容器化
- Docker
- Docker Swarm

## 配置说明

### 1. 公共组件模块 (common)

#### 依赖配置
- Spring Boot Web
- JWT (用户鉴权)
- Redis (缓存)
- OpenFeign (服务间调用)
- Swagger (API 文档)

#### 核心配置类
- `SwaggerConfig.java` - 全局 Swagger 配置
- `WebMvcConfig.java` - Web MVC 配置
- `RedisConfig.java` - Redis 配置
- `FeignGlobalConfig.java` - Feign 全局配置
- `security/BaseSecurityConfig.java` - 基础安全配置（JWT认证）
- `security/JwtAuthenticationFilter.java` - JWT认证过滤器

#### 工具类
- `ValidateUtils` - 验证工具
- `RedisUtils` - Redis 工具
- `JwtUtils` - JWT 工具
- `FileUtils` - 文件工具
- `DateUtils` - 日期工具

#### 异常处理
- `BusinessException.java` - 业务异常
- `TokenException.java` - 令牌异常
- `GlobalExceptionHandler.java` - 全局异常处理器

### 2. 用户服务模块 (user-service)

#### 依赖配置
- 公共组件模块
- Spring Cloud Alibaba Nacos Discovery
- MyBatis-Plus
- MySQL 驱动
- Spring Security

#### 核心配置
- `application.yml` - 应用配置
  - 数据库连接配置
  - MyBatis-Plus 配置
  - 服务器端口 (8081)

#### 数据库配置
- 数据库：`doc_ai`
- 表结构：`user` 表
- 初始数据：admin 用户 (密码：${DOC_AI_ADMIN_PASSWORD}) 和 user 用户 (密码：${DOC_AI_USER_PASSWORD})

#### API 接口
- `POST /api/users/login` - 用户登录
- `POST /api/users/register` - 用户注册
- `GET /api/users/{id}` - 获取用户信息
- `POST /api/users/refresh` - 刷新令牌
- `POST /api/users/logout` - 用户登出

### 3. API 网关模块 (gateway-service)

#### 依赖配置
- Spring Cloud Gateway
- Spring Cloud Alibaba Nacos Discovery
- Spring Cloud Alibaba Sentinel (网关限流)
- Redis (用于限流)
- 公共组件模块 (JWT工具/统一返回)

#### 核心配置
- `application.yml` - 应用配置
  - 服务器端口 (8080)
  - Nacos服务发现配置
  - Redis配置
  - Sentinel配置
  - 健康检查和超时配置

#### 核心配置类
- `GatewayApplication.java` - 网关服务启动类
- `GatewayConfig.java` - 路由配置
- `GlobalCorsConfig.java` - 跨域配置
- `AuthGlobalFilter.java` - JWT鉴权全局过滤器

#### 路由规则
- `/api/users/**` → user-service
- `/api/files/**` → file-service
- `/api/ai/**` → ai-service
- `/api/documents/**` → document-service

#### 鉴权规则
- 白名单路径（无需鉴权）：
  - `/api/users/login` - 用户登录
  - `/api/users/register` - 用户注册
  - `/api/users/refresh` - 刷新令牌
- 其他路径需要携带有效的JWT令牌

#### 限流配置
- 使用Sentinel进行网关限流
- 使用Redis进行分布式限流计数

### 4. 文件服务模块 (file-service)

#### 依赖配置
- 公共组件模块
- Spring Cloud Alibaba Nacos Discovery
- MyBatis-Plus
- MySQL 驱动
- MinIO 客户端
- Spring Security

#### 核心配置
- `application.yml` - 应用配置
  - 服务器端口 (8082)
  - 数据库连接配置
  - MinIO 配置
  - Nacos服务发现配置

#### 核心功能
- 文件上传/下载/删除
- 大文件分片上传
- 文件元数据管理
- 多存储策略支持（本地/MinIO）
- 文件类型校验和大小限制

#### API 接口
- `POST /api/files/upload` - 文件上传
- `GET /api/files/download/{id}` - 文件下载
- `DELETE /api/files/{id}` - 文件删除
- `GET /api/files/metadata/{id}` - 获取文件元数据
- `GET /api/files/list` - 获取文件列表

### 5. AI 服务模块 (ai-service)

#### 依赖配置
- 公共组件模块
- Spring Cloud Alibaba Nacos Discovery
- Spring AI
- Redis (向量搜索)
- RabbitMQ

#### 核心配置
- `application.yml` - 应用配置
  - 服务器端口 (8083)
  - Nacos服务发现配置
  - AI 模型配置
  - Redis 向量搜索配置

#### 核心功能
- AI 文档摘要
- AI 文档纠错
- 关键词提取
- RAG 知识库系统
- AI Agent 架构

#### API 接口
- `POST /api/ai/summarize` - 文档摘要
- `POST /api/ai/correct` - 文档纠错
- `POST /api/ai/keywords` - 关键词提取
- `POST /api/ai/rag/query` - 知识库问答

### 5. 文档服务模块 (document-service)

#### 依赖配置
- 公共组件模块
- Spring Cloud Alibaba Nacos Discovery
- MyBatis-Plus
- MySQL 驱动
- Redis (缓存)
- Sentinel (限流熔断)

#### 核心配置
- `application.yml` - 应用配置
  - 服务器端口 (8084)
  - Nacos服务发现配置
  - Redis缓存配置

#### 核心功能
- 文档创建、更新、删除
- 文档版本控制
- 文档内容索引
- 文档搜索

#### API 接口
- `POST /api/documents` - 创建文档
- `PUT /api/documents` - 更新文档
- `DELETE /api/documents/{id}` - 删除文档
- `GET /api/documents/{id}` - 获取文档详情
- `GET /api/documents/user/{userId}` - 获取用户文档列表
- `GET /api/documents/search` - 搜索文档
- `GET /api/documents/{id}/versions` - 获取文档版本列表
- `POST /api/documents/{id}/restore/{versionNumber}` - 恢复文档版本

### 6. Nacos 服务配置

#### 运行方式
- 在 Docker Swarm 集群中运行
- 配置为高可用模式

#### 端口说明
- **8848**：HTTP 端口，用于 Web 控制台访问和部分 API 调用
- **9848**：gRPC 主端口，用于服务注册发现的实时通信
- **9849**：gRPC 客户端端口，用于客户端向服务端发送请求

> 注意：Nacos 2.0+ 版本必需映射 gRPC 端口，否则客户端无法正常连接

### 7. Docker 配置

#### user-service Dockerfile
```dockerfile
FROM swr.cn-north-4.myhuaweicloud.com/qxk/jdk-17:latest
WORKDIR /app
COPY target/user-service-1.0.0.jar /app/user-service.jar
EXPOSE 8081
CMD ["java", "-jar", "user-service.jar"]
```

#### gateway-service Dockerfile
```dockerfile
FROM swr.cn-north-4.myhuaweicloud.com/qxk/jdk-17:latest
WORKDIR /app
COPY target/gateway-service-1.0.0.jar /app/gateway-service.jar
EXPOSE 8080
CMD ["java", "-jar", "gateway-service.jar"]
```

#### file-service Dockerfile
```dockerfile
FROM swr.cn-north-4.myhuaweicloud.com/qxk/jdk-17:latest
WORKDIR /app
COPY target/file-service-1.0.0.jar /app/file-service.jar
EXPOSE 8082
CMD ["java", "-jar", "file-service.jar"]
```

#### ai-service Dockerfile
```dockerfile
FROM swr.cn-north-4.myhuaweicloud.com/qxk/jdk-17:latest
WORKDIR /app
COPY target/ai-service-1.0.0.jar /app/ai-service.jar
EXPOSE 8083
CMD ["java", "-jar", "ai-service.jar"]
```

### 8. 前端配置 (vue-test-app)

#### 依赖配置
- Vue 3
- Vite
- Axios

#### 核心配置
- `vite.config.js` - Vite 配置
  - 代理配置：`/api` → http://localhost:8080

#### 测试页面
- 登录页面
- 注册页面
- 用户信息页面

## 部署说明

### 1. 环境准备

#### 服务器配置

| 服务器 | CPU | 内存 | 磁盘 | IP 地址 | 角色 |
|--------|-----|------|------|---------|------|
| manager | 2核 | 16G | 40G | ${DOC_AI_MANAGER_HOST} | Manager + 存储节点 |
| node1 | 2核 | 4G | 40G | ${DOC_AI_NODE1_HOST} | Worker + 计算节点 |
| node2 | 2核 | 4G | 40G | ${DOC_AI_NODE2_HOST} | Worker + 计算节点 |
| node3 | 1核 | 2G | 40G | ${DOC_AI_NODE3_HOST} | Worker + 监控节点 |

#### 后端环境
- Docker 26.0+
- Docker Swarm
- JDK 17+ (用于构建)
- Maven 3.8+ (用于构建)

#### 前端环境
- Node.js 18+
- npm 9+

### 2. Docker Swarm 集群初始化

#### 1. 初始化 Swarm 集群

在 manager 节点 (${DOC_AI_MANAGER_HOST}) 上执行：

```bash
# 初始化 Swarm 集群
docker swarm init --advertise-addr ${DOC_AI_MANAGER_HOST}

# 获取加入命令
docker swarm join-token worker
```

#### 2. 其他节点加入集群

在 node1、node2、node3 上执行获取到的加入命令：

```bash
docker swarm join --token <token> ${DOC_AI_MANAGER_HOST}:2377
```

#### 3. 验证集群状态

```bash
docker node ls
```

### 3. 网络配置

#### 创建 overlay 网络

```bash
docker network create --driver overlay --attachable docai-network
```

### 4. 存储配置

#### 创建 Docker 卷

在 manager 节点上创建持久化卷：

```bash
# MySQL 数据卷
docker volume create mysql-data

# Redis 数据卷
docker volume create redis-data

# MinIO 数据卷
docker volume create minio-data

# Nacos 数据卷
docker volume create nacos-data
```

### 5. 服务部署

#### 1. 部署基础设施服务

创建 `docker-compose.yml` 文件：

```yaml
version: '3.8'

services:
  # MySQL 服务
  mysql:
    image: mysql:8.0
    deploy:
      placement:
        constraints: [node.hostname == manager]
      replicas: 1
      restart_policy:
        condition: on-failure
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: doc_ai
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - docai-network
    ports:
      - "3306:3306"

  # Redis 集群
  redis:
    image: redis:7.0
    deploy:
      mode: replicated
      replicas: 3
      placement:
        max_replicas_per_node: 1
      restart_policy:
        condition: on-failure
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis-data:/data
    networks:
      - docai-network
    ports:
      - "6379:6379"

  # RabbitMQ
  rabbitmq:
    image: rabbitmq:3.8-management
    deploy:
      placement:
        constraints: [node.hostname == manager]
      replicas: 1
      restart_policy:
        condition: on-failure
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_DEFAULT_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_DEFAULT_PASS}
    networks:
      - docai-network
    ports:
      - "5672:5672"
      - "15672:15672"

  # MinIO
  minio:
    image: minio/minio
    deploy:
      placement:
        constraints: [node.hostname == manager]
      replicas: 1
      restart_policy:
        condition: on-failure
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    command: server /data
    volumes:
      - minio-data:/data
    networks:
      - docai-network
    ports:
      - "9000:9000"
      - "9001:9001"

  # Nacos
  nacos:
    image: ${NACOS_USERNAME}/${NACOS_PASSWORD}-server:v2.2.0
    deploy:
      placement:
        constraints: [node.hostname == manager]
      replicas: 1
      restart_policy:
        condition: on-failure
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos_config
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: ${NACOS_MYSQL_PASSWORD}
    volumes:
      - nacos-data:/home/nacos/data
    networks:
      - docai-network
    ports:
      - "8848:8848"
      - "9848:9848"
      - "9849:9849"

networks:
  docai-network:
    external: true

volumes:
  mysql-data:
    external: true
  redis-data:
    external: true
  minio-data:
    external: true
  nacos-data:
    external: true
```

部署基础设施服务：

```bash
docker stack deploy -c docker-compose.yml docai-infra
```

#### 2. 构建微服务镜像

在本地构建微服务镜像：

```bash
# 构建所有服务
mvn clean package -DskipTests

# 构建并推送镜像
docker build -t docai/user-service:1.0.0 user-service/
docker build -t docai/gateway-service:1.0.0 gateway-service/
docker build -t docai/file-service:1.0.0 file-service/
docker build -t docai/ai-service:1.0.0 ai-service/
```

#### 3. 部署微服务

创建 `docker-compose-services.yml` 文件：

```yaml
version: '3.8'

services:
  # 网关服务
  gateway-service:
    image: docai/gateway-service:1.0.0
    deploy:
      mode: replicated
      replicas: 2
      placement:
        constraints: [node.role == worker]
      restart_policy:
        condition: on-failure
    environment:
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER-ADDR: nacos:8848
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD}
    networks:
      - docai-network
    ports:
      - "8080:8080"

  # 用户服务
  user-service:
    image: docai/user-service:1.0.0
    deploy:
      mode: replicated
      replicas: 2
      placement:
        constraints: [node.role == worker]
      restart_policy:
        condition: on-failure
    environment:
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER-ADDR: nacos:8848
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/doc_ai?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
    networks:
      - docai-network
    ports:
      - "8081:8081"

  # 文件服务
  file-service:
    image: docai/file-service:1.0.0
    deploy:
      mode: replicated
      replicas: 2
      placement:
        constraints: [node.role == worker]
      restart_policy:
        condition: on-failure
    environment:
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER-ADDR: nacos:8848
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/doc_ai?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}
      MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}
    networks:
      - docai-network
    ports:
      - "8082:8082"

  # AI 服务
  ai-service:
    image: docai/ai-service:1.0.0
    deploy:
      mode: replicated
      replicas: 1
      placement:
        constraints: [node.hostname == node2]
      restart_policy:
        condition: on-failure
    environment:
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER-ADDR: nacos:8848
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD}
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: ${RABBITMQ_USERNAME}
      SPRING_RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
    networks:
      - docai-network
    ports:
      - "8083:8083"

networks:
  docai-network:
    external: true
```

部署微服务：

```bash
docker stack deploy -c docker-compose-services.yml docai-services
```

### 6. 数据库初始化

#### SQL 初始化代码

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS doc_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 切换到 doc_ai 数据库
USE doc_ai;

-- 创建 user 表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
  `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `avatar` VARCHAR(255) COMMENT '头像',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `role` VARCHAR(20) DEFAULT 'user' COMMENT '角色：admin，user',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_username` (`username`),
  INDEX `idx_email` (`email`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入初始数据（密码使用 BCrypt 加密，明文分别为 ${DOC_AI_ADMIN_PASSWORD} 和 ${DOC_AI_USER_PASSWORD}）
INSERT INTO `user` (`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `status`, `role`) VALUES
('admin', '${DOC_AI_PASSWORD_HASH}', '${DOC_AI_ADMIN_EMAIL}', '${DOC_AI_ADMIN_PHONE}', '管理员', NULL, 0, 1, 'admin'),
('user', '${DOC_AI_PASSWORD_HASH}', '${DOC_AI_USER_EMAIL}', '${DOC_AI_USER_PHONE}', '测试用户', NULL, 0, 1, 'user');

-- 查看用户数据
SELECT id, username, email, phone, nickname, status, role, create_time FROM `user`;
```

### 7. 服务启动顺序

1. **基础设施服务**：MySQL → Redis → RabbitMQ → MinIO → Nacos
2. **微服务**：user-service → file-service → ai-service → gateway-service

### 8. 访问地址

- Nacos 控制台：http://${DOC_AI_MANAGER_HOST}:8848/nacos
- API 网关：http://${DOC_AI_MANAGER_HOST}:8080
- User Service Swagger UI：http://${DOC_AI_MANAGER_HOST}:8081/swagger-ui.html
- File Service Swagger UI：http://${DOC_AI_MANAGER_HOST}:8082/swagger-ui.html
- AI Service Swagger UI：http://${DOC_AI_MANAGER_HOST}:8083/swagger-ui.html
- MinIO 控制台：http://${DOC_AI_MANAGER_HOST}:9001
- RabbitMQ 控制台：http://${DOC_AI_MANAGER_HOST}:15672

## 测试方法

### 1. 使用 Swagger UI 测试 API

#### 测试用户服务 API
1. 访问：http://${DOC_AI_MANAGER_HOST}:8081/swagger-ui.html
2. 点击 `user-controller` 展开接口列表
3. 测试 `POST /api/users/register` 注册新用户
4. 测试 `POST /api/users/login` 登录获取令牌
5. 测试 `GET /api/users/{id}` 获取用户信息

#### 测试文件服务 API
1. 访问：http://${DOC_AI_MANAGER_HOST}:8082/swagger-ui.html
2. 点击 `file-controller` 展开接口列表
3. 测试 `POST /api/files/upload` 上传文件
4. 测试 `GET /api/files/download/{id}` 下载文件
5. 测试 `DELETE /api/files/{id}` 删除文件
6. 测试 `GET /api/files/metadata/{id}` 获取文件元数据
7. 测试 `GET /api/files/list` 获取文件列表

#### 测试 AI 服务 API
1. 访问：http://${DOC_AI_MANAGER_HOST}:8083/swagger-ui.html
2. 测试 `POST /api/ai/summarize` 文档摘要
3. 测试 `POST /api/ai/correct` 文档纠错
4. 测试 `POST /api/ai/keywords` 关键词提取

### 2. 使用前端应用测试

1. 构建前端应用：
   ```bash
   cd vue-test-app
   npm install
   npm run build
   ```

2. 部署前端应用（可使用 Nginx 或直接部署到容器）

3. 访问前端应用，测试注册、登录和用户信息功能

## 监控与维护

### 1. 集群监控

- **Docker Swarm 监控**：使用 Portainer 或 Docker Dashboard
- **服务监控**：集成 Prometheus + Grafana
- **日志管理**：使用 ELK 栈或 Loki

### 2. 常见问题

#### 1. Nacos 服务无法访问
- 检查 Nacos 容器是否运行：`docker service ps docai-infra_nacos`
- 检查端口映射：`docker service inspect docai-infra_nacos | grep -A 20 "Ports"`
- 检查网络连接：`docker exec -it <nacos-container-id> ping mysql`

#### 2. API 网关路由失败
- 检查 Nacos 服务是否运行
- 检查微服务是否注册到 Nacos
- 检查路由配置是否正确

#### 3. 前端请求 500 错误
- 检查 API 网关是否运行
- 检查对应微服务是否运行
- 检查数据库连接是否正常
- 查看服务日志获取详细错误信息：`docker service logs docai-services_user-service`

#### 4. 密码验证失败
- 确认使用明文密码登录（系统会自动加密验证）
- 检查数据库中密码是否正确存储
- 检查 `UserServiceImpl` 中的密码验证逻辑

#### 5. 网关服务返回503错误
**问题描述**：通过网关访问API时返回503 Service Unavailable错误，但直接访问微服务正常。

**原因分析**：
- 网关服务缺少Spring Cloud LoadBalancer依赖，无法支持`lb://`协议
- 版本兼容性问题：Spring Cloud 2023.0.3依赖于Spring Boot 3.2.0中引入的RestClient类

**解决方案**：
1. **升级Spring Boot版本**：
   - 修改父POM文件中的Spring Boot版本从3.1.8到3.2.0
   - 同时更新`spring-boot.version`属性值

2. **添加Spring Cloud LoadBalancer依赖**：
   ```xml
   <!-- 在gateway-service的pom.xml中添加 -->
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-loadbalancer</artifactId>
   </dependency>
   ```

3. **重新构建并部署服务**：
   ```bash
   # 构建网关服务
   cd gateway-service
   mvn clean package -DskipTests
   
   # 重新构建Docker镜像
   docker build -t docai/gateway-service:1.0.0 .
   
   # 重新部署服务
   docker service update --image docai/gateway-service:1.0.0 docai-services_gateway-service
   ```

## 注意事项

1. **安全配置**
   - 生产环境中应修改默认密码
   - 生产环境中应启用 HTTPS
   - 生产环境中应配置更严格的 CORS 规则
   - 统一安全配置：所有服务使用 common 模块中的基础安全配置，确保安全性一致

2. **性能优化**
   - 生产环境中应配置 Redis 缓存
   - 生产环境中应配置数据库连接池
   - 生产环境中应配置日志级别

3. **容器化部署**
   - 生产环境中应配置容器健康检查
   - 生产环境中应配置容器资源限制
   - 生产环境中应使用 Docker Secret 管理敏感信息

4. **高可用性**
   - 确保所有关键服务都有多个副本
   - 配置适当的重启策略
   - 定期备份数据

## 许可证

MIT License