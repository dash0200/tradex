#!/bin/bash
set -euo pipefail

# -------- CONFIG --------
EC2_IP="13.235.81.141"
PEM_FILE="../tradex-keypair.pem"
EC2_USER="ec2-user"

TRADE_PORT=8080
PORTFOLIO_PORT=8081
APP_DIR="~/tradex-apps"

SSH_OPTS="-o StrictHostKeyChecking=no -i $PEM_FILE"

echo "================================================"
echo "🚀 Deploying to $EC2_USER@$EC2_IP"
echo "================================================"

# -------- BUILD --------
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(dirname "$SCRIPT_DIR")

echo "--> Building Trade Service..."
cd "$PROJECT_ROOT"
mvn -q clean package -DskipTests

TRADE_JAR="$PROJECT_ROOT/target/$(ls target | grep '.jar' | grep -v sources | head -n1)"

if [ ! -f "$TRADE_JAR" ]; then
  echo "❌ Trade JAR not found"
  exit 1
fi

echo "Trade JAR: $(basename $TRADE_JAR)"

echo "--> Building Portfolio Service..."
cd "$PROJECT_ROOT/portfolio-service"
mvn -q clean package -DskipTests

PORTFOLIO_JAR="$PROJECT_ROOT/portfolio-service/target/$(ls target | grep '.jar' | grep -v sources | head -n1)"

if [ ! -f "$PORTFOLIO_JAR" ]; then
  echo "❌ Portfolio JAR not found"
  exit 1
fi

echo "Portfolio JAR: $(basename $PORTFOLIO_JAR)"

# -------- DEPLOY --------
echo "--> Creating app dir..."
ssh $SSH_OPTS $EC2_USER@$EC2_IP "mkdir -p $APP_DIR"

echo "--> Uploading JARs..."
scp $SSH_OPTS "$TRADE_JAR" $EC2_USER@$EC2_IP:$APP_DIR/trade-service.jar.new
scp $SSH_OPTS "$PORTFOLIO_JAR" $EC2_USER@$EC2_IP:$APP_DIR/portfolio-service.jar.new

# -------- REMOTE EXEC --------
echo "--> Restarting services..."
ssh $SSH_OPTS $EC2_USER@$EC2_IP << EOF
set -e
cd $APP_DIR

# Atomic replace
[ -f trade-service.jar.new ] && mv trade-service.jar.new trade-service.jar
[ -f portfolio-service.jar.new ] && mv portfolio-service.jar.new portfolio-service.jar

# Stop if running
if lsof -i:$TRADE_PORT -t >/dev/null; then
  kill \$(lsof -i:$TRADE_PORT -t)
fi

if lsof -i:$PORTFOLIO_PORT -t >/dev/null; then
  kill \$(lsof -i:$PORTFOLIO_PORT -t)
fi

sleep 2

# Start services
nohup java -jar trade-service.jar --server.port=$TRADE_PORT > trade.log 2>&1 &
nohup java -jar portfolio-service.jar --server.port=$PORTFOLIO_PORT > portfolio.log 2>&1 &

echo "Services running."
EOF

echo "================================================"
echo "✅ Deployment Done"
echo "Trade:     http://$EC2_IP:$TRADE_PORT"
echo "Portfolio: http://$EC2_IP:$PORTFOLIO_PORT"
echo "================================================"