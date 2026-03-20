#!/bin/bash
# Step 2: Create DynamoDB table for storing validated trades
set -e
source "$(dirname "$0")/config.sh"

echo "Creating DynamoDB table: $DYNAMODB_TABLE..."

if aws dynamodb describe-table --table-name "$DYNAMODB_TABLE" --region "$AWS_REGION" 2>/dev/null >/dev/null; then
    echo "Table $DYNAMODB_TABLE already exists. Skipping creation."
else
    aws dynamodb create-table \
        --table-name "$DYNAMODB_TABLE" \
        --attribute-definitions \
            AttributeName=tradeId,AttributeType=S \
            AttributeName=tradeDate,AttributeType=S \
            AttributeName=portfolioId,AttributeType=S \
            AttributeName=symbol,AttributeType=S \
        --key-schema \
            AttributeName=tradeId,KeyType=HASH \
            AttributeName=tradeDate,KeyType=RANGE \
        --global-secondary-indexes \
            "[
                {
                    \"IndexName\": \"PortfolioSymbolIndex\",
                    \"KeySchema\": [{\"AttributeName\":\"portfolioId\",\"KeyType\":\"HASH\"}, {\"AttributeName\":\"symbol\",\"KeyType\":\"RANGE\"}],
                    \"Projection\": {\"ProjectionType\":\"ALL\"}
                }
            ]" \
        --billing-mode PAY_PER_REQUEST \
        --region "$AWS_REGION" > /dev/null

    echo "Waiting for table to become active..."
    aws dynamodb wait table-exists --table-name "$DYNAMODB_TABLE" --region "$AWS_REGION"
    echo "✅ DynamoDB table created: $DYNAMODB_TABLE"
fi

