package com.tradex.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradex.service.S3Service;
import com.tradex.service.Trade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final S3Service s3Service;

    public FileUploadController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload/json")
    public ResponseEntity<?> uploadJson(@Valid @RequestBody Trade trade) {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        trade.setUserId(userId);
        trade.setUploadTimestamp(System.currentTimeMillis());

        String key = s3Service.uploadTrade(trade);

        return ResponseEntity.ok(Map.of(
                "message", "File uploaded successfully",
                "s3Key", key,
                "userId", userId
        ));

    }
}
