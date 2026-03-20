package com.portfolio.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Repository
public class PortfolioRepository {

    private final AmazonDynamoDB dynamoDB;

    @Value("${aws.dynamodb.portfolio-table}")
    private String tableName;

    @Value("${aws.region}")
    private String region;

    public PortfolioRepository() {
        this.dynamoDB = AmazonDynamoDBClientBuilder.standard().build();
    }
    
    @PostConstruct
    public void init() {
         // Optionally set region if needed if not standard
    }

    public void updateHolding(String userId, String portfolioId, String symbol, int quantityChange, double valueChange) {
        String portfolioSymbolKey = portfolioId + "_" + symbol;

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("userId", new AttributeValue(userId));
        key.put("portfolioId_symbol", new AttributeValue(portfolioSymbolKey));

        UpdateItemRequest updateRequest = new UpdateItemRequest()
                .withTableName(tableName)
                .withKey(key)
                .withUpdateExpression("ADD quantity :q, totalValue :tv")
                .withExpressionAttributeValues(Map.of(
                        ":q", new AttributeValue().withN(String.valueOf(quantityChange)),
                        ":tv", new AttributeValue().withN(String.valueOf(valueChange))
                ));

        dynamoDB.updateItem(updateRequest);
        System.out.println("Updated holdings for " + userId + " - " + portfolioSymbolKey + ": " + quantityChange);
    }

    public List<Map<String, Object>> getUserPortfolio(String userId) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withKeyConditionExpression("userId = :uid")
                .withExpressionAttributeValues(Map.of(":uid", new AttributeValue(userId)));

        QueryResult result = dynamoDB.query(queryRequest);
        
        List<Map<String, Object>> portfolio = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            Map<String, Object> holding = new HashMap<>();
            holding.put("userId", item.get("userId").getS());
            
            String[] parts = item.get("portfolioId_symbol").getS().split("_", 2);
            holding.put("portfolioId", parts[0]);
            holding.put("symbol", parts.length > 1 ? parts[1] : "");
            
            holding.put("quantity", Integer.parseInt(item.get("quantity").getN()));
            
            double totalValue = item.containsKey("totalValue") ? Double.parseDouble(item.get("totalValue").getN()) : 0.0;
            holding.put("totalValue", totalValue);
            
            portfolio.add(holding);
        }
        return portfolio;
    }
}
