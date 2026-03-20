package com.tradex.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tradex.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final S3Service s3Service;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public FileUploadController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    // Option 1: Multipart file upload
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();

            // Read file content, inject userId, analyze validation
            String json = new String(file.getBytes());
            String enrichedJson = enrichAndValidateJson(json, userId);

            String key = s3Service.uploadJsonString(enrichedJson);
            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "s3Key", key,
                    "userId", userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process uploaded file"));
        }
    }

    // Option 2: Raw JSON body
    @PostMapping("/upload/json")
    public ResponseEntity<?> uploadJson(@RequestBody String jsonBody) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();

            String enrichedJson = enrichAndValidateJson(jsonBody, userId);

            String key = s3Service.uploadJsonString(enrichedJson);
            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "s3Key", key,
                    "userId", userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process JSON payload"));
        }
    }

    /**
     * Performs strict real-time standard validations across all the required fields.
     */
    private void validateTradeNode(ObjectNode node) {
        if (!node.hasNonNull("tradeType") || (!node.get("tradeType").asText().equals("BUY") && !node.get("tradeType").asText().equals("SELL"))) {
            throw new IllegalArgumentException("Validation failed: 'tradeType' must be strictly 'BUY' or 'SELL'");
        }
        if (!node.hasNonNull("portfolioId") || node.get("portfolioId").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: 'portfolioId' is required and cannot be blank");
        }
        if (!node.hasNonNull("brokerId") || node.get("brokerId").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: 'brokerId' is required and cannot be blank");
        }
        if (!node.hasNonNull("symbol") || !node.get("symbol").asText().matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Validation failed: 'symbol' is required and must contain only letters and numbers");
        }
        if (!node.hasNonNull("quantity") || !node.get("quantity").isNumber() || node.get("quantity").asInt() <= 0) {
            throw new IllegalArgumentException("Validation failed: 'quantity' must be a numeric integer greater than 0");
        }
        if (!node.hasNonNull("price") || !node.get("price").isNumber() || node.get("price").asDouble() <= 0.0) {
            throw new IllegalArgumentException("Validation failed: 'price' must be a positive number greater than 0");
        }
        if (!node.hasNonNull("currency") || !node.get("currency").asText().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Validation failed: 'currency' must be exactly exactly 3 uppercase letters (e.g. INR, USD)");
        }
        if (!node.hasNonNull("tradeDate") || !node.get("tradeDate").asText().matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new IllegalArgumentException("Validation failed: 'tradeDate' is required and must match format YYYY-MM-DD");
        }
        if (!node.hasNonNull("exchange") || node.get("exchange").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: 'exchange' is required and cannot be blank");
        }
        if (!node.hasNonNull("clientId") || node.get("clientId").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Validation failed: 'clientId' is required and cannot be blank");
        }
    }

    /**
     * Validates the trade input and injects the authenticated userId into the trade JSON before uploading to S3.
     */
    private String enrichAndValidateJson(String json, String userId) throws IOException {
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);
        
        validateTradeNode(node);
        
        node.put("userId", userId);
        node.put("uploadTimestamp", System.currentTimeMillis());
        return objectMapper.writeValueAsString(node);
    }
}
