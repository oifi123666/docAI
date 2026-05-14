# AI 服务模块 README

## 模块概述

AI 服务模块是 DocAI 项目的核心智能处理模块，基于 Spring AI 框架实现，提供文档智能分析、处理和协作功能。该模块负责对接外部 AI 服务，实现文档摘要、纠错、关键词提取等功能，并扩展支持 AI Agent 架构，包括 RAG 知识库系统、对话功能和 AIOps 功能。

## 技术栈

- Spring Boot 3.2.0
- Spring Cloud 2023.0.3
- Spring AI
- Spring AI Alibaba
- Redis 7.0+ (集群)
- RabbitMQ
- 向量数据库 (Redis 向量搜索)
- ChatGLM-6B API
- 讯飞星火 API

## 核心功能

### 1. AI Agent 架构

#### Knowledge Index Agent
- 负责文档向量化和知识库索引
- 支持文档自动分类和标签生成
- 实现文档内容的结构化提取

#### Chat ReAct Agent
- 基于 ReAct 模式实现对话功能
- 支持多轮对话和上下文管理
- 实现业务咨询、告警自救、工单预处理等场景

#### Plan-Execute-Replan Agent
- 基于 Plan-Execute 模式实现智能规划
- 支持复杂任务的分解和执行
- 实现自动调整和优化执行计划

### 2. RAG 知识库系统

- 文档向量化存储和检索
- 基于内部知识库的智能问答
- 支持增量学习和知识更新
- 提供准确的业务咨询和技术支持

### 3. 对话功能

- 基于 ReAct 模式的对话 Agent
- 支持多场景无缝切换
- 一次开发覆盖研发、运维、业务多角色需求
- 提供自然语言交互界面

### 4. AIOps 功能

- 基于 Plan-Execute 模式的智能运维 Agent
- 自动故障检测和处理
- 将运维响应时间从小时级降低到分钟级
- 支持系统监控和告警管理

## 架构设计

### 模块结构

```
ai-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/javaee/aiservice/
│   │   │       ├── agent/            # AI Agent 实现
│   │   │       │   ├── KnowledgeIndexAgent.java
│   │   │       │   ├── ChatReactAgent.java
│   │   │       │   └── PlanExecuteAgent.java
│   │   │       ├── rag/              # RAG 知识库系统
│   │   │       │   ├── DocumentVectorizer.java
│   │   │       │   ├── KnowledgeBase.java
│   │   │       │   └── VectorStore.java
│   │   │       ├── conversation/     # 对话功能
│   │   │       │   ├── ConversationManager.java
│   │   │       │   └── ContextManager.java
│   │   │       ├── aiops/            # AIOps 功能
│   │   │       │   ├── MonitoringService.java
│   │   │       │   └── FaultDetector.java
│   │   │       ├── config/           # 配置类
│   │   │       │   ├── AiConfig.java
│   │   │       │   └── VectorStoreConfig.java
│   │   │       ├── controller/       # API 控制器
│   │   │       │   ├── AiController.java
│   │   │       │   ├── AgentController.java
│   │   │       │   └── RagController.java
│   │   │       ├── service/          # 业务服务
│   │   │       │   ├── impl/
│   │   │       │   ├── AiService.java
│   │   │       │   ├── AgentService.java
│   │   │       │   └── RagService.java
│   │   │       └── AiServiceApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application.properties
│   └── test/
├── Dockerfile
└── pom.xml
```

### 服务调用流程

1. **客户端请求**：通过 API 网关发送 AI 处理请求
2. **路由转发**：网关将请求转发到 AI 服务
3. **Agent 处理**：根据请求类型选择合适的 Agent 处理
4. **知识库查询**：如需知识库信息，调用 RAG 系统
5. **AI 模型调用**：调用外部 AI API 进行处理
6. **结果返回**：将处理结果返回给客户端

## API 接口

### 1. AI 处理接口

- `POST /api/ai/summarize` - 文档摘要
- `POST /api/ai/correct` - 文档纠错
- `POST /api/ai/keywords` - 关键词提取
- `POST /api/ai/analyze` - 文档分析

### 2. Agent 接口

- `POST /api/ai/agent/chat` - 对话 Agent
- `POST /api/ai/agent/plan` - 规划 Agent
- `POST /api/ai/agent/knowledge` - 知识索引 Agent

