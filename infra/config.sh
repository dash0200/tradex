#!/bin/bash
# Shared configuration for all infra scripts

AWS_REGION="ap-south-1"
S3_BUCKET="tradex-uploads-$(aws sts get-caller-identity --query Account --output text)"
DYNAMODB_TABLE="Trades"
LAMBDA_FUNCTION_NAME="tradex-validator"
IAM_ROLE_NAME="tradex-lambda-role"
LAMBDA_HANDLER="com.tradex.lambda.TradeValidatorHandler::handleRequest"
LAMBDA_RUNTIME="java17"
LAMBDA_MEMORY=512
LAMBDA_TIMEOUT=30
LAMBDA_JAR_PATH="../lambda/target/tradex-lambda-1.0.0.jar"

SNS_TOPIC_NAME="tradex-validated-trades-topic"
SQS_QUEUE_NAME="tradex-portfolio-queue"
DYNAMODB_PORTFOLIO_TABLE="Portfolios"
