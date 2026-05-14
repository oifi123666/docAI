FROM swr.cn-north-4.myhuaweicloud.com/qxk/jdk-17:latest

# 维护者信息
MAINTAINER qxk

# 设定时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone

# 复制jar包到容器内
COPY target/user-service-1.0.0.jar /app/user-service.jar

# 设定工作目录
WORKDIR /app

# 暴露端口
EXPOSE 8081

# 启动应用（添加参数，禁用MyBatis-Plus的ddlApplicationRunner）
CMD ["java", "-Dmybatis-plus.global-config.db-config.ddl-auto=none", "-jar", "user-service.jar"]