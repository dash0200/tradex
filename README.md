# TradeX — Trade & Portfolio Management System

A simple cloud-native trade management application built with **Spring Boot**, **AWS services**, and a **React** frontend. Users register, log in, submit stock trades as JSON, and view their consolidated portfolio — all powered by a serverless event-driven pipeline on AWS.

---

## What Does TradeX Do?

1. **Register / Login** — Create an account and get a JWT token.
2. **Submit a Trade** — Paste a JSON trade (BUY or SELL) from the UI. It gets uploaded to S3.
3. **Automatic Processing** — A Lambda function validates the trade and stores it in DynamoDB.
4. **Portfolio Update** — The validated trade flows through SNS → SQS to the Portfolio Service, which updates your holdings.
5. **View Portfolio** — See your consolidated holdings and total portfolio value in real time.

---

## Architecture Overview

```
┌─────────────┐      POST /api/files/upload/json       ┌──────────────────┐
│  React UI   │ ──────────────────────────────────────► │  Trade Service   │
│ (Vite :5173)│                                         │  (Spring Boot    │
│             │      POST /api/auth/login               │   :8080)         │
│             │ ──────────────────────────────────────► │                  │
└─────────────┘                                         └────────┬─────────┘
                                                                 │ uploads JSON
                                                                 ▼
                                                        ┌──────────────────┐
                                                        │    AWS S3        │
                                                        │ tradex-uploads-* │
                                                        └────────┬─────────┘
                                                                 │ S3 trigger
                                                                 ▼
                                                        ┌──────────────────┐
                                                        │  AWS Lambda      │
                                                        │ tradex-validator │
                                                        │                  │
                                                        │ • validates trade│
                                                        │ • saves to       │
                                                        │   DynamoDB Trades│
                                                        │ • publishes to   │
                                                        │   SNS            │
                                                        └──┬──────────┬────┘
                                                           │          │
                                                           ▼          ▼
                                                  ┌────────────┐ ┌────────────┐
                                                  │ DynamoDB   │ │  AWS SNS   │
                                                  │ (Trades)   │ │  Topic     │
                                                  └────────────┘ └─────┬──────┘
                                                                       │
                                                                       ▼
                                                                 ┌────────────┐
                                                                 │  AWS SQS   │
                                                                 │  Queue     │
                                                                 └─────┬──────┘
                                                                       │ polls every 5s
                                                                       ▼
┌─────────────┐     GET /api/portfolio              ┌──────────────────────┐
│  React UI   │ ◄────────────────────────────────── │  Portfolio Service   │
│             │                                      │  (Spring Boot :8081) │
└─────────────┘                                      │                      │
                                                     │  • Reads SQS msgs    │
                                                     │  • Updates DynamoDB  │
                                                     │    Portfolios table  │
                                                     └──────────┬───────────┘
                                                                │
                                                                ▼
                                                        ┌────────────────┐
                                                        │   DynamoDB     │
                                                        │  (Portfolios)  │
                                                        └────────────────┘
```

---

## AWS Components

| Component | Name | Purpose |
|-----------|------|---------|
| **S3 Bucket** | `tradex-uploads-<account-id>` | Stores uploaded trade JSON files |
| **Lambda** | `tradex-validator` | Triggered by S3 upload. Validates trade, saves to Trades table, publishes to SNS |
| **DynamoDB** | `Trades` | Stores all validated trades with deduplication (content hash) |
| **DynamoDB** | `Portfolios` | Stores consolidated holdings per user (quantity + total value) |
| **SNS Topic** | `tradex-validated-trades-topic` | Broadcasts validated trades to subscribers |
| **SQS Queue** | `tradex-portfolio-queue` | Subscribes to SNS. Portfolio Service polls this queue |
| **EC2** | `13.235.81.141` | Hosts both Spring Boot services |
| **IAM Role** | `tradex-lambda-role` | Grants Lambda access to S3, DynamoDB, SNS |

---

## Services

### 1. Trade Service (Port 8080)

The main backend. Handles authentication and trade uploads.

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/register` | POST | No | Register a new user |
| `/api/auth/login` | POST | No | Login, returns JWT token |
| `/api/files/upload/json` | POST | JWT | Submit a trade as JSON |

### 2. Portfolio Service (Port 8081)

Reads validated trades from SQS and maintains a real-time portfolio.

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/portfolio` | GET | JWT | Get all holdings for the logged-in user |