### 3. RAG 接口

- `POST /api/ai/rag/index` - 文档索引
- `POST /api/ai/rag/search` - 知识库搜索
- `POST /api/ai/rag/query` - 知识库问答

### 4. AIOps 接口

- `POST /api/ai/aiops/monitor` - 系统监控
- `POST /api/ai/aiops/detect` - 故障检测
- `POST /api/ai/aiops/resolve` - 故障处理

## 配置说明

### 1. 应用配置

在 `application.yml` 中配置以下内容：

```yaml
spring:
  # AI 配置
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    huggingface:
      api-key: ${HUGGINGFACE_API_KEY}
  # 向量数据库配置
  vector:
    store:
      type: redis
      redis:
        host: ${REDIS_HOST}
        port: ${REDIS_PORT}
        password: ${REDIS_PASSWORD}
  # 消息队列配置
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

### 2. 环境变量

| 环境变量 | 描述 | 默认值 |
|---------|------|--------|
| OPENAI_API_KEY | OpenAI API 密钥 | - |
| HUGGINGFACE_API_KEY | Hugging Face API 密钥 | - |
| REDIS_HOST | Redis 主机地址 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| REDIS_PASSWORD | Redis 密码 | - |
| RABBITMQ_HOST | RabbitMQ 主机地址 | localhost |
| RABBITMQ_PORT | RabbitMQ 端口 | 5672 |
| RABBITMQ_USERNAME | RabbitMQ 用户名 | ${RABBITMQ_USERNAME} |
| RABBITMQ_PASSWORD | RabbitMQ 密码 | ${RABBITMQ_PASSWORD} |

## 部署说明

### 1. 依赖服务

- Redis 7.0+ (集群)
- RabbitMQ 3.8+
- MySQL 8.0+
- Nacos 2.2.0+

### 2. 构建与运行

#### 构建

```bash
cd ai-service
mvn clean package -DskipTests
```

#### 运行

```bash
# 本地运行
java -jar target/ai-service-1.0.0.jar

# Docker 运行
docker build -t ai-service:1.0.0 .
docker run --name ai-service -d \
  --network docai-network \
  -p 8083:8083 \
  -e OPENAI_API_KEY=${OPENAI_API_KEY} \
  -e REDIS_HOST=redis \
  ai-service:1.0.0
```

### 3. 集成到现有架构

1. **服务注册**：AI 服务自动注册到 Nacos 服务发现中心
2. **API 网关**：通过 Gateway 服务暴露 AI 服务接口
3. **数据存储**：使用 MySQL 存储元数据，Redis 存储缓存和向量数据
4. **消息队列**：使用 RabbitMQ 实现异步处理

## 监控与维护

### 1. 监控指标

- AI 服务调用成功率
- 文档处理响应时间
- 知识库查询命中率
- Agent 执行成功率

### 2. 日志管理

- 服务日志：记录服务运行状态和错误信息
- AI 调用日志：记录 AI API 调用和响应
- 性能日志：记录系统性能指标

### 3. 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| AI API 调用失败 | API 密钥无效或网络问题 | 检查 API 密钥和网络连接 |
| 知识库搜索无结果 | 文档未索引或向量相似度低 | 重新索引文档或调整相似度阈值 |
| Agent 执行超时 | 任务复杂度高或 AI 响应慢 | 优化任务分解或增加超时时间 |
| 内存使用过高 | 向量数据量大 | 增加内存或优化向量存储 |

## 未来规划

1. **扩展 Agent 类型**：增加更多专业领域的 Agent
2. **优化 RAG 系统**：提升知识库检索准确性和效率
3. **增强对话能力**：支持更多场景和更自然的对话
4. **完善 AIOps 功能**：增加更多自动化运维能力
5. **集成更多 AI 模型**：支持更多开源和商业 AI 模型

## 技术文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/docs/current/reference/html/)
- [Redis 向量搜索文档](https://redis.io/docs/stack/search/reference/vectors/)
- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [ChatGLM-6B API 文档](https://open.bigmodel.cn/doc/api)
- [讯飞星火 API 文档](https://www.xfyun.cn/doc/spark/introduction.html)