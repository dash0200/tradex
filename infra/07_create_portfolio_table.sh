#!/bin/bash
# Step 7: Create DynamoDB Portfolios table
set -e
source "$(dirname "$0")/config.sh"

echo "Creating DynamoDB table: $DYNAMODB_PORTFOLIO_TABLE..."

if aws dynamodb describe-table --table-name "$DYNAMODB_PORTFOLIO_TABLE" --region "$AWS_REGION" 2>/dev/null >/dev/null; then
    echo "Table $DYNAMODB_PORTFOLIO_TABLE already exists. Skipping creation."
else
    aws dynamodb create-table \
        --table-name "$DYNAMODB_PORTFOLIO_TABLE" \
        --attribute-definitions \
            AttributeName=userId,AttributeType=S \
            AttributeName=portfolioId_symbol,AttributeType=S \
        --key-schema \
            AttributeName=userId,KeyType=HASH \
            AttributeName=portfolioId_symbol,KeyType=RANGE \
        --billing-mode PAY_PER_REQUEST \
        --region "$AWS_REGION" \
        >/dev/null

    echo "Waiting for table to become ACTIVE (takes a few seconds)..."
    aws dynamodb wait table-exists --table-name "$DYNAMODB_PORTFOLIO_TABLE" --region "$AWS_REGION"
    echo "✅ DynamoDB table created: $DYNAMODB_PORTFOLIO_TABLE"
fi