### 3. React UI (Port 5173 dev)

Frontend built with Vite + React. Connects to both services.

**Pages:**
- `/login` — Sign in (redirects to portfolio if already logged in)
- `/register` — Create account
- `/portfolio` — View holdings + submit trades via JSON editor

---

## How the Data Flows

### Submitting a Trade

1. User pastes trade JSON in the React UI and clicks **Submit Trade**
2. React sends `POST /api/files/upload/json` to the Trade Service with the JWT token
3. Trade Service validates the fields, attaches the `userId`, and uploads the JSON to **S3**
4. S3 triggers the **Lambda** function (`tradex-validator`)
5. Lambda:
   - Reads the JSON from S3
   - Validates all fields (type, symbol, quantity, price, etc.)
   - Checks for duplicate trades (SHA-256 hash)
   - For SELL trades: checks if user has enough holdings
   - Saves the validated trade to the **Trades** DynamoDB table
   - Publishes the trade to **SNS**
6. SNS forwards the message to the **SQS** queue
7. Portfolio Service polls SQS every 5 seconds, picks up the trade
8. For BUY: adds quantity and value to the **Portfolios** DynamoDB table
9. For SELL: subtracts quantity and value

### Viewing Portfolio

1. User opens the `/portfolio` page
2. React sends `GET /api/portfolio` to the Portfolio Service with JWT
3. Portfolio Service extracts `userId` from the JWT and queries the **Portfolios** DynamoDB table
4. Returns a list of holdings with `symbol`, `quantity`, `totalValue`, and `portfolioId`

---

## Trade JSON Format

```json
{
  "tradeType": "BUY",
  "portfolioId": "PF1001",
  "brokerId": "BRK789",
  "symbol": "INFY",
  "quantity": 100,
  "price": 1450.75,
  "currency": "INR",
  "tradeDate": "2026-03-09",
  "exchange": "NSE",
  "clientId": "CLT456"
}
```

| Field | Rules |
|-------|-------|
| `tradeType` | `BUY` or `SELL` |
| `portfolioId` | Alphanumeric, max 20 chars |
| `brokerId` | Alphanumeric, max 20 chars |
| `symbol` | Uppercase letters only (e.g. `INFY`, `TCS`) |
| `quantity` | Must be > 0 |
| `price` | Must be > 0 |
| `currency` | `INR` |
| `tradeDate` | `yyyy-MM-dd` format, cannot be in the future |
| `exchange` | `NSE` or `BSE` |
| `clientId` | Alphanumeric, max 20 chars |

---

## Trade Validations (Lambda)

The Lambda function performs these checks before accepting a trade:

- **Field validation** — All required fields must be present and valid
- **Duplicate detection** — SHA-256 hash of the trade content prevents re-processing identical trades
- **Idempotency** — Duplicate S3 events are silently skipped
- **SELL without BUY** — Rejected if no holdings exist for that symbol
- **Oversell** — Rejected if sell quantity exceeds available holdings
- **Partial sell** — Allowed if sell quantity ≤ available holdings

---

## How to Run

### 1. Set Up AWS Infrastructure

Run the infra scripts in order from the `infra/` directory:

```bash
cd infra
./01_create_s3_bucket.sh
./02_create_dynamodb_table.sh
./03_create_iam_role.sh
./04_build_and_deploy_lambda.sh
./05_add_s3_trigger.sh
./06_create_sns_sqs.sh
./07_create_portfolio_table.sh
```

### 2. Deploy Services to EC2

```bash
cd infra
./deploy_app.sh
```

This builds both JARs and deploys them to the EC2 instance.

- Trade Service → `http://<EC2_IP>:8080`
- Portfolio Service → `http://<EC2_IP>:8081`

### 3. Run the React UI

```bash
cd portfolio-ui
npm install
npm run dev
```

Configure the `.env` file:
```
VITE_PORTFOLIO_API_URL=http://<EC2_IP>:8088
VITE_TRADE_UPLOAD_API_URL=http://<EC2_IP>:8080
```

### 4. Use the App

1. Open `http://localhost:5173`
2. Register or log in
3. Paste a trade JSON and click **Submit Trade**
4. Wait a few seconds for the pipeline to process
5. Your portfolio holdings and total value will update automatically

---

## Teardown

To delete all AWS resources:

```bash
cd infra
./99_teardown.sh
```
