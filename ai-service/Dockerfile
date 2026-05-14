FROM swr.cn-north-4.myhuaweicloud.com/qxk/jdk-17:latest

# 维护者信息
MAINTAINER qxk

# 设定时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone

# 复制jar包到容器内
COPY target/ai-service-1.0.0.jar /app/ai-service.jar

# 设定工作目录
WORKDIR /app

# 暴露端口
EXPOSE 8083

# 启动应用
CMD ["java", "-jar", "ai-service.jar"]