package com.javaee.documentservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 文档服务应用启动类
 */
@SpringBootApplication(scanBasePackages = {"com.javaee.documentservice", "com.javaee.common"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan(basePackages = "com.javaee.documentservice.mapper")
@EnableAsync
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DocumentServiceApplication.class);
        Environment environment = application.run(args).getEnvironment();
        System.out.println("Application name: " + environment.getProperty("spring.application.name"));
        System.out.println("Server port: " + environment.getProperty("server.port"));
    }

}
