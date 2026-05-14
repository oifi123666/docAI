#!/bin/bash

# 构建和部署脚本

echo "=== DocAI 项目部署脚本 ==="

# 设置变量
MANAGER_IP="${DOC_AI_MANAGER_HOST}"
NODE1_IP="${DOC_AI_NODE1_HOST}"
NODE2_IP="${DOC_AI_NODE2_HOST}"
NODE3_IP="${DOC_AI_NODE3_HOST}"

# 步骤 1: 构建项目
echo "\n步骤 1: 构建项目..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "构建失败，退出脚本"
    exit 1
fi

echo "项目构建成功！"

# 步骤 2: 构建 Docker 镜像
echo "\n步骤 2: 构建 Docker 镜像..."

# 构建用户服务镜像
docker build -t docai/user-service:1.0.0 user-service/
if [ $? -ne 0 ]; then
    echo "构建 user-service 镜像失败"
    exit 1
fi

# 构建文件服务镜像
docker build -t docai/file-service:1.0.0 file-service/
if [ $? -ne 0 ]; then
    echo "构建 file-service 镜像失败"
    exit 1
fi

# 构建网关服务镜像
docker build -t docai/gateway-service:1.0.0 gateway-service/
if [ $? -ne 0 ]; then
    echo "构建 gateway-service 镜像失败"
    exit 1
fi

# 构建 AI 服务镜像
docker build -t docai/ai-service:1.0.0 ai-service/
if [ $? -ne 0 ]; then
    echo "构建 ai-service 镜像失败"
    exit 1
fi

# 构建文档服务镜像
docker build -t docai/document-service:1.0.0 document-service/
if [ $? -ne 0 ]; then
    echo "构建 document-service 镜像失败"
    exit 1
fi

echo "Docker 镜像构建成功！"

# 步骤 3: 初始化 Docker Swarm 集群
echo "\n步骤 3: 初始化 Docker Swarm 集群..."

# 检查是否已初始化 Swarm
SWARM_STATUS=$(docker info --format '{{.Swarm.LocalNodeState}}')

if [ "$SWARM_STATUS" != "active" ]; then
    echo "初始化 Docker Swarm 集群..."
    docker swarm init --advertise-addr $MANAGER_IP
    
    if [ $? -ne 0 ]; then
        echo "初始化 Swarm 集群失败"
        exit 1
    fi
    
    echo "Swarm 集群初始化成功！"
    echo "请在其他节点上执行以下命令加入集群："
    docker swarm join-token worker
else
    echo "Swarm 集群已初始化，跳过此步骤"
fi

# 步骤 4: 创建网络和数据卷
echo "\n步骤 4: 创建网络和数据卷..."

# 创建网络
if [ -z "$(docker network ls | grep docai-network)" ]; then
    docker network create --driver overlay --attachable docai-network
    echo "创建网络 docai-network 成功"
else
    echo "网络 docai-network 已存在，跳过此步骤"
fi

# 创建数据卷
for volume in mysql-data redis-data minio-data nacos-data; do
    if [ -z "$(docker volume ls | grep $volume)" ]; then
        docker volume create $volume
        echo "创建数据卷 $volume 成功"
    else
        echo "数据卷 $volume 已存在，跳过此步骤"
    fi
done

# 步骤 5: 部署基础设施服务
echo "\n步骤 5: 部署基础设施服务..."
docker stack deploy -c docker-compose-infra.yml docai-infra

if [ $? -ne 0 ]; then
    echo "部署基础设施服务失败"
    exit 1
fi

echo "基础设施服务部署成功！"
echo "等待 30 秒，让服务启动..."
sleep 30

# 步骤 6: 数据库初始化
echo "\n步骤 6: 数据库初始化..."

# 检查 MySQL 服务是否运行
MYSQL_STATUS=$(docker service ps docai-infra_mysql | grep Running | wc -l)

if [ $MYSQL_STATUS -eq 1 ]; then
    echo "MySQL 服务运行正常，执行数据库初始化..."
    
    # 执行 SQL 脚本
    docker exec -i $(docker ps -qf name=docai-infra_mysql) mysql -u"${MYSQL_USERNAME}" -p"${MYSQL_PASSWORD}" << EOF
CREATE DATABASE IF NOT EXISTS doc_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE doc_ai;

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

INSERT INTO `user` (`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `status`, `role`) VALUES
('admin', '${DOC_AI_PASSWORD_HASH}', '${DOC_AI_ADMIN_EMAIL}', '${DOC_AI_ADMIN_PHONE}', '管理员', NULL, 0, 1, 'admin'),
('user', '${DOC_AI_PASSWORD_HASH}', '${DOC_AI_USER_EMAIL}', '${DOC_AI_USER_PHONE}', '测试用户', NULL, 0, 1, 'user');
EOF
    
    echo "数据库初始化成功！"
else
    echo "MySQL 服务未运行，跳过数据库初始化"
fi

# 步骤 7: 部署微服务
echo "\n步骤 7: 部署微服务..."
docker stack deploy -c docker-compose-services.yml docai-services

if [ $? -ne 0 ]; then
    echo "部署微服务失败"
    exit 1
fi

echo "微服务部署成功！"
echo "等待 30 秒，让服务启动..."
sleep 30

# 步骤 8: 验证部署
echo "\n步骤 8: 验证部署..."

# 检查服务状态
echo "\n检查基础设施服务状态："
docker service ls | grep docai-infra

echo "\n检查微服务状态："
docker service ls | grep docai-services

# 显示访问地址
echo "\n=== 部署完成 ==="
echo "访问地址："
echo "- Nacos 控制台：http://$MANAGER_IP:8848/nacos"
echo "- API 网关：http://$MANAGER_IP:8080"
echo "- User Service Swagger UI：http://$MANAGER_IP:8081/swagger-ui.html"
echo "- File Service Swagger UI：http://$MANAGER_IP:8082/swagger-ui.html"
echo "- AI Service Swagger UI：http://$MANAGER_IP:8083/swagger-ui.html"
echo "- Document Service Swagger UI：http://$MANAGER_IP:8084/swagger-ui.html"
echo "- MinIO 控制台：http://$MANAGER_IP:9001"
echo "- RabbitMQ 控制台：http://$MANAGER_IP:15672"

echo "\n部署脚本执行完成！"
