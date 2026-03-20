#!/bin/bash
# Step 6: Create SNS topic, SQS queue, and subscribe queue to topic
set -e
source "$(dirname "$0")/config.sh"

echo "Creating SNS Topic: $SNS_TOPIC_NAME..."
SNS_TOPIC_ARN=$(aws sns create-topic --name "$SNS_TOPIC_NAME" --region "$AWS_REGION" --query 'TopicArn' --output text)
echo "Topic ARN: $SNS_TOPIC_ARN"

echo "Creating SQS Queue: $SQS_QUEUE_NAME..."
SQS_QUEUE_URL=$(aws sqs create-queue --queue-name "$SQS_QUEUE_NAME" --region "$AWS_REGION" --query 'QueueUrl' --output text)
echo "Queue URL: $SQS_QUEUE_URL"

SQS_QUEUE_ARN=$(aws sqs get-queue-attributes --queue-url "$SQS_QUEUE_URL" --region "$AWS_REGION" --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)

echo "Subscribing SQS to SNS..."
aws sns subscribe \
    --topic-arn "$SNS_TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$SQS_QUEUE_ARN" \
    --region "$AWS_REGION" \
    >/dev/null

echo "Granting SNS permission to publish to SQS..."
# Policy allowing SNS to send messages to the SQS queue
POLICY_DOC=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Id": "SNS_TO_SQS_POLICY",
  "Statement": [
    {
      "Sid": "Allow-SNS-SendMessage",
      "Effect": "Allow",
      "Principal": {
        "Service": "sns.amazonaws.com"
      },
      "Action": "sqs:SendMessage",
      "Resource": "$SQS_QUEUE_ARN",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "$SNS_TOPIC_ARN"
        }
      }
    }
  ]
}
EOF
)

aws sqs set-queue-attributes \
    --queue-url "$SQS_QUEUE_URL" \
    --region "$AWS_REGION" \
    --attributes "Policy='$POLICY_DOC'"

echo "✅ SNS Topic and SQS queue created and connected."
