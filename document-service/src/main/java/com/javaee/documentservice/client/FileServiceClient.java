package com.javaee.documentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 文件服务客户端
 * 通过OpenFeign调用file服务的API
 */
@FeignClient(name = "file", url = "${file.service.url:http://file:8082}")
public interface FileServiceClient {

    /**
     * 根据文件ID下载文件内容
     * @param fileId 文件ID
     * @return 文件内容字节数组
     */
    @GetMapping("/api/files/download/{fileId}")
    ResponseEntity<byte[]> downloadFile(@PathVariable("fileId") String fileId);

    /**
     * 根据文件ID获取文件名
     * @param fileId 文件ID
     * @return 文件名
     */
    @GetMapping("/api/files/info/{fileId}/name")
    ResponseEntity<String> getFileName(@PathVariable("fileId") String fileId);

    /**
     * 根据文件ID获取文件类型
     * @param fileId 文件ID
     * @return 文件类型（content-type）
     */
    @GetMapping("/api/files/info/{fileId}/type")
    ResponseEntity<String> getFileType(@PathVariable("fileId") String fileId);
}