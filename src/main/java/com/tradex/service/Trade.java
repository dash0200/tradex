package com.tradex.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class Trade {

    @NotBlank(message = "tradeType is required")
    @Pattern(regexp = "^(BUY|SELL)$", message = "tradeType must be BUY or SELL")
    private String tradeType;

    @NotBlank(message = "portfolioId is required")
    @Size(max = 20, message = "portfolioId max length is 20")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "portfolioId must be alphanumeric")
    private String portfolioId;

    @NotBlank(message = "brokerId is required")
    @Size(max = 20, message = "brokerId max length is 20")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "brokerId must be alphanumeric")
    private String brokerId;

    @NotBlank(message = "symbol is required")
    @Pattern(regexp = "^[A-Z]+$", message = "symbol must be uppercase alphabets only")
    @Size(max = 10, message = "symbol max length is 10")
    private String symbol;

    @Min(value = 1, message = "quantity must be > 0")
    private int quantity;

    @DecimalMin(value = "0.01", inclusive = true, message = "price must be > 0")
    private double price;

    @NotBlank(message = "currency is required")
    @Pattern(regexp = "^(INR)$", message = "currency must be INR or USD")
    private String currency;

    @NotNull(message = "tradeDate is required")
    @PastOrPresent(message = "tradeDate cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate tradeDate;

    @NotBlank(message = "exchange is required")
    @Pattern(regexp = "^(NSE|BSE)$", message = "exchange must be NSE or BSE")
    private String exchange;

    @NotBlank(message = "clientId is required")
    @Size(max = 20, message = "clientId max length is 20")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "clientId must be alphanumeric")
    private String clientId;

    private Long uploadTimestamp;

    private String userId;

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate  tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(Long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
}