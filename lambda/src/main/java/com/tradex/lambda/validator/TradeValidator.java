package com.tradex.lambda.validator;

import com.tradex.lambda.model.Trade;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates a Trade based on rules from Validations_Trade.txt.
 */
public class TradeValidator {

    private static final Set<String> VALID_TRADE_TYPES = Set.of("BUY", "SELL");
    private static final Set<String> VALID_CURRENCIES = Set.of("INR", "USD");
    private static final Set<String> VALID_EXCHANGES = Set.of("NSE", "BSE");

    public List<String> validate(Trade trade) {
        List<String> errors = new ArrayList<>();

        // tradeType — must be BUY or SELL
        if (isBlank(trade.getTradeType())) {
            errors.add("tradeType is required");
        } else if (!VALID_TRADE_TYPES.contains(trade.getTradeType())) {
            errors.add("tradeType must be BUY or SELL, got: " + trade.getTradeType());
        }

        // Mandatory string fields
        if (isBlank(trade.getPortfolioId()))
            errors.add("portfolioId is required");
        if (isBlank(trade.getBrokerId()))
            errors.add("brokerId is required");
        if (isBlank(trade.getSymbol()))
            errors.add("symbol is required");
        if (isBlank(trade.getClientId()))
            errors.add("clientId is required");

        // quantity — must be > 0
        if (trade.getQuantity() <= 0) {
            errors.add("quantity must be > 0, got: " + trade.getQuantity());
        }

        // price — must be > 0
        if (trade.getPrice() <= 0) {
            errors.add("price must be > 0, got: " + trade.getPrice());
        }

        // currency — must be INR or USD
        if (isBlank(trade.getCurrency())) {
            errors.add("currency is required");
        } else if (!VALID_CURRENCIES.contains(trade.getCurrency())) {
            errors.add("currency must be INR or USD, got: " + trade.getCurrency());
        }

        // exchange — must be NSE or BSE
        if (isBlank(trade.getExchange())) {
            errors.add("exchange is required");
        } else if (!VALID_EXCHANGES.contains(trade.getExchange())) {
            errors.add("exchange must be NSE or BSE, got: " + trade.getExchange());
        }

        // tradeDate — cannot be future date
        if (isBlank(trade.getTradeDate())) {
            errors.add("tradeDate is required");
        } else {
            try {
                LocalDate date = LocalDate.parse(trade.getTradeDate());
                if (date.isAfter(LocalDate.now())) {
                    errors.add("tradeDate cannot be a future date: " + trade.getTradeDate());
                }
            } catch (DateTimeParseException e) {
                errors.add("tradeDate is not a valid date (expected YYYY-MM-DD): " + trade.getTradeDate());
            }
        }

        return errors;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
