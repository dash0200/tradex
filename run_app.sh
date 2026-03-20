#!/bin/bash
# Run the Spring Boot application
set -e

echo "Starting Tradex Spring Boot app..."
cd "$(dirname "$0")"
mvn clean spring-boot:run
