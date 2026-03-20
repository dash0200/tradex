#!/bin/bash
# Run the Spring Boot Portfolio application
set -e

echo "Starting Portfolio Spring Boot Microservice..."
cd "$(dirname "$0")/portfolio-service"
mvn clean spring-boot:run
