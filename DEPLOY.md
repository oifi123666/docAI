# DocAI 项目部署指南

## 项目概述

DocAI 是一个基于 Spring Cloud 的文档智能处理系统，包含用户管理、文件处理、AI 分析和文档管理等功能模块。本指南将帮助您在 Docker Swarm 集群上部署该项目。

## 服务器配置

| 服务器 | CPU | 内存 | 磁盘 | IP 地址 | 角色 |
|--------|-----|------|------|---------|------|
| manager | 2核 | 16G | 40G | ${DOC_AI_MANAGER_HOST} | Manager + 存储节点 |
| node1 | 2核 | 4G | 40G | ${DOC_AI_NODE1_HOST} | Worker + 计算节点 |
| node2 | 2核 | 4G | 40G | ${DOC_AI_NODE2_HOST} | Worker + 计算节点 |
| node3 | 1核 | 2G | 40G | ${DOC_AI_NODE3_HOST} | Worker + 监控节点 |

## 环境准备

### 1. 安装必要软件

在所有服务器上安装以下软件：

- **Docker 26.0+**
  ```bash
  # Ubuntu/Debian
  apt-get update && apt-get install -y docker.io
  
  # CentOS/RHEL
  yum install -y docker
  
  # 启动 Docker 服务
  systemctl start docker
  systemctl enable docker
  ```

- **JDK 17**（仅在构建服务器上需要）
  ```bash
  # Ubuntu/Debian
  apt-get install -y openjdk-17-jdk
  
  # CentOS/RHEL
  yum install -y java-17-openjdk
  ```

- **Maven 3.8+**（仅在构建服务器上需要）
  ```bash
  # Ubuntu/Debian
  apt-get install -y maven
  
  # CentOS/RHEL
  yum install -y maven
  ```

## 部署步骤

### 1. 克隆代码

在 manager 节点上克隆项目代码：

```bash
git clone <repository-url>
cd docAI
```

### 2. 构建和部署

使用提供的部署脚本进行构建和部署：

```bash
# 给脚本添加执行权限
chmod +x deploy.sh

# 执行部署脚本
./deploy.sh
```

部署脚本将执行以下步骤：

1. **构建项目**：使用 Maven 构建所有模块
2. **构建 Docker 镜像**：为每个微服务构建 Docker 镜像
3. **初始化 Docker Swarm 集群**：在 Server 1 上初始化 Swarm 集群
4. **创建网络和数据卷**：创建必要的网络和数据卷
5. **部署基础设施服务**：部署 MySQL、Redis、RabbitMQ、MinIO 和 Nacos
6. **数据库初始化**：创建数据库和表结构，插入初始数据
7. **部署微服务**：部署网关服务、用户服务、文件服务、AI 服务和文档服务
8. **验证部署**：检查服务状态并显示访问地址

### 3. 其他节点加入集群

部署脚本执行过程中，会显示其他节点加入 Swarm 集群的命令。在 node1、node2、node3 上执行该命令：

```bash
docker swarm join --token <token> ${DOC_AI_MANAGER_HOST}:2377
```

## 服务访问

部署完成后，可以通过以下地址访问各个服务：

- **Nacos 控制台**：http://${DOC_AI_MANAGER_HOST}:8848/nacos
- **API 网关**：http://${DOC_AI_MANAGER_HOST}:8080
- **User Service Swagger UI**：http://${DOC_AI_MANAGER_HOST}:8081/swagger-ui.html
- **File Service Swagger UI**：http://${DOC_AI_MANAGER_HOST}:8082/swagger-ui.html
- **AI Service Swagger UI**：http://${DOC_AI_MANAGER_HOST}:8083/swagger-ui.html
- **Document Service Swagger UI**：http://${DOC_AI_MANAGER_HOST}:8084/swagger-ui.html
- **MinIO 控制台**：http://${DOC_AI_MANAGER_HOST}:9001
- **RabbitMQ 控制台**：http://${DOC_AI_MANAGER_HOST}:15672

## 默认账号

