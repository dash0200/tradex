#!/bin/bash
# Step 1: Create S3 bucket for trade uploads
set -e
source "$(dirname "$0")/config.sh"

echo "Creating S3 bucket: $S3_BUCKET in $AWS_REGION..."

if aws s3api head-bucket --bucket "$S3_BUCKET" 2>/dev/null; then
    echo "Bucket $S3_BUCKET already exists. Skipping creation."
else
    aws s3api create-bucket \
        --bucket "$S3_BUCKET" \
        --region "$AWS_REGION" \
        --create-bucket-configuration LocationConstraint="$AWS_REGION"
        
    echo "✅ S3 bucket created: $S3_BUCKET"
fi
