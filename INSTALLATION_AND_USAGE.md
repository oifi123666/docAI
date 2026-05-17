# DocAI 文档智能处理系统 - 安装与使用指南

## 一、项目概述

DocAI 是一个基于 Spring Cloud 的企业级文档智能处理系统，包含用户管理、文件存储、AI 分析、文档管理、智能 Agent 等功能模块，采用 Docker Swarm 集群部署。

## 二、环境准备

### 2.1 硬件配置

| 服务器角色 | CPU | 内存 | 磁盘 |
|----------|-----|------|------|
| Manager 节点 | 2核+ | 16G+ | 40G+ |
| Worker 节点 | 2核+ | 4G+ | 40G+ |
| 监控节点 | 1核+ | 2G+ | 40G+ |

### 2.2 软件环境

**所有节点需要安装：**
- **Docker 26.0+** - 容器运行环境
- 操作系统支持：Ubuntu/Debian/CentOS/RHEL

**仅构建服务器需要安装：**
- **JDK 17** - Java 开发工具包
- **Maven 3.8+** - 项目构建工具

### 2.3 软件安装示例

```bash
# 安装 Docker
apt-get update && apt-get install -y docker.io
systemctl start docker
systemctl enable docker

# 安装 JDK 17（仅构建服务器）
apt-get install -y openjdk-17-jdk

# 安装 Maven（仅构建服务器）
apt-get install -y maven
```

## 三、项目安装

### 3.1 克隆项目

```bash
cd ~
git clone <项目仓库地址>
cd docai/doc-ai
```

### 3.2 一键部署

项目提供了自动化部署脚本 `deploy.sh`，可以一站式完成构建与部署：

```bash
# 添加脚本执行权限
chmod +x deploy.sh

# 执行部署
./deploy.sh
```

### 3.3 部署步骤详解

脚本会自动完成以下步骤：

1. **项目构建** - 使用 Maven 打包所有模块
2. **镜像构建** - 为各微服务构建 Docker 镜像
3. **Swarm 初始化** - 在 Manager 节点初始化集群
4. **资源创建** - 创建网络和数据卷
5. **基础设施部署** - MySQL、Redis、RabbitMQ、MinIO、Nacos
6. **数据库初始化** - 创建表和初始数据
7. **微服务部署** - 网关、用户、文件、AI、文档服务
8. **部署验证** - 检查服务状态，显示访问地址

### 3.4 节点加入集群

脚本执行过程中会显示其他节点加入命令，在 Worker 节点上执行：

```bash
docker swarm join --token <token> <manager-ip>:2377
```

## 四、服务访问

部署完成后，可以通过以下地址访问各服务：

| 服务名称 | 访问地址 | 默认账号密码 |
|--------|--------|-----------|
| Nacos 控制台 | http://manager-ip:8848/nacos | nacos/nacos |
| API 网关 | http://manager-ip:8080 | - |
| 用户服务 Swagger | http://manager-ip:8081/swagger-ui.html | - |
| 文件服务 Swagger | http://manager-ip:8082/swagger-ui.html | - |
| AI 服务 Swagger | http://manager-ip:8083/swagger-ui.html | - |
| 文档服务 Swagger | http://manager-ip:8084/swagger-ui.html | - |
| MinIO 控制台 | http://manager-ip:9001 | ${MINIO_ROOT_USER}/${MINIO_ROOT_PASSWORD} |
| RabbitMQ 控制台 | http://manager-ip:15672 | guest/guest |
| Grafana 监控 | http://manager-ip:3000 | admin/${DOC_AI_ADMIN_PASSWORD} |

### 4.1 系统测试账号

- **管理员**：admin / ${DOC_AI_ADMIN_PASSWORD}
- **普通用户**：user / ${DOC_AI_USER_PASSWORD}

## 五、功能使用

### 5.1 用户管理

1. **注册/登录** - 通过用户服务接口注册账号
2. **权限控制** - 管理员和普通用户角色分离
3. **信息管理** - 支持头像、昵称等信息修改

### 5.2 文件管理

1. **文件上传** - 支持单文件、多文件、大文件分片上传
2. **文件下载/预览** - 直接下载或在线预览
3. **文件管理** - 删除、重命名、移动、复制操作
4. **存储切换** - 支持本地存储或 MinIO 分布式存储

### 5.3 文档处理

1. **文档创建** - 上传文件自动解析内容
2. **文档编辑** - 版本管理，支持历史回溯
3. **智能分析** - AI 摘要、关键词提取、纠错
4. **分类标签** - 文档分类和标签管理

### 5.4 AI 智能功能

1. **对话 Agent** - 基于 ReAct 模式的多轮对话
2. **规划 Agent** - 复杂任务自动分解与执行
3. **RAG 知识库** - 内部知识检索与问答
4. **AIOps 运维** - 系统监控和故障检测

## 六、运维管理

### 6.1 查看服务状态

```bash
# 查看所有服务
docker service ls

# 查看特定服务日志
docker service logs -f docai-services_user-service

# 查看容器
docker ps
```

### 6.2 服务扩缩容

```bash
# 扩展服务实例
docker service scale docai-services_gateway-service=3
```

### 6.3 更新服务

```bash
# 更新镜像
docker service update --image docai/user-service:1.0.1 docai-services_user-service
```

### 6.4 数据备份

```bash
# MySQL 数据备份
docker exec $(docker ps -qf name=docai-infra_mysql) mysqldump -uroot -p123456 doc_ai > backup.sql

# MinIO 数据备份
docker cp $(docker ps -qf name=docai-infra_minio):/data /path/to/backup
```

## 七、常见问题

### 7.1 服务无法启动

检查服务日志和依赖服务状态
```bash
docker service logs docai-services_user-service
docker service ps docai-infra_mysql
```

### 7.2 数据库连接失败

检查网络连接和环境变量
```bash
docker service inspect docai-services_user-service | grep -A 20 "Environment"
```

### 7.3 服务注册失败

确认 Nacos 服务正常运行
```bash
docker service ps docai-infra_nacos
```

### 7.4 网关路由失败

检查 Nacos 服务列表和网关配置

## 八、注意事项

1. **生产安全** - 修改默认密码，启用 HTTPS，使用 Docker Secrets 管理敏感信息
2. **性能优化** - 配置 Redis 缓存，调整连接池大小，限制容器资源
3. **高可用** - 多副本部署，定期备份数据，合理配置重启策略
4. **监控告警** - 配置 Prometheus 告警规则，定期监控系统状态

---


