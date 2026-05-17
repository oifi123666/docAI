package com.javaee.aiservice.rag;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文档向量化器
 * 负责将文档内容转换为向量表示
 * 适配阿里云百炼dashscope的Embedding API
 */
@Component
public class DocumentVectorizer {

    private static final Logger log = LoggerFactory.getLogger(DocumentVectorizer.class);

    @Value("${spring.ai.dashscope.embedding.api-key:${spring.ai.dashscope.api-key}}")
    private String apiKey;

    @Value("${spring.ai.dashscope.embedding.model:text-embedding-v4}")
    private String model;

    @Value("${spring.ai.dashscope.embedding.dimension:1024}")
    private int dimension;

    /**
     * 将文本向量化
     * @param text 文本内容
     * @return 向量表示
     */
    public float[] vectorize(String text) {
        log.debug("开始向量化文本，长度={}", text.length());

        try {
            // 构建请求参数
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .model(model)
                    .apiKey(apiKey)
                    .texts(Arrays.asList(text))
                    .build();

            // 创建模型实例并调用
            TextEmbedding textEmbedding = new TextEmbedding();
            TextEmbeddingResult result = textEmbedding.call(param);

            // 输出结果
            System.out.println("========== Embedding响应内容开始 ==========");
            System.out.println(result);
            System.out.println("========== Embedding响应内容结束 ==========");

            // 解析结果
            if (result != null && result.getOutput() != null 
                    && result.getOutput().getEmbeddings() != null 
                    && !result.getOutput().getEmbeddings().isEmpty()) {
                
                List<Double> embeddingList = result.getOutput().getEmbeddings().get(0).getEmbedding();
                float[] embedding = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    embedding[i] = embeddingList.get(i).floatValue();
                }
                
                System.out.println("解析到向量维度: " + embedding.length);
                return embedding;
            }

            throw new RuntimeException("Embedding API返回结果为空");

        } catch (ApiException | NoApiKeyException e) {
            log.error("调用Embedding API失败", e);
            throw new RuntimeException("文档向量化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量向量化
     * @param texts 文本列表
     * @return 向量列表
     */
    public float[][] vectorizeBatch(String[] texts) {
        log.info("批量向量化，数量={}", texts.length);

        try {
            float[][] result = new float[texts.length][];
            for (int i = 0; i < texts.length; i++) {
                result[i] = vectorize(texts[i]);
            }
            log.info("批量向量化完成");
            return result;
        } catch (Exception e) {
            log.error("批量向量化失败", e);
            throw new RuntimeException("批量向量化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取向量维度
     * @return 向量维度
     */
    public int getVectorDimension() {
        try {
            float[] sample = vectorize("test");
            return sample.length;
        } catch (Exception e) {
            log.warn("获取向量维度失败，使用默认值", e);
            return dimension;
        }
    }
}