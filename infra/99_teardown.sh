#!/bin/bash
# Teardown: Delete all AWS resources created by the infra scripts
set -e
source "$(dirname "$0")/config.sh"

echo "⚠️  This will DELETE all tradex AWS resources. Press Ctrl+C to cancel."
read -p "Type 'yes' to confirm: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Cancelled."
    exit 0
fi

echo "Deleting Lambda function..."
aws lambda delete-function --function-name "$LAMBDA_FUNCTION_NAME" --region "$AWS_REGION" 2>/dev/null || true

echo "Deleting IAM role policy..."
aws iam delete-role-policy --role-name "$IAM_ROLE_NAME" --policy-name "tradex-lambda-policy" 2>/dev/null || true

echo "Deleting IAM role..."
aws iam delete-role --role-name "$IAM_ROLE_NAME" 2>/dev/null || true

echo "Deleting DynamoDB tables..."
aws dynamodb delete-table --table-name "$DYNAMODB_TABLE" --region "$AWS_REGION" 2>/dev/null || true
aws dynamodb delete-table --table-name "$DYNAMODB_PORTFOLIO_TABLE" --region "$AWS_REGION" 2>/dev/null || true

echo "Deleting SNS and SQS..."
SNS_TOPIC_ARN="arn:aws:sns:${AWS_REGION}:$(aws sts get-caller-identity --query Account --output text):${SNS_TOPIC_NAME}"
aws sns delete-topic --topic-arn "$SNS_TOPIC_ARN" --region "$AWS_REGION" 2>/dev/null || true

SQS_QUEUE_URL=$(aws sqs get-queue-url --queue-name "$SQS_QUEUE_NAME" --region "$AWS_REGION" --query 'QueueUrl' --output text 2>/dev/null)
if [ -n "$SQS_QUEUE_URL" ]; then
    aws sqs delete-queue --queue-url "$SQS_QUEUE_URL" --region "$AWS_REGION" 2>/dev/null || true
fi

echo "Emptying and deleting S3 bucket..."
aws s3 rm "s3://${S3_BUCKET}" --recursive 2>/dev/null || true
aws s3api delete-bucket --bucket "$S3_BUCKET" --region "$AWS_REGION" 2>/dev/null || true

echo "✅ All tradex resources deleted."
