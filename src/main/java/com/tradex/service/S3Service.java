package com.tradex.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    /**
     * Uploads a file to S3 and returns the generated key.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // Always ensure .json extension so the S3 trigger fires
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".json")) {
            originalName = (originalName != null ? originalName : "trade") + ".json";
        }
        String key = UUID.randomUUID() + "_" + originalName;

        // Read bytes first to guarantee content is captured
        byte[] bytes = file.getBytes();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/json");
        metadata.setContentLength(bytes.length);
        System.out.println("File size: " + file.getSize());
        System.out.println("Bytes length: " + bytes.length);
        s3Client.putObject(bucket, key, new ByteArrayInputStream(bytes), metadata);

        return key;
    }

    /**
     * Uploads raw JSON string to S3 and returns the generated key.
     */
    public String uploadJsonString(String json) {
        String key = UUID.randomUUID() + "_trade.json";
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/json");
        metadata.setContentLength(bytes.length);

        s3Client.putObject(bucket, key, new ByteArrayInputStream(bytes), metadata);

        return key;
    }
}
