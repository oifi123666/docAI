FROM swr.cn-north-4.myhuaweicloud.com/qxk/jdk-17:latest
WORKDIR /app
COPY target/file-service-1.0.0.jar /app/file-service.jar
EXPOSE 8082
CMD ["java", "-jar", "file-service.jar"]