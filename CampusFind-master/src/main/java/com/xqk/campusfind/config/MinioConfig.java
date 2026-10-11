package com.xqk.campusfind.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置。
 * 负责创建 {@link MinioClient} Bean，并在应用启动时确保目标桶存在且开启公开读，
 * 上传后的图片可通过 public-url 直链直接访问。
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 启动时初始化存储桶：不存在则创建，并写入公开读策略。
     * 使用 try-catch 包裹，避免 MinIO 未启动时导致整个应用无法启动，仅打印告警。
     */
    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient) {
        return args -> {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO 存储桶 [{}] 不存在，已自动创建。", bucket);
                }
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucket).config(publicReadPolicy(bucket)).build());
                log.info("MinIO 存储桶 [{}] 已就绪，并设置为公开读。", bucket);
            } catch (Exception e) {
                log.warn("MinIO 初始化存储桶失败（请确认 MinIO 服务已启动且凭证正确）：{}", e.getMessage());
            }
        };
    }

    /**
     * 生成允许匿名 GetObject 的桶策略 JSON。
     */
    private String publicReadPolicy(String bucketName) {
        return "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
                "      \"Action\": [\"s3:GetObject\"],\n" +
                "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
