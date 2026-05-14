# File Service 模块

## 功能概述

File Service 是 DocAI 项目的文件处理服务，提供文件上传、下载、存储管理等核心功能。

## 技术栈

- Spring Boot 3.1.8
- Spring Cloud 2023.0.3
- Spring Cloud Alibaba Nacos Discovery
- MyBatis-Plus 3.5.5
- MySQL 8.0+
- MinIO 8.5.2 (对象存储)
- Spring Security

## 核心功能

1. **文件上传**
   - 支持单文件上传
   - 支持多文件上传
   - 支持大文件分片上传
   - 支持文件类型校验
   - 支持文件大小限制

2. **文件下载**
   - 支持文件直接下载
   - 支持文件预览
   - 支持文件分片下载

3. **文件管理**
   - 支持文件删除
   - 支持文件重命名
   - 支持文件移动
   - 支持文件复制

4. **文件元数据**
   - 支持文件元数据查询
   - 支持文件列表查询
   - 支持文件搜索

5. **存储管理**
   - 支持本地存储
   - 支持 MinIO 存储
   - 支持存储策略配置

## 项目结构

```
file-service/
├── pom.xml                  # Maven构建文件
├── Dockerfile               # Docker构建文件
├── docker-compose.yml       # Docker Compose编排
├── .gitignore               # Git忽略文件
├── README.md                # 项目文档
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── javaee/
│   │   │           └── fileservice/
│   │   │               ├── FileServiceApplication.java  # 项目启动类
│   │   │               ├── config/                     # 配置类目录
│   │   │               ├── controller/                  # 接口层
│   │   │               ├── service/                    # 业务逻辑层
│   │   │               ├── mapper/                     # 数据访问层
│   │   │               ├── entity/                     # 数据实体
│   │   │               ├── dto/                        # 数据传输对象
│   │   │               ├── enums/                      # 枚举类
│   │   │               ├── exception/                  # 自定义异常
│   │   │               └── util/                       # 工具类
│   │   └── resources/
│   │       ├── application.yml                        # 主配置文件
│   │       ├── application-dev.yml                    # 开发环境配置
│   │       ├── application-prod.yml                   # 生产环境配置
│   │       ├── mapper/                                # MyBatis映射文件
│   │       ├── static/                                # 静态资源
│   │       └── templates/                             # 模板文件
│   └── test/
│       └── java/
│           └── com/
│               └── javaee/
│                   └── fileservice/                    # 测试目录
└── logs/                                               # 运行时日志目录
```

## 配置说明

### 1. 主配置文件 (application.yml)

- 服务器端口：8082
- 数据库连接配置
- MinIO 配置
- Nacos 服务发现配置

### 2. 环境配置文件

- application-dev.yml：开发环境配置
- application-prod.yml：生产环境配置

## API 接口

### 1. 文件上传接口
- `POST /api/files/upload` - 单文件上传
- `POST /api/files/upload-multiple` - 多文件上传
- `POST /api/files/upload-chunk` - 分片上传
- `POST /api/files/merge-chunk` - 分片合并

### 2. 文件下载接口
- `GET /api/files/download/{fileId}` - 文件下载
- `GET /api/files/preview/{fileId}` - 文件预览
- `GET /api/files/chunk/{fileId}` - 分片下载

### 3. 文件管理接口
- `DELETE /api/files/{fileId}` - 文件删除
- `PUT /api/files/{fileId}/rename` - 文件重命名
- `PUT /api/files/{fileId}/move` - 文件移动
- `PUT /api/files/{fileId}/copy` - 文件复制

### 4. 元数据接口
- `GET /api/files/metadata/{fileId}` - 获取文件元数据
- `GET /api/files/list` - 获取文件列表
- `GET /api/files/search` - 搜索文件

## 部署说明

### 1. 本地部署

```bash
# 启动服务
cd file-service
mvn spring-boot:run
```

### 2. Docker 部署

```bash
# 构建镜像
docker build -t file-service:1.0.0 .

# 运行容器
docker run -d --name file-service -p 8082:8082 file-service:1.0.0
```

### 3. Docker Compose 部署

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down
```

## 开发指南

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- MinIO 服务
- Nacos 服务

### 2. 数据库初始化

执行数据库脚本创建文件元数据表。

### 3. 代码规范

- 遵循 Java 代码规范
- 使用 Lombok 简化代码
- 使用 MyBatis-Plus 操作数据库
- 使用 Swagger 生成 API 文档

## 注意事项

1. **存储配置**
   - 生产环境中应配置 MinIO 集群
   - 应配置适当的存储策略和生命周期管理

2. **安全配置**
   - 生产环境中应启用 HTTPS
   - 应配置适当的文件访问权限
   - 应配置文件类型白名单

3. **性能优化**
   - 应配置文件缓存
   - 应配置数据库连接池
   - 应配置合适的文件分片大小

4. **监控配置**
   - 应配置文件服务监控
   - 应配置存储使用情况监控
   - 应配置文件操作日志

## 许可证

MIT License