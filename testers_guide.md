# Tradex API Testing Guide

## 1. Registration & Authentication

You must register and log in to get a **JWT Token**. This token is required for all other endpoints.

### 1a. Register a New User
*   **Method:** `POST`
*   **URL:** `http://localhost:8080/api/auth/register`
*   **Body (JSON):**
    ```json
    {
        "username": "tester1",
        "password": "password123"
    }
    ```

### 1b. Login / Get token
*   **Method:** `POST`
*   **URL:** `http://localhost:8080/api/auth/login`
*   **Body (JSON):**
    ```json
    {
        "username": "tester1",
        "password": "password123"
    }
    ```
*   **Response:** Returns a `token` string. Copy this!

---

## 2. Placing Trades

Use the token from step 1 in the `Authorization` header for all requests below:
*   **Header Key:** `Authorization`
*   **Header Value:** `Bearer [YOUR_TOKEN]`

### Option A: Upload Raw JSON (Easiest for Postman)
*   **Method:** `POST`
*   **URL:** `http://localhost:8080/api/files/upload/json`
*   **Body (JSON):**
    ```json
    {
        "tradeType": "BUY",
        "portfolioId": "PF100",
        "brokerId": "BRK789",
        "symbol": "TCS",
        "quantity": 100,
        "price": 3150.75,
        "currency": "INR",
        "tradeDate": "2026-03-10",
        "exchange": "NSE",
        "clientId": "CLT456"
    }
    ```
---

## 3. Viewing Portfolio

Wait ~5 seconds after uploading a trade (to allow AWS to process it), then check your updated portfolio holdings. Use the same Bearer Token.

*   **Method:** `GET`
*   **URL:** `http://localhost:8088/api/portfolio`
*   **Response:** A list of your current holdings across portfolios.
