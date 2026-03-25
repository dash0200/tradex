package com.tradex.service.TradeValidationTest;

import com.tradex.service.Trade;
import jakarta.validation.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TradeValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private Trade validTrade() {
        Trade trade = new Trade();
        trade.setTradeType("BUY");
        trade.setPortfolioId("PF1001");
        trade.setBrokerId("BRK789");
        trade.setSymbol("INFY");
        trade.setQuantity(100);
        trade.setPrice(1450.75);
        trade.setCurrency("INR");
        trade.setTradeDate(LocalDate.now());
        trade.setExchange("NSE");
        trade.setClientId("CLT456");
        trade.setUserId("USER1");
        trade.setUploadTimestamp(System.currentTimeMillis());
        return trade;
    }

    private Set<ConstraintViolation<Trade>> validate(Trade trade) {
        return validator.validate(trade);
    }

    @Test
    void shouldPassForValidTrade() {
        Trade trade = validTrade();
        assertTrue(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenTradeTypeInvalid() {
        Trade trade = validTrade();
        trade.setTradeType("INVALID");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenTradeTypeBlank() {
        Trade trade = validTrade();
        trade.setTradeType("");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenPortfolioIdHasSpecialChars() {
        Trade trade = validTrade();
        trade.setPortfolioId("PF@123");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenPortfolioIdTooLong() {
        Trade trade = validTrade();
        trade.setPortfolioId("A".repeat(25));

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenSymbolNotUppercase() {
        Trade trade = validTrade();
        trade.setSymbol("infy");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenSymbolHasNumbers() {
        Trade trade = validTrade();
        trade.setSymbol("INFY1");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenQuantityZero() {
        Trade trade = validTrade();
        trade.setQuantity(0);

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenQuantityNegative() {
        Trade trade = validTrade();
        trade.setQuantity(-10);

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenPriceZero() {
        Trade trade = validTrade();
        trade.setPrice(0);

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenCurrencyInvalid() {
        Trade trade = validTrade();
        trade.setCurrency("EUR");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenExchangeInvalid() {
        Trade trade = validTrade();
        trade.setExchange("NYSE");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenClientIdHasSpecialChars() {
        Trade trade = validTrade();
        trade.setClientId("CLT@123");

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenTradeDateFuture() {
        Trade trade = validTrade();
        trade.setTradeDate(LocalDate.now().plusDays(1));

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenTradeDateNull() {
        Trade trade = validTrade();
        trade.setTradeDate(null);

        assertFalse(validate(trade).isEmpty());
    }

    @Test
    void shouldFailWhenRequiredFieldsMissing() {
        Trade trade = new Trade();

        assertFalse(validate(trade).isEmpty());
    }
}