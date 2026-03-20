#!/bin/bash
# Step 4: Build the Lambda JAR and deploy (create or update)
set -e
source "$(dirname "$0")/config.sh"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ROLE_ARN="arn:aws:iam::${ACCOUNT_ID}:role/${IAM_ROLE_NAME}"

echo "Building Lambda JAR..."
cd "$(dirname "$0")/../lambda"
mvn clean package -q
cd -

echo "Deploying Lambda function: $LAMBDA_FUNCTION_NAME..."

SNS_TOPIC_ARN="arn:aws:sns:${AWS_REGION}:${ACCOUNT_ID}:${SNS_TOPIC_NAME}"

# Check if function already exists
if aws lambda get-function --function-name "$LAMBDA_FUNCTION_NAME" --region "$AWS_REGION" > /dev/null 2>&1; then
    echo "Function exists — updating code..."
    aws lambda update-function-code \
        --function-name "$LAMBDA_FUNCTION_NAME" \
        --zip-file "fileb://$(dirname "$0")/$LAMBDA_JAR_PATH" \
        --region "$AWS_REGION" >/dev/null
        
    echo "Waiting for code update to complete..."
    aws lambda wait function-updated \
        --function-name "$LAMBDA_FUNCTION_NAME" \
        --region "$AWS_REGION"
        
    echo "Updating function configuration..."
    aws lambda update-function-configuration \
        --function-name "$LAMBDA_FUNCTION_NAME" \
        --region "$AWS_REGION" \
        --environment "Variables={DYNAMODB_TABLE=$DYNAMODB_TABLE,AWS_REGION_NAME=$AWS_REGION,SNS_TOPIC_ARN=$SNS_TOPIC_ARN}" >/dev/null
else
    echo "Creating new function..."
    aws lambda create-function \
        --function-name "$LAMBDA_FUNCTION_NAME" \
        --runtime "$LAMBDA_RUNTIME" \
        --handler "$LAMBDA_HANDLER" \
        --role "$ROLE_ARN" \
        --zip-file "fileb://$(dirname "$0")/$LAMBDA_JAR_PATH" \
        --memory-size "$LAMBDA_MEMORY" \
        --timeout "$LAMBDA_TIMEOUT" \
        --region "$AWS_REGION" \
        --environment "Variables={DYNAMODB_TABLE=$DYNAMODB_TABLE,AWS_REGION_NAME=$AWS_REGION,SNS_TOPIC_ARN=$SNS_TOPIC_ARN}" >/dev/null
fi

echo "✅ Lambda deployed: $LAMBDA_FUNCTION_NAME"
