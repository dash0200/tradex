#!/bin/bash
# Step 3: Create IAM role for Lambda with required permissions
set -e
source "$(dirname "$0")/config.sh"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo "Creating IAM role: $IAM_ROLE_NAME..."

# Trust policy — allows Lambda to assume this role
TRUST_POLICY='{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "Service": "lambda.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }
  ]
}'

aws iam create-role \
    --role-name "$IAM_ROLE_NAME" \
    --assume-role-policy-document "$TRUST_POLICY" 2>/dev/null || echo "Role $IAM_ROLE_NAME already exists, skipping creation."

# Permissions: S3 read + DynamoDB write + CloudWatch logs
POLICY_DOC=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::${S3_BUCKET}/*"
    },
    {
      "Effect": "Allow",
      "Action": ["dynamodb:PutItem", "dynamodb:GetItem", "dynamodb:Scan"],
      "Resource": "arn:aws:dynamodb:${AWS_REGION}:${ACCOUNT_ID}:table/${DYNAMODB_TABLE}"
    },
    {
      "Effect": "Allow",
      "Action": ["sns:Publish"],
      "Resource": "arn:aws:sns:${AWS_REGION}:${ACCOUNT_ID}:${SNS_TOPIC_NAME}"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:${AWS_REGION}:${ACCOUNT_ID}:*"
    }
  ]
}
EOF
)

aws iam put-role-policy \
    --role-name "$IAM_ROLE_NAME" \
    --policy-name "tradex-lambda-policy" \
    --policy-document "$POLICY_DOC"

echo "Waiting 10s for IAM role propagation..."
sleep 10

echo "✅ IAM role created: $IAM_ROLE_NAME"
