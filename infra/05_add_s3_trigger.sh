#!/bin/bash
# Step 5: Wire S3 bucket to trigger Lambda on .json uploads
set -e
source "$(dirname "$0")/config.sh"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
LAMBDA_ARN="arn:aws:lambda:${AWS_REGION}:${ACCOUNT_ID}:function:${LAMBDA_FUNCTION_NAME}"

echo "Adding Lambda permission for S3 invocation..."

aws lambda add-permission \
    --function-name "$LAMBDA_FUNCTION_NAME" \
    --statement-id "s3-trigger" \
    --action "lambda:InvokeFunction" \
    --principal "s3.amazonaws.com" \
    --source-arn "arn:aws:s3:::${S3_BUCKET}" \
    --region "$AWS_REGION" 2>/dev/null || echo "(Permission may already exist)"

echo "Configuring S3 event notification..."

NOTIFICATION_CONFIG=$(cat <<EOF
{
  "LambdaFunctionConfigurations": [
    {
      "LambdaFunctionArn": "${LAMBDA_ARN}",
      "Events": ["s3:ObjectCreated:*"],
      "Filter": {
        "Key": {
          "FilterRules": [
            { "Name": "suffix", "Value": ".json" }
          ]
        }
      }
    }
  ]
}
EOF
)

aws s3api put-bucket-notification-configuration \
    --bucket "$S3_BUCKET" \
    --notification-configuration "$NOTIFICATION_CONFIG"

echo "✅ S3 trigger configured: *.json → $LAMBDA_FUNCTION_NAME"
