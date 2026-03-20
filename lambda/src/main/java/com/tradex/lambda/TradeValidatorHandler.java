package com.tradex.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradex.lambda.model.Trade;
import com.tradex.lambda.validator.TradeValidator;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Lambda handler triggered by S3 ObjectCreated events.
 * Reads the uploaded JSON -> validates (field + business rules) -> stores in
 * DynamoDB.
 *
 * Covers ALL validations from Validations_Trade.txt:
 * - Field validations (via TradeValidator)
 * - Duplicate trade detection (content hash)
 * - SELL without BUY (no holdings)
 * - Oversell (sell qty > available qty)
 * - Partial sell (allowed if qty <= available)
 * - Idempotency (same S3 event delivered multiple times)
 */
public class TradeValidatorHandler implements RequestHandler<S3Event, String> {

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    private final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.defaultClient();
    private final AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final TradeValidator validator = new TradeValidator();

    private final String tableName = System.getenv("DYNAMODB_TABLE") != null
            ? System.getenv("DYNAMODB_TABLE")
            : "Trades";

    private final String snsTopicArn = System.getenv("SNS_TOPIC_ARN") != null
            ? System.getenv("SNS_TOPIC_ARN")
            : "tradex-validated-trades-topic";

    @Override
    public String handleRequest(S3Event event, Context context) {
        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getUrlDecodedKey();

            context.getLogger().log("Processing file: s3://" + bucket + "/" + key);

            try {
                // --- Edge Case #11: Idempotency - skip if S3 key already processed ---
                if (isAlreadyProcessed(key)) {
                    context.getLogger().log("SKIPPED (idempotency): " + key + " was already processed");
                    return "SKIPPED_DUPLICATE_EVENT";
                }

                // 1. Read JSON from S3
                InputStream stream = s3Client.getObject(bucket, key).getObjectContent();
                byte[] rawBytes = stream.readAllBytes();
                String jsonContent = new String(rawBytes, StandardCharsets.UTF_8);
                Trade trade = mapper.readValue(jsonContent, Trade.class);

                // 2. Field-level validation
                List<String> errors = validator.validate(trade);

                // Validate userId is present (injected by Spring Boot)
                if (trade.getUserId() == null || trade.getUserId().isBlank()) {
                    errors.add("userId is required (must be set by the uploading service)");
                }

                if (!errors.isEmpty()) {
                    context.getLogger().log("Validation failed for " + key + ": " + errors);
                    return "VALIDATION_FAILED: " + errors;
                }

                // --- Edge Case #1: Duplicate Trade Upload (same content) ---
                String contentHash = computeHash(jsonContent);
                if (isDuplicateContent(contentHash)) {
                    context.getLogger().log("REJECTED (duplicate): trade content already exists, hash=" + contentHash);
                    return "REJECTED_DUPLICATE_TRADE";
                }

                // --- Edge Cases #5, #6, #7: SELL validation against holdings ---
                if ("SELL".equals(trade.getTradeType())) {
                    int availableQty = getAvailableQuantity(trade.getUserId(), trade.getPortfolioId(),
                            trade.getSymbol());

                    // #5: SELL without any BUY
                    if (availableQty <= 0) {
                        String msg = "No holdings available for " + trade.getSymbol()
                                + " in portfolio " + trade.getPortfolioId();
                        context.getLogger().log("REJECTED: " + msg);
                        return "REJECTED: " + msg;
                    }

                    // #7: Oversell
                    if (trade.getQuantity() > availableQty) {
                        String msg = "Cannot sell " + trade.getQuantity() + " " + trade.getSymbol()
                                + ". Available: " + availableQty;
                        context.getLogger().log("REJECTED (oversell): " + msg);
                        return "REJECTED_OVERSELL: " + msg;
                    }

                    // #6: Partial sell is allowed (qty <= available) — just log it
                    if (trade.getQuantity() < availableQty) {
                        int remaining = availableQty - trade.getQuantity();
                        context.getLogger().log("Partial sell: " + trade.getQuantity() + " of "
                                + availableQty + " " + trade.getSymbol() + ". Remaining: " + remaining);
                    }
                }

                // 3. Generate trade ID
                String tradeId = generateTradeId();
                trade.setTradeId(tradeId);

                // 4. Save to DynamoDB (with contentHash and s3Key for future checks)
                saveToDynamoDB(trade, contentHash, key);
                context.getLogger().log("Trade saved to DynamoDB: " + tradeId);

                // 5. Publish to SNS
                if (snsTopicArn != null && !snsTopicArn.isEmpty()) {
                    String tradeJson = mapper.writeValueAsString(trade);
                    snsClient.publish(new PublishRequest(snsTopicArn, tradeJson));
                    context.getLogger().log("Trade published to SNS: " + tradeId);
                } else {
                    context.getLogger().log("Warning: SNS_TOPIC_ARN not set. Skipping SNS publish.");
                }

                return "SUCCESS: " + tradeId;

            } catch (Exception e) {
                context.getLogger().log("Error processing " + key + ": " + e.getMessage());
                return "ERROR: " + e.getMessage();
            }
        }
        return "NO_RECORDS";
    }

    /**
     * Edge Case #11: Check if this S3 key was already processed (idempotency).
     * Scans DynamoDB for any item with matching s3Key.
     */
    private boolean isAlreadyProcessed(String s3Key) {
        ScanRequest request = new ScanRequest()
                .withTableName(tableName)
                .withFilterExpression("s3Key = :sk")
                .withExpressionAttributeValues(Map.of(":sk", new AttributeValue(s3Key)))
                .withLimit(1);
        ScanResult result = dynamoDB.scan(request);
        return !result.getItems().isEmpty();
    }

    /**
     * Edge Case #1: Check if a trade with identical content already exists.
     * Uses SHA-256 hash of the JSON content.
     */
    private boolean isDuplicateContent(String contentHash) {
        ScanRequest request = new ScanRequest()
                .withTableName(tableName)
                .withFilterExpression("contentHash = :ch")
                .withExpressionAttributeValues(Map.of(":ch", new AttributeValue(contentHash)))
                .withLimit(1);
        ScanResult result = dynamoDB.scan(request);
        return !result.getItems().isEmpty();
    }

    /**
     * Calculate net available quantity for a stock in a
     * portfolio.
     * SUM(BUY quantities) - SUM(SELL quantities) for the given portfolioId +
     * symbol.
     */
    private int getAvailableQuantity(String userId, String portfolioId, String symbol) {
        ScanRequest request = new ScanRequest()
                .withTableName(tableName)
                .withFilterExpression("userId = :uid AND portfolioId = :pid AND symbol = :sym")
                .withExpressionAttributeValues(Map.of(
                        ":uid", new AttributeValue(userId),
                        ":pid", new AttributeValue(portfolioId),
                        ":sym", new AttributeValue(symbol)));

        ScanResult result = dynamoDB.scan(request);
        int net = 0;
        for (Map<String, AttributeValue> item : result.getItems()) {
            String type = item.get("tradeType").getS();
            int qty = Integer.parseInt(item.get("quantity").getN());
            if ("BUY".equals(type)) {
                net += qty;
            } else if ("SELL".equals(type)) {
                net -= qty;
            }
        }
        return net;
    }

    private String generateTradeId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seq = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "TRD-" + datePart + "-" + seq;
    }

    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString(); // fallback
        }
    }

    private void saveToDynamoDB(Trade trade, String contentHash, String s3Key) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("tradeId", new AttributeValue(trade.getTradeId()));
        item.put("tradeType", new AttributeValue(trade.getTradeType()));
        item.put("portfolioId", new AttributeValue(trade.getPortfolioId()));
        item.put("brokerId", new AttributeValue(trade.getBrokerId()));
        item.put("symbol", new AttributeValue(trade.getSymbol()));
        item.put("quantity", new AttributeValue().withN(String.valueOf(trade.getQuantity())));
        item.put("price", new AttributeValue().withN(String.valueOf(trade.getPrice())));
        item.put("currency", new AttributeValue(trade.getCurrency()));
        item.put("tradeDate", new AttributeValue(trade.getTradeDate()));
        item.put("exchange", new AttributeValue(trade.getExchange()));
        item.put("clientId", new AttributeValue(trade.getClientId()));
        item.put("userId", new AttributeValue(trade.getUserId()));
        
        if (trade.getUploadTimestamp() != null) {
            item.put("uploadTimestamp", new AttributeValue().withN(String.valueOf(trade.getUploadTimestamp())));
        }
        
        item.put("contentHash", new AttributeValue(contentHash));
        item.put("s3Key", new AttributeValue(s3Key));

        dynamoDB.putItem(new PutItemRequest(tableName, item));
    }
}