- **MySQL**：${MYSQL_USERNAME} / ${MYSQL_PASSWORD}
- **Nacos**：${NACOS_USERNAME} / ${NACOS_PASSWORD}
- **MinIO**：${MINIO_ACCESS_KEY} / ${MINIO_SECRET_KEY}
- **RabbitMQ**：${RABBITMQ_USERNAME} / ${RABBITMQ_PASSWORD}
- **系统用户**：
  - 管理员：admin / ${DOC_AI_ADMIN_PASSWORD}
  - 普通用户：user / ${DOC_AI_USER_PASSWORD}

## 常见问题处理

### 1. 服务无法启动

- **检查服务日志**：
  ```bash
  docker service logs <service-name>
  ```

- **检查网络连接**：确保所有服务在同一个网络中
  ```bash
  docker network inspect docai-network
  ```

- **检查依赖服务**：确保依赖的服务（如 MySQL、Redis、Nacos）已正常启动
  ```bash
  docker service ps docai-infra_mysql
  docker service ps docai-infra_redis
  docker service ps docai-infra_nacos
  ```

### 2. 数据库连接失败

- **检查 MySQL 服务是否运行**：
  ```bash
  docker service ps docai-infra_mysql
  ```

- **检查数据库配置是否正确**：检查服务的环境变量配置
  ```bash
  docker service inspect docai-services_user-service | grep -A 20 "Environment"
  ```

- **检查网络连接是否正常**：
  ```bash
  docker exec -it $(docker ps -qf name=docai-services_user-service) ping mysql
  ```

### 3. 服务注册失败

- **检查 Nacos 服务是否运行**：
  ```bash
  docker service ps docai-infra_nacos
  ```

- **检查服务配置中的 Nacos 地址是否正确**：
  ```bash
  docker service inspect docai-services_user-service | grep -A 10 "SPRING_CLOUD_NACOS"
  ```

- **检查网络连接是否正常**：
  ```bash
  docker exec -it $(docker ps -qf name=docai-services_user-service) ping nacos
  ```

### 4. 网关路由失败

- **检查微服务是否注册到 Nacos**：访问 Nacos 控制台，查看服务列表

- **检查网关配置中的路由规则是否正确**：
  ```bash
  docker service inspect docai-services_gateway-service | grep -A 20 "SPRING_CLOUD"
  ```

- **检查 Spring Cloud LoadBalancer 依赖是否添加**：确保网关服务的 pom.xml 中添加了 LoadBalancer 依赖

## 扩展与维护

### 1. 扩展服务实例

要扩展某个服务的实例数量，可以使用以下命令：

```bash
docker service scale docai-services_gateway-service=3
```

### 2. 更新服务

要更新服务的镜像版本，可以使用以下命令：

```bash
docker service update --image docai/gateway-service:1.0.1 docai-services_gateway-service
```

### 3. 监控服务

- **查看服务状态**：
  ```bash
  docker service ls
  ```

- **查看服务日志**：
  ```bash
  docker service logs -f docai-services_user-service
  ```

- **查看容器状态**：
  ```bash
  docker ps
  ```

### 4. 备份数据

- **备份 MySQL 数据**：
  ```bash
  docker exec $(docker ps -qf name=docai-infra_mysql) mysqldump -uroot -p"${MYSQL_PASSWORD}" doc_ai > doc_ai_backup.sql
  ```

- **备份 MinIO 数据**：
  ```bash
  docker cp $(docker ps -qf name=docai-infra_minio):/data /path/to/backup
  ```

## 注意事项

1. **生产环境配置**：
   - 修改默认密码
   - 启用 HTTPS
   - 配置更严格的 CORS 规则
   - 使用 Docker Secret 管理敏感信息

2. **性能优化**：
   - 配置 Redis 缓存
   - 配置数据库连接池
   - 配置适当的容器资源限制

3. **高可用性**：
   - 确保所有关键服务都有多个副本
   - 配置适当的重启策略
   - 定期备份数据

4. **安全性**：
   - 限制容器网络访问
   - 定期更新 Docker 镜像
   - 监控异常访问

## 许可证

MIT License